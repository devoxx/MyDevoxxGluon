/*
 * Copyright (c) 2016, 2018 Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.util;

import com.devoxx.DevoxxView;
import com.devoxx.model.Conference;
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.views.SessionPresenter;
import com.devoxx.views.helper.Util;
import com.gluonhq.attach.localnotifications.LocalNotificationsService;
import com.gluonhq.attach.localnotifications.Notification;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.util.DevoxxLogging.LOGGING_ENABLED;
import static java.time.temporal.ChronoUnit.SECONDS;

@Singleton
public class DevoxxNotifications {
    
    private static final Logger LOG = Logger.getLogger(DevoxxNotifications.class.getName());

    public static final String GCM_SENDER_ID = "945674729015";
    
    private static final String ID_START = "START_";
    private static final String ID_VOTE = "VOTE_";
    private static final String ID_RATE = "RATE_";

    private final static String TITLE_RATE_DEVOXX = DevoxxBundle.getString("OTN.VISUALS.RATE_DEVOXX");
    private final static String TITLE_VOTE_SESSION = DevoxxBundle.getString("OTN.VISUALS.VOTE_NOW");
    private final static String TITLE_SESSION_STARTS = DevoxxBundle.getString("OTN.VISUALS.SESSION_STARTING_SOON");
    private final static int SHOW_VOTE_NOTIFICATION = -2; // show vote notification two minutes before session ends
    private final static int SHOW_SESSION_START_NOTIFICATION = -15; // show session start warning 15 minutes before posted start time
    
    private final Map<String, Notification> startSessionNotificationMap = new HashMap<>();
    private final Map<String, Notification> voteSessionNotificationMap = new HashMap<>();
    private final Map<String, Notification> ratingNotificationMap = new HashMap<>();
    private final Map<String, Notification> dummyNotificationMap = new HashMap<>();

    private InvalidationListener ratingListener;
    private ListChangeListener<Session> favoriteSessionsListener;

    @Inject
    private Service service;
    
    private final Optional<LocalNotificationsService> notificationsService;
    private boolean startup;
    private boolean authenticatedStartup;

    public DevoxxNotifications() {
        notificationsService = LocalNotificationsService.create();
    }

    /**
     * For a new Favorite Session, we create two local notifications:
     * - One notification will be triggered by the device before the session starts
     * - One notification will be triggered by the device right before the session ends
     * 
     * We create the notifications and schedule them on the device, but only for future events. 
     * 
     * @param session The new favored session
     */
    public final void addFavoriteSessionNotifications(Session session) {
        final String sessionId = session.getTalk().getId();
        
        if (!startSessionNotificationMap.containsKey(sessionId)) {
            createStartNotification(session).ifPresent(notification -> {
                startSessionNotificationMap.put(sessionId, notification);
                notificationsService.ifPresent(n -> n.getNotifications().add(notification));
            });
        }
        
        if (!voteSessionNotificationMap.containsKey(sessionId)) {
            createVoteNotification(session).ifPresent(notification -> {
                voteSessionNotificationMap.put(sessionId, notification);
                notificationsService.ifPresent(n -> n.getNotifications().add(notification));
            });
        }
    }

    public void addRatingNotification(Conference conference) {
        if (!ratingNotificationMap.containsKey(conference.getId())) {
            createRatingNotification(conference).ifPresent(notification -> {
                ratingNotificationMap.put(conference.getId(), notification);
                notificationsService.ifPresent(n -> n.getNotifications().add(notification));
            });
        }
    }
    
    /**
     * If a favorite session is removed as favorite, both start and vote notifications
     * should be cancelled on the device
     * @param session session removed as favorite 
     */
    public final void removeFavoriteSessionNotifications(Session session) {
        final Notification startNotification = startSessionNotificationMap.remove(session.getTalk().getId());
        if (startNotification != null) { 
            notificationsService.ifPresent(n -> n.getNotifications().remove(startNotification));
        }
        final Notification voteNotification = voteSessionNotificationMap.remove(session.getTalk().getId());
        if (voteNotification != null) { 
            notificationsService.ifPresent(n -> n.getNotifications().remove(voteNotification));
        }
    }

    /**
     * Called when the application starts, allows retrieving the rating
     * notifications, and restoring the notifications map
     */
    public void preloadRatingNotifications() {
        if (LOGGING_ENABLED) {
            LOG.log(Level.INFO, "Preload of rating notifications started");
        }
        startup = true;
        ratingListener = observable -> {
            if (service.retrieveSessions().size() > 0) {
                addAlreadyRatingNotifications();
                service.retrieveSessions().removeListener(ratingListener);
            }
        };
        service.retrieveSessions().addListener(ratingListener);
    }

    /**
     * Called when the application starts, allows retrieving the favorite
     * notifications, and restoring the notifications map
     */
    public void preloadFavoriteSessions() {
        if (LOGGING_ENABLED) {
            LOG.log(Level.INFO, "Preload of favored sessions started");
        }
        if (service.isAuthenticated()) { 
            authenticatedStartup = true;
            favoriteSessionsListener = (ListChangeListener.Change<? extends Session> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Session session : c.getAddedSubList()) {
                            if (LOGGING_ENABLED) {
                                LOG.log(Level.INFO, String.format("Adding notification %s", session.getTalk().getId()));
                            }
                            addAlreadyFavoredSessionNotifications(session);
                        }
                    }
                }
            };
            service.retrieveFavoredSessions().addListener(favoriteSessionsListener);
            // we don't make call to preloadRatingNotifications from DevoxxService#retrieveSessionsInternal
            // when the user is authenticated and try to fuse the flow with that of FavoriteNotifications
            preloadRatingNotifications();
        }
    }

    /**
     * Called after the application has started and pre-loading the favored sessions
     * ends. At this point, we have all the notifications available, and we can remove
     * the listener (so new notifications are not treated as already scheduled) and 
     * send them to the Local Notifications service at once
     */
    public void preloadingNotificationsDone() {
        if (favoriteSessionsListener != null) {
            service.retrieveFavoredSessions().removeListener(favoriteSessionsListener);
            favoriteSessionsListener = null;
        }

        if (ratingListener != null) {
            service.retrieveSessions().removeListener(ratingListener);
            ratingListener = null;
        }

        if (! dummyNotificationMap.isEmpty()) {

            // 1. Add all dummy notifications to the notification map at once.
            // These need to be present all the time (as notifications will
            // be opened always some time after they were delivered).
            // Adding these dummy notifications doesn't schedule them on the
            // device and it doesn't cause duplicate exceptions
            notificationsService.ifPresent(ns ->
                    ns.getNotifications().addAll(dummyNotificationMap.values()));
        }

        // process notifications at once
        List<Notification> notificationList = new ArrayList<>();
        notificationList.addAll(startSessionNotificationMap.values());
        notificationList.addAll(voteSessionNotificationMap.values());
        notificationList.addAll(ratingNotificationMap.values());

        if (! notificationList.isEmpty()) {

            // 2. Schedule only real future notifications
            final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
            notificationsService.ifPresent(ns -> {
                for (Notification n : notificationList) {
                    if (n.getDateTime() != null && n.getDateTime().isAfter(now)) {
                        // Remove notification before scheduling it again,
                        // to avoid duplicate exception
                        final Notification dummyN = dummyNotificationMap.get(n.getId());
                        if (dummyN != null) {
                            if (LOGGING_ENABLED) {
                                LOG.log(Level.INFO, String.format("Removing notification %s", n.getId()));
                            }
                            ns.getNotifications().remove(dummyN);
                        }
                        if (LOGGING_ENABLED) {
                            LOG.log(Level.INFO, String.format("Adding favored notification %s", n.getId()));
                        }
                        ns.getNotifications().add(n);
                    }
                }
            });
        }

        dummyNotificationMap.clear();

        startup = false;
        authenticatedStartup = false;
        if (LOGGING_ENABLED) {
            LOG.log(Level.INFO, "Preload of favored sessions ended");
        }
    }
    
    /**
     * Creates a notification that will be triggered by the device before the session starts
     * @param session the favored session
     * @return a notification if the event is in the future
     */
    private Optional<Notification> createStartNotification(Session session) {
        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        
        // Add notification 15 min before session starts or during authenticatedStartup
        ZonedDateTime dateTimeStart = session.getStartDate().plusMinutes(SHOW_SESSION_START_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeStart = dateTimeStart.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
            if (LOGGING_ENABLED) {
                LOG.log(Level.INFO, String.format("Start notification scheduled at: %s", dateTimeStart));
            }
        }
        
        // add the notification for new ones if they haven't started yet
        if (dateTimeStart.isAfter(now) || authenticatedStartup) {
            return Optional.of(getStartNotification(session, dateTimeStart));
        }
        return Optional.empty();
    }
    
    /**
     * Creates a notification that will be triggered by the device right before the session ends
     * @param session the favored session
     * @return a notification if the session wasn't added yet and it was already scheduled or 
     * the event is in the future
     */
    private Optional<Notification> createVoteNotification(Session session) {
        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        
        // Add notification 2 min before session ends
        ZonedDateTime dateTimeVote = session.getEndDate().plusMinutes(SHOW_VOTE_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeVote = dateTimeVote.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
            if (LOGGING_ENABLED) {
                LOG.log(Level.INFO, String.format("Vote notification scheduled at: %s", dateTimeVote));
            }
        }
        
        // add the notification if the session hasn't finished yet or during authenticatedStartup
        if (dateTimeVote.isAfter(now) || authenticatedStartup) {
            return Optional.of(getVoteNotification(session, dateTimeVote));
        }
        return Optional.empty();
    }

    /**
     * Creates a notification that will be triggered by the device right before the end of the conference
     * @param conference Conference for which the notification has to be triggered
     * @return a notification if the session wasn't added yet and it was already scheduled or
     * the event is in the future
     */
    private Optional<Notification> createRatingNotification(Conference conference) {
        final ZonedDateTime now = ZonedDateTime.now(conference.getConferenceZoneId());

        // Add notification an hour before the last session begins
        ZonedDateTime dateTimeRating = Util.findLastSessionOfLastDay(service).getStartDate().minusHours(1);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeRating = dateTimeRating.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
            if (LOGGING_ENABLED) {
                LOG.log(Level.INFO, String.format("Rating notification scheduled at: %s", dateTimeRating));
            }
        }

        // add the notification if the time is in future or is startup is true
        if (dateTimeRating.isAfter(now) || startup) {
            return Optional.of(getRatingNotification(conference, dateTimeRating));
        }
        return Optional.empty();
    }
    
    /**
     * For an already favored session, we create two local notifications.
     * 
     * These notifications are not sent to the Local Notification service yet: 
     * this will be done when calling {@link #preloadingNotificationsDone()}.
     * 
     * We don't schedule them again on the device, but we add these notifications to the 
     * notifications map, so in case they are delivered, their runnable is available.
     * 
     * @param session The already favored session
     */
    private void addAlreadyFavoredSessionNotifications(Session session) {
        final String sessionId = session.getTalk().getId();
        
        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        // Add notification 15 min before session starts
        ZonedDateTime dateTimeStart = session.getStartDate().plusMinutes(SHOW_SESSION_START_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeStart = dateTimeStart.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
        }
        
        if (dateTimeStart.isAfter(now) || authenticatedStartup) {
            if (!startSessionNotificationMap.containsKey(sessionId)) {
                dummyNotificationMap.put(ID_START + sessionId, getStartNotification(session, null));

                // Add notification
                createStartNotification(session).ifPresent(n -> {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.INFO, String.format("Adding start notification %s", n.getId()));
                    }
                    startSessionNotificationMap.put(sessionId, n);
                });
            }
        }
        
        // Add notification 2 min before session ends
        ZonedDateTime dateTimeVote = session.getEndDate().plusMinutes(SHOW_VOTE_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeVote = dateTimeVote.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
        }
        if (dateTimeVote.isAfter(now) || authenticatedStartup) {
            if (!voteSessionNotificationMap.containsKey(sessionId)) {
                dummyNotificationMap.put(ID_VOTE + sessionId, getVoteNotification(session, null));
                
                createVoteNotification(session).ifPresent(n -> {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.INFO, String.format("Adding vote notification %s", n.getId()));
                    }
                    voteSessionNotificationMap.put(sessionId, n);
                });
            }
        }
    }

    private void addAlreadyRatingNotifications() {
        final Conference conference = service.getConference();

        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        // Add notification an hour before the last session begins
        ZonedDateTime dateTimeRating = Util.findLastSessionOfLastDay(service).getStartDate().minusHours(1);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeRating = dateTimeRating.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
        }

        // add the notification if the time is in future
        if (dateTimeRating.isAfter(now)  || startup) {
            if (!ratingNotificationMap.containsKey(conference.getId())) {
                dummyNotificationMap.put(ID_RATE + conference.getId(), getRatingNotification(conference, null));

                createRatingNotification(conference).ifPresent(n -> {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.INFO, String.format("Adding rating notification %s", n.getId()));
                    }
                    ratingNotificationMap.put(conference.getId(), n);
                });
            }
        }
    }
    
    /**
     * Creates a notification that will be triggered by the device before the session starts
     * 
     * @param session the favored session
     * @param dateTimeStart the session's start zoned date time. If null, this notification won't be 
     * scheduled on the device
     * @return a local notification
     */
    private Notification getStartNotification(Session session, ZonedDateTime dateTimeStart) {
        return new Notification(
                ID_START + session.getTalk().getId(),
                TITLE_SESSION_STARTS, 
                DevoxxBundle.getString("OTN.VISUALS.IS_ABOUT_TO_START"+(session.hasRoomName()?"_IN_ROOM":""), session.getTitle(), session.getRoomName()),
                DevoxxNotifications.class.getResourceAsStream("/icon.png"),
                dateTimeStart,
                () -> {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.INFO, String.format("Running start notification %s", session.getTalk().getId()));
                    }
                    DevoxxView.SESSION.switchView().ifPresent(presenter -> {
                        SessionPresenter sessionPresenter = (SessionPresenter) presenter;
                        sessionPresenter.showSession(session);
                    });
                });
    }
    
    /**
     * Creates a notification that will be triggered by the device right before the session ends
     * @param session the favored session
     * @param dateTimeVote the session's end zoned date time. If null, this notification won't be 
     * scheduled on the device
     * @return a local notification
     */
    private Notification getVoteNotification(Session session, ZonedDateTime dateTimeVote) {
        return new Notification(
                ID_VOTE + session.getTalk().getId(),
                TITLE_VOTE_SESSION,
                DevoxxBundle.getString("OTN.VISUALS.CAST_YOUR_VOTE_ON", session.getTitle()),
                DevoxxNotifications.class.getResourceAsStream("/icon.png"),
                dateTimeVote,
                () -> {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.INFO, String.format("Running vote notification %s", session.getTalk().getId()));
                    }
                    // first go to the session
                    DevoxxView.SESSION.switchView().ifPresent(presenter -> {
                        SessionPresenter sessionPresenter = (SessionPresenter) presenter;
                        sessionPresenter.showSession(session, SessionPresenter.Pane.VOTE);
                    });
                });
    }

    /**
     * Creates a notification that will be triggered by the device at the end of a conference,
     * requesting users to rate for the app
     * @param conference Conference for which the notification is to be scheduled
     * @param dateTimeRating Zoned date time at which the notification is to be shown.
     * If null, this notification won't be scheduled on the device
     * @return a local notification
     */
    private Notification getRatingNotification(Conference conference, ZonedDateTime dateTimeRating) {
        return new Notification(
            ID_RATE + conference.getId(),
            TITLE_RATE_DEVOXX,
            DevoxxBundle.getString("OTN.VISUALS.HELP_US_IMPROVE_DEVOXX"),
            DevoxxNotifications.class.getResourceAsStream("/icon.png"),
            dateTimeRating,
            () -> {
                if (LOGGING_ENABLED) {
                    LOG.log(Level.INFO, String.format("Running rating notification %s", conference.getId()));
                }
                DevoxxView.SESSIONS.switchView();
            }
        );
    }
}
