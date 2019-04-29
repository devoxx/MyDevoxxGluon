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
package com.devoxx.service;

import static com.devoxx.util.DevoxxSettings.LOCAL_NOTIFICATION_RATING;
import static com.devoxx.util.DevoxxSettings.SESSION_FILTER;
import static com.devoxx.views.helper.Util.addCSVToLocalStorage;
import static com.devoxx.views.helper.Util.fetchCSVFromLocalStorage;
import static com.devoxx.views.helper.Util.isOnGoing;
import static com.devoxx.views.helper.Util.safeStr;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.airhacks.afterburner.injection.Injector;
import com.devoxx.model.Badge;
import com.devoxx.model.Conference;
import com.devoxx.model.Exhibitor;
import com.devoxx.model.Favored;
import com.devoxx.model.Favorite;
import com.devoxx.model.Favorites;
import com.devoxx.model.Feedback;
import com.devoxx.model.Floor;
import com.devoxx.model.Location;
import com.devoxx.model.Note;
import com.devoxx.model.Rating;
import com.devoxx.model.RatingData;
import com.devoxx.model.Session;
import com.devoxx.model.SessionId;
import com.devoxx.model.SessionType;
import com.devoxx.model.Speaker;
import com.devoxx.model.Sponsor;
import com.devoxx.model.SponsorBadge;
import com.devoxx.model.TeamMember;
import com.devoxx.model.Track;
import com.devoxx.model.Vote;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.devoxx.views.helper.Util;
import com.devoxx.views.layer.ConferenceLoadingLayer;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.RuntimeArgsService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Alert;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.data.DataClient;
import com.gluonhq.cloudlink.client.data.DataClientBuilder;
import com.gluonhq.cloudlink.client.data.OperationMode;
import com.gluonhq.cloudlink.client.data.RemoteFunctionBuilder;
import com.gluonhq.cloudlink.client.data.RemoteFunctionList;
import com.gluonhq.cloudlink.client.data.RemoteFunctionObject;
import com.gluonhq.cloudlink.client.data.SyncFlag;
import com.gluonhq.cloudlink.client.push.PushClient;
import com.gluonhq.cloudlink.client.user.LoginMethod;
import com.gluonhq.cloudlink.client.user.User;
import com.gluonhq.cloudlink.client.user.UserClient;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.converter.JsonInputConverter;
import com.gluonhq.connect.converter.JsonIterableInputConverter;
import com.gluonhq.connect.provider.DataProvider;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;

public class DevoxxService implements Service {

    private static final Logger LOG = Logger.getLogger(DevoxxService.class.getName());
    private static final String REMOTE_FUNCTION_FAILED_MSG = "Remote function '%s' failed.";

    private static File rootDir;
    static {
        try {
            rootDir = Services.get(StorageService.class)
                    .flatMap(StorageService::getPrivateStorage)
                    .orElseThrow(() -> new IOException("Private storage file not available"));
            deleteOldFiles(rootDir);
            Services.get(RuntimeArgsService.class).ifPresent(ras -> {
                ras.addListener(RuntimeArgsService.LAUNCH_PUSH_NOTIFICATION_KEY, (f) -> {
                    LOG.log(Level.INFO, ">>> received a silent push notification with contents: " + f);
                    LOG.log(Level.INFO, "[DBG] writing reload file");
                    File file = new File (rootDir, DevoxxSettings.RELOAD);
                    try (BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                        br.write(f);
                        LOG.log(Level.INFO, "[DBG] writing silent notification file done");
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        LOG.log(Level.INFO, "[DBG] exception writing reload file " + ex);
                    }
                });
            });

            // Remove Sessions filter key-value
            Services.get(SettingsService.class).ifPresent(ss -> ss.remove(SESSION_FILTER));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private final ReadOnlyObjectWrapper<Conference> conference = new ReadOnlyObjectWrapper<>();

    private final UserClient authenticationClient;
    private final PushClient pushClient;
    private final DataClient localDataClient;
    private final DataClient cloudDataClient;

    private final StringProperty cfpUserUuid = new SimpleStringProperty(this, "cfpUserUuid", "");

    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    /**
     * The sessions field is crucial. It is returned in the
     * retrieveSessions call that is used by the SessionsPresenter. Hence, the content of the sessions
     * directly reflect to the UI.
     */
    private final ReadOnlyListWrapper<Session> sessions = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSessions = new AtomicBoolean(false);
    private final AtomicBoolean retrievingFavoriteSessions = new AtomicBoolean(false);

    private final ReadOnlyListWrapper<Speaker> speakers = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSpeakers = new AtomicBoolean(false);

    private ReadOnlyListWrapper<Track> tracks = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<SessionType> sessionTypes = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<Floor> exhibitionMaps = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    // user specific data
    private ObservableList<Session> favoredSessions;
    private ObservableList<Note> notes;
    private ObservableList<Badge> badges;

    private GluonObservableObject<Favorites> allFavorites;
    private ListChangeListener<Session> internalFavoredSessionsListener = null;
    private ObservableList<Session> internalFavoredSessions = FXCollections.observableArrayList();
    private ObservableList<Favorite> favorites = FXCollections.observableArrayList();

    public DevoxxService() {

        allFavorites = new GluonObservableObject<>();
        allFavorites.setState(ConnectState.SUCCEEDED);

        localDataClient = DataClientBuilder.create()
                .operationMode(OperationMode.LOCAL_ONLY)
                .build();

        authenticationClient = new UserClient();

        cloudDataClient = DataClientBuilder.create()
                .authenticateWith(authenticationClient)
                .operationMode(OperationMode.CLOUD_FIRST)
                .build();

        // enable push notifications and subscribe to the possibly selected conference
        pushClient = new PushClient();
        pushClient.enable(DevoxxNotifications.GCM_SENDER_ID);
        pushClient.enabledProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                Conference conference = getConference();
                if (conference != null) {
                    pushClient.subscribe(conference.getId());
                }
            }
        });

        cfpUserUuid.addListener((obs, ov, nv) -> {
            if ("".equals(nv)) {
                if (internalFavoredSessions != null && internalFavoredSessionsListener != null) {
                    internalFavoredSessions.removeListener(internalFavoredSessionsListener);
                }
            }
        });

        conferenceProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                if (ov != null) {
                    clearCfpAccount();
                    if (authenticationClient.isAuthenticated()) {
                        // Load all authenticated data once user has been authenticated
                        loadCfpAccount(authenticationClient.getAuthenticatedUser(), this::retrieveAuthenticatedUserSessionInformation);
                    }

                    // unsubscribe from previous push notification topic
                    if (pushClient.isEnabled()) {
                        pushClient.unsubscribe(ov.getId());
                    }
                } else {
                    if (authenticationClient.isAuthenticated()) {
                        // Load all authenticated data once user has been authenticated
                        loadCfpAccount(authenticationClient.getAuthenticatedUser(), null);
                    }
                }

                // subscribe to push notification topic, named after the conference id
                if (pushClient.isEnabled()) {
                    pushClient.subscribe(nv.getId());
                }

                retrieveSessionsInternal();
                retrieveSpeakersInternal();
                retrieveTracksInternal();
                retrieveSessionTypesInternal();
                retrieveExhibitionMapsInternal();

                favorites.clear();
                refreshFavorites();
            }
        });

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            String configuredConferenceId = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (configuredConferenceId != null) {
                if (isNumber(configuredConferenceId)) {
                    retrieveConference(configuredConferenceId);
                } else {
                    LOG.log(Level.WARNING, "Found old conference id format, removing it");
                    clearCfpAccount();
                    settingsService.remove(DevoxxSettings.SAVED_CONFERENCE_ID);
                }
            }
        });
    }

    @Override
    public void authenticate(Runnable successRunnable) {
        authenticationClient.authenticate(user -> {
            if (user.getEmail() == null) {
                showEmailAlert();
            } else {
                loadCfpAccount(user, successRunnable);
            }
        });
    }

    @Override
    public void authenticate(Runnable successRunnable, Runnable failureRunnable) {
        authenticationClient.authenticate(user -> {
            if (user.getEmail() == null) {
                showEmailAlert();
            } else {
                loadCfpAccount(user, successRunnable);
            }
        }, message -> {
            if (failureRunnable != null) {
                failureRunnable.run();
            }
        });
    }

    private void showEmailAlert() {
        internalLogOut();
        Alert alert = new Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(DevoxxBundle.getString("OTN.LOGIN.EMAIL_MESSAGE"));
        alert.showAndWait();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationClient.isAuthenticated() && cfpUserUuid.isNotEmpty().get();
    }

    @Override
    public BooleanProperty readyProperty() {
        return ready;
    }

    private boolean loggedOut;

    @Override
    public boolean logOut() {
        loggedOut = false;

        Dialog<Button> dialog = new Dialog<>();
        Placeholder logoutDialogContent = new Placeholder(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.TITLE"), DevoxxBundle.getString("OTN.LOGOUT_DIALOG.CONTENT"), MaterialDesignIcon.HELP);

        // FIXME: Too narrow Dialogs in Glisten
        logoutDialogContent.setPrefWidth(MobileApplication.getInstance().getView().getScene().getWidth() - 40);

        dialog.setContent(logoutDialogContent);
        Button yesButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.YES"));
        Button noButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.NO"));
        yesButton.setOnAction(e -> {
            loggedOut = internalLogOut();
            dialog.hide();
        });
        noButton.setOnAction(e -> dialog.hide());
        dialog.getButtons().addAll(noButton, yesButton);

        dialog.showAndWait();

        return loggedOut;
    }

    private boolean internalLogOut() {
        authenticationClient.signOut();
        java.net.CookieHandler.setDefault(new java.net.CookieManager());
        if (!authenticationClient.isAuthenticated()) {
            clearCfpAccount();
        }
        return true;
    }

    @Override
    public void setConference(Conference conference) {
        this.conference.set(conference);
    }

    @Override
    public Conference getConference() {
        return conference.get();
    }

    @Override
    public ReadOnlyObjectProperty<Conference> conferenceProperty() {
        return conference.getReadOnlyProperty();
    }

    @Override
    public GluonObservableList<Conference> retrievePastConferences() {
        RemoteFunctionList fnConferences = RemoteFunctionBuilder.create("conferences")
                .param("time", "past")
                .param("type", "")
                .list();
        final GluonObservableList<Conference> conferences = fnConferences.call(Conference.class);
        conferences.setOnFailed(e -> LOG.log(
                Level.WARNING,
                String.format(REMOTE_FUNCTION_FAILED_MSG, "conferences" + " in retrievePastConferences()"),
                e.getSource().getException()));
        return conferences;
    }
    
    @Override
    public GluonObservableList<Conference> retrieveConferences() {
        RemoteFunctionList fnConferences = RemoteFunctionBuilder.create("allConferences")
                .list();
        final GluonObservableList<Conference> conferences = fnConferences.call(new JsonIterableInputConverter<>(Conference.class));
        conferences.setOnFailed(e -> LOG.log(Level.WARNING,
                String.format(REMOTE_FUNCTION_FAILED_MSG, "conferences") + " in retrieveConferences()",
                e.getSource().getException()));
        return conferences;
    }

    @Override
    public GluonObservableObject<Conference> retrieveConference(String conferenceId) {
        RemoteFunctionObject fnConference = RemoteFunctionBuilder.create("conference")
                .param("id", conferenceId)
                .object();
        GluonObservableObject<Conference> conference = fnConference.call(Conference.class);

        ready.set(false);
        if (conference.isInitialized()) {
            setConference(conference.get());
            ready.set(true);
        } else {
            conference.setOnSucceeded(e -> {
                if (conference.get() != null) {
                    setConference(conference.get());
                    ready.set(true);
                }
            });
            conference.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "conference"), e.getSource().getException()));
        }
        
        return conference;
    }

    @Override
    public void checkIfReloadRequested() {
        if (rootDir != null && getConference() != null) {
            File reload = new File(rootDir, DevoxxSettings.RELOAD);
            LOG.log(Level.INFO, "Reload requested? " + reload.exists());
            if (reload.exists()) {
                reload.delete();
                retrieveSessionsInternal();
                retrieveSpeakersInternal();
            }
        }
    }

    @Override
    public boolean showRatingDialog() {
        if (getConference() == null) return false;
        if (retrieveSessions().isEmpty()) return false;
        if (!fetchCSVFromLocalStorage(DevoxxSettings.RATING).contains(getConference().getId())) {
            ZonedDateTime dateTimeRating = Util.findLastSessionOfLastDay(this).getStartDate().minusHours(1);
            if (DevoxxSettings.NOTIFICATION_TESTS) {
                dateTimeRating = dateTimeRating.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
            }
            ZonedDateTime currentTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
            // isOnGoing() checks rating dialog to be shown for past conferences
            if (isOnGoing(getConference()) && currentTime.isAfter(dateTimeRating)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ObservableList<Session> reloadSessionsFromCFP(SessionListType sessionListType) {
        if (getConference() != null) {
            if (sessionListType == SessionListType.FAVORITES) {
                favoredSessions = null;
                if (internalFavoredSessions != null && internalFavoredSessionsListener != null) {
                    internalFavoredSessions.removeListener(internalFavoredSessionsListener);
                    internalFavoredSessions.clear();
                    return retrieveFavoredSessions();
                }
            }
        }
        return FXCollections.emptyObservableList();
    }

    @Override
    public Optional<Session> findSession(String uuid) {
        for (Session session : retrieveSessions()) {
            if (session.getTalk() != null && session.getTalk().getId().equals(uuid)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    @Override
    public ReadOnlyListProperty<Session> retrieveSessions() {
        return sessions.getReadOnlyProperty();
    }

    private void retrieveSessionsInternal() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSessions.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving sessions from cfp, just return.");
            return;
        }

        sessions.clear();

        RemoteFunctionList fnSessions = RemoteFunctionBuilder.create("sessionsV2")
                .param("cfpEndpoint", getCfpURL())
                .param("conferenceId", getConference().getCfpVersion())
                .list();

        GluonObservableList<Session> sessionsList = fnSessions.call(Session.class);
        ListChangeListener<Session> sessionsListChangeListener = change -> {
            while (change.next()) {
                for (Session session : change.getAddedSubList()) {
                    session.setStartDate(timeToZonedDateTime(session.getFromTimeMillis(), getConference().getConferenceZoneId()));
                    session.setEndDate(timeToZonedDateTime(session.getToTimeMillis(), getConference().getConferenceZoneId()));
                }
            }
        };
        sessionsList.addListener(sessionsListChangeListener);

        DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
        if (!isAuthenticated()) {
            notifications.preloadRatingNotifications();
        }

        sessionsList.setOnFailed(e -> {
            retrievingSessions.set(false);
            sessionsList.removeListener(sessionsListChangeListener);
            ConferenceLoadingLayer.hide(getConference());
            LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "sessions"), e.getSource().getException());
        });
        sessionsList.setOnSucceeded(e -> {
            retrievingSessions.set(false);
            sessionsList.removeListener(sessionsListChangeListener);
            retrieveAuthenticatedUserSessionInformation();
            finishNotificationsPreloading();
            addLocalNotification();
        });

        sessions.set(sessionsList);
    }

    @Override
    public ReadOnlyListProperty<Speaker> retrieveSpeakers() {
        return speakers.getReadOnlyProperty();
    }

    private void retrieveSpeakersInternal() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSpeakers.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving speakers from cfp, just return.");
            return;
        }

        speakers.clear();

        RemoteFunctionList fnSpeakers = RemoteFunctionBuilder.create("speakers")
                .param("cfpEndpoint", getCfpURL())
                .param("conferenceId", getConference().getCfpVersion())
                .list();

        GluonObservableList<Speaker> speakersList = fnSpeakers.call(Speaker.class);
        speakersList.setOnFailed(e -> {
            retrievingSpeakers.set(false);
            LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "speakers"), e.getSource().getException());
        });
        speakersList.setOnSucceeded(e -> { 
            speakers.setAll(speakersList);
            retrievingSpeakers.set(false);
        });
    }

    @Override
    public ReadOnlyObjectProperty<Speaker> retrieveSpeaker(String uuid) {
        Speaker speakerWithUuid = null;
        for (Speaker speaker : speakers) {
            if (uuid.equals(speaker.getUuid())) {
                speakerWithUuid = speaker;
                break;
            }
        }

        if (speakerWithUuid != null) {
            if (speakerWithUuid.isDetailsRetrieved()) {
                return new ReadOnlyObjectWrapper<>(speakerWithUuid).getReadOnlyProperty();
            } else {
                RemoteFunctionObject fnSpeaker = RemoteFunctionBuilder.create("speaker")
                        .param("cfpEndpoint", getCfpURL())
                        .param("conferenceId", getConference().getCfpVersion())
                        .param("uuid", uuid)
                        .object();

                GluonObservableObject<Speaker> gluonSpeaker = fnSpeaker.call(Speaker.class);
                gluonSpeaker.setOnSucceeded(e -> {
                    updateSpeakerDetails(gluonSpeaker.get());
                });
                gluonSpeaker.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "speaker"), e.getSource().getException()));
                return gluonSpeaker;
            }
        }

        return new ReadOnlyObjectWrapper<>();
    }

    private String getCfpURL() {
        final String cfpURL = getConference().getCfpURL();
        if (cfpURL == null) return "";
        if (cfpURL.endsWith("/api/")) {
            return cfpURL.substring(0, cfpURL.length() - 1);
        }
        if (cfpURL.endsWith("/api")) {
            return cfpURL;
        }
        if (cfpURL.endsWith("/")) {
            return cfpURL + "api";
        }
        return cfpURL + "/api";
    }

    private void updateSpeakerDetails(Speaker updatedSpeaker) {
        for (Speaker speaker : speakers) {
            if (speaker.getUuid().equals(updatedSpeaker.getUuid())) {
                speaker.setAcceptedTalks(updatedSpeaker.getAcceptedTalks());
                speaker.setAvatarURL(updatedSpeaker.getAvatarURL());
                speaker.setBio(updatedSpeaker.getBio());
                speaker.setBioAsHtml(updatedSpeaker.getBioAsHtml());
                speaker.setBlog(updatedSpeaker.getBlog());
                speaker.setCompany(updatedSpeaker.getCompany());
                speaker.setFirstName(updatedSpeaker.getFirstName());
                speaker.setLang(updatedSpeaker.getLang());
                speaker.setLastName(updatedSpeaker.getLastName());
                speaker.setTwitter(updatedSpeaker.getTwitter());
                speaker.setDetailsRetrieved(true);
            }
        }
    }

    @Override
    public ReadOnlyListProperty<Track> retrieveTracks() {
        return tracks.getReadOnlyProperty();
    }

    private void retrieveTracksInternal() {
        if (getConference() != null && getConference().getTracks() != null) {
            tracks.setAll(getConference().getTracks());
        }
    }

    @Override
    public ReadOnlyListProperty<SessionType> retrieveSessionTypes() {
        return sessionTypes.getReadOnlyProperty();
    }

    private void retrieveSessionTypesInternal() {
        if (getConference() != null && getConference().getSessionTypes() != null) {
            Set<String> dedup = new HashSet<>();
            List<SessionType> types = new LinkedList<>();
            for(SessionType t : getConference().getSessionTypes()) {
                if (!dedup.contains(t.getName())) {
                    dedup.add(t.getName());
                    if (!t.isPause()) {
                        types.add(t);
                    }
                }
            }
            sessionTypes.setAll(types);
        }
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Floor> retrieveExhibitionMaps() {
        return exhibitionMaps.getReadOnlyProperty();
    }
    
    @Override
	public GluonObservableList<TeamMember> retrieveTeam() {
        RemoteFunctionList fnSponsors = RemoteFunctionBuilder.create("team")
                .param("conferenceId", getConference().getId())
                .list();
        GluonObservableList<TeamMember> teamMemberList = fnSponsors.call(TeamMember.class);
        teamMemberList.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "team"), e.getSource().getException()));
        return teamMemberList;
	}

	private void retrieveExhibitionMapsInternal() {
        Task<List<Floor>> task = new Task<List<Floor>>() {
            @Override
            protected List<Floor> call() {

                List<Floor> floors = new ArrayList<>();
                for (Floor floor : getConference().getFloorPlans()) {
                    if (floor.getImageURL().startsWith("https")) {
                        floors.add(floor);
                    }
                }

                return floors;
            }
        };

        task.setOnSucceeded(event -> exhibitionMaps.setAll(task.getValue()));

        Thread retrieveExhibitionMapsThread = new Thread(task);
        retrieveExhibitionMapsThread.setDaemon(true);
        retrieveExhibitionMapsThread.start();
    }
	

    @Override
    public GluonObservableList<Sponsor> retrieveSponsors() {
        RemoteFunctionList fnSponsors = RemoteFunctionBuilder.create("sponsors")
                .param("conferenceId", getConference().getId())
                .list();
        GluonObservableList<Sponsor> badgeSponsorsList = fnSponsors.call(Sponsor.class);
        badgeSponsorsList.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "sponsors"), e.getSource().getException()));
        return badgeSponsorsList;
    }

    @Override
	public GluonObservableObject<Sponsor> retrieveSponsorByCode(String sponsorCode) {
    	RemoteFunctionObject fnSponsorByCode = RemoteFunctionBuilder.create("sponsor")
    			.param("conferenceId", getConference().getId())
    			.param("sponsorCode", sponsorCode)
    			.object();
    	GluonObservableObject<Sponsor> sponsor = fnSponsorByCode.call(Sponsor.class);
    	sponsor.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "sponsor"), e.getSource().getException()));
		return sponsor;
	}

	@Override
    public ObservableList<Session> retrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (favoredSessions == null) {
            try {
                DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
                // stop recreating notifications, after the list of scheduled sessions is fully retrieved
                favoredSessions = internalRetrieveFavoredSessions();
                // start recreating notifications as soon as the scheduled sessions are being retrieved
                notifications.preloadFavoriteSessions();
            } catch (IllegalStateException ise) {
                LOG.log(Level.WARNING, "Can't instantiate Notifications when running a background service");
            }
        }

        return favoredSessions;
    }

    @Override
    public String getCfpUserUid() {
        return cfpUserUuid.get();
    }

    private ObservableList<Session> internalRetrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user that was verified at Devoxx CFP must be available when calling this method.");
        }

        retrievingFavoriteSessions.set(true);

        User authUser = getAuthenticatedUser();
        String userUuid = authUser.getKey();
		RemoteFunctionObject fnFavored = RemoteFunctionBuilder.create("favored")
                .param("cfpEndpoint", getCfpURL())
                .param("userUuid", userUuid)
                .object();

        GluonObservableObject<Favored> functionSessions = fnFavored.call(Favored.class);
        functionSessions.setOnSucceeded(e -> {

            for (SessionId sessionId : functionSessions.get().getFavored()) {
                findSession(sessionId.getId()).ifPresent(internalFavoredSessions::add);
            }
            internalFavoredSessionsListener = initializeSessionsListener(internalFavoredSessions, "favored");
            ready.set(true);
            retrievingFavoriteSessions.set(false);
            finishNotificationsPreloading();
        });
        functionSessions.setOnFailed(e -> {
            LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "favored"), e.getSource().getException());
            retrievingFavoriteSessions.set(false);
        });

        return internalFavoredSessions;
    }

    private ListChangeListener<Session> initializeSessionsListener(ObservableList<Session> sessions, String functionPrefix) {
        ListChangeListener<Session> listChangeListener = c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Session session : c.getRemoved()) {
                        LOG.log(Level.INFO, "Removing Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        User authUser = getAuthenticatedUser();
                        String userUuid = authUser.getKey();
						RemoteFunctionObject fnRemove = RemoteFunctionBuilder.create(functionPrefix + "Remove")
                                .param("cfpEndpoint", getCfpURL())
                                .param("userUuid", userUuid)
                                .param("talkId", session.getTalk().getId())
                                .object();
                        GluonObservableObject<String> response = fnRemove.call(String.class);
                        response.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to remove session " + session.getTalk().getId() + " from " + functionPrefix + ": " + response.getException().getMessage()));
                    }
                }
                if (c.wasAdded()) {
                    for (Session session : c.getAddedSubList()) {
                        LOG.log(Level.INFO, "Adding Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        User authUser = getAuthenticatedUser();
                        String userUuid = authUser.getKey();
						RemoteFunctionObject fnAdd = RemoteFunctionBuilder.create(functionPrefix + "Add")
                                .param("cfpEndpoint", getCfpURL())
                                .param("userUuid", userUuid)
                                .param("talkId", session.getTalk().getId())
                                .object();
                        GluonObservableObject<String> response = fnAdd.call(String.class);
                        response.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to add session " + session.getTalk().getId() + " to " + functionPrefix + ": " + response.getException().getMessage()));
                    }
                }
            }
        };
        sessions.addListener(listChangeListener);
        return listChangeListener;
    }

    @Override
    public ObservableList<Note> retrieveNotes() {
        if (!isAuthenticated() && DevoxxSettings.USE_REMOTE_NOTES) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (notes == null) {
            notes = internalRetrieveNotes();
        }

        return notes;
    }

    @Override
    public ObservableList<Badge> retrieveBadges() {
        if (!isAuthenticated() && DevoxxSettings.USE_REMOTE_NOTES) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (badges == null) {
            badges = internalRetrieveBadges();
        }

        return badges;
    }

    @Override
    public GluonObservableList<SponsorBadge> retrieveSponsorBadges(Sponsor sponsor) {
        RemoteFunctionList fnSponsorBadges = RemoteFunctionBuilder.create("sponsorBadges")
                .param("conferenceId", getConference().getId())
                .param("sponsorId", sponsor.getId())
                .cachingEnabled(false)
                .list();
        GluonObservableList<SponsorBadge> badgeSponsorsList = fnSponsorBadges.call(SponsorBadge.class);
        badgeSponsorsList.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "sponsorBadges"), e.getSource().getException()));
        return badgeSponsorsList;
    }

    @Override
    public void logoutSponsor() {
    	// still used?
    }

    @Override
    public void saveSponsorBadge(SponsorBadge sponsorBadge) {
        RemoteFunctionObject fnSponsorBadge = RemoteFunctionBuilder.create("saveSponsorBadge")
        		.param("id", sponsorBadge.getId())
                .param("sponsorId", safeStr(sponsorBadge.getSponsor().getId()))
                .param("name", safeStr(sponsorBadge.getName()))
                .param("email", safeStr(sponsorBadge.getEmail()))
                .param("language", safeStr(sponsorBadge.getLanguage()))
                .param("age", safeStr(sponsorBadge.getAge()+""))
                .param("gender", safeStr(sponsorBadge.getGender()))
                .param("company", safeStr(sponsorBadge.getCompany()))
                .param("city", safeStr(sponsorBadge.getCity()))
                .param("country", safeStr(sponsorBadge.getCountry()))
                .param("programmingLanguages", safeStr(sponsorBadge.getProgrammingLanguages()))
                .param("jobTitle", safeStr(sponsorBadge.getJobTitle()))
                .param("details", safeStr(sponsorBadge.getDetails()))
                .param("timestamp", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .object();        
        GluonObservableObject<String> sponsorBadgeResult = fnSponsorBadge.call(String.class);
        sponsorBadgeResult.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {            	
            	String res = sponsorBadgeResult.get();
            	JSONObject obj = new JSONObject(res);
            	String sponsorBadgeId = obj.getString("_id");
            	if (sponsorBadgeId != null) {
            		sponsorBadge.setId(sponsorBadgeId);
            	}
				LOG.log(Level.INFO, "Response from save sponsor badge: " + res);
            }
        });
        sponsorBadgeResult.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to call save sponsor badge: ", e.getSource().getException()));
    }

    @Override
	public void removeSponsorBadge(SponsorBadge sponsorBadge) {
        RemoteFunctionObject fnRemoveSponsorBadge = RemoteFunctionBuilder.create("removeSponsorBadge")
                .param("sponsorBadgeId", sponsorBadge.getId())
                .object();
        GluonObservableObject<String> removeSponsorBadgeResult = fnRemoveSponsorBadge.call(String.class);
        removeSponsorBadgeResult.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                LOG.log(Level.INFO, "Response from remove sponsor badge: " + removeSponsorBadgeResult.get());
            }
        });
        removeSponsorBadgeResult.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to call remove sponsor badge: ", e.getSource().getException()));
	}

	@Override
    public GluonObservableObject<Location> retrieveLocation() {
        // TODO: issue-56
        // This RF uses https://api.voxxed.com/api/voxxeddays/location/$locationId
        // instead of cfpEndpoint
        RemoteFunctionObject fnLocation = RemoteFunctionBuilder.create("location")
                // .param("cfpEndpoint", getCfpURL())
                .param("locationId", String.valueOf(getConference().getLocationId()))
                .object();
        return fnLocation.call(Location.class);
    }

    @Override
    public ObservableList<RatingData> retrieveVoteTexts(int rating) {
        ObservableList<RatingData> ratingData = FXCollections.observableArrayList();
        RemoteFunctionList fnTexts = RemoteFunctionBuilder.create("voteTexts").list();
        GluonObservableList<Rating> voteTexts = fnTexts.call(Rating.class);
        voteTexts.setOnSucceeded(e -> {
            for (Rating voteText : voteTexts) {
                if (voteText.getRating() == rating) {
                    ratingData.setAll(voteText.getData());
                    break;
                }
            }
        });
        voteTexts.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "voteTexts"), e.getSource().getException()));
        return ratingData;
    }

    @Override
    public GluonObservableObject<String> authenticateSponsor() {
        RemoteFunctionObject fnValidateSponsor = RemoteFunctionBuilder.create("validateSponsor").cachingEnabled(false).object();
        return fnValidateSponsor.call(String.class);
    }

    @Override
    public void voteTalk(Vote vote) {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        User authenticatedUser = authenticationClient.getAuthenticatedUser();
        if (authenticatedUser.getEmail() == null || authenticatedUser.getEmail().isEmpty()) {
            LOG.log(Level.WARNING, "Can not send vote, authenticated user doesn't have an email address.");
        } else {
            RemoteFunctionObject fnVoteTalk = RemoteFunctionBuilder.create("voteTalk")
                    .param("0", getCfpURL())
                    .param("value", String.valueOf(vote.getValue()))
                    .param("email", authenticatedUser.getEmail())
                    .param("talkId", vote.getTalkId())
                    .param("delivery", vote.getDelivery())
                    .param("content", vote.getContent())
                    .param("other", vote.getOther())
                    .object();
            GluonObservableObject<String> voteResult = fnVoteTalk.call(String.class);
            voteResult.initializedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    LOG.log(Level.INFO, "Response from vote: " + voteResult.get());
                }
            });
            voteResult.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "voteTalk"), e.getSource().getException()));
        }
    }

    @Override
    public ObservableList<Favorite> retrieveFavorites() {
        return favorites;
    }

    @Override
    public void refreshFavorites() {
        if (getConference() != null && DevoxxSettings.conferenceHasFavoriteCount(getConference()) && 
                (allFavorites.getState() == ConnectState.SUCCEEDED || allFavorites.getState() == ConnectState.FAILED)) {
            RemoteFunctionObject fnAllFavorites = RemoteFunctionBuilder.create("allFavorites")
                    .param("0", getCfpURL())
                    .object();
            allFavorites = fnAllFavorites.call(new JsonInputConverter<>(Favorites.class));
            allFavorites.setOnSucceeded(e -> {
                for (Favorite favorite : allFavorites.get().getFavorites()) {
                    int index = 0;
                    for (; index < favorites.size(); index++) {
                        if (favorites.get(index).getId().equals(favorite.getId())) {
                            favorites.get(index).setFavs(favorite.getFavs());
                            break;
                        }
                    }
                    if (index == favorites.size()) {
                        favorites.add(favorite);
                    }
                }
                allFavorites.setOnSucceeded(null);
            });
        }
    }
    
    @Override
    public User getAuthenticatedUser() {
        return authenticationClient.getAuthenticatedUser();
    }

    @Override
    public void sendFeedback(Feedback feedback) {
        RemoteFunctionObject fnSendFeedback = RemoteFunctionBuilder.create("sendFeedback")
                .param("name", feedback.getName())
                .param("email", feedback.getEmail())
                .param("message", feedback.getMessage())
                .object();
        fnSendFeedback.call(String.class);
    }

    private ObservableList<Note> internalRetrieveNotes() {
        if (DevoxxSettings.USE_REMOTE_NOTES) {
            return DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_notes",
                    Note.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        } else {
            return DataProvider.retrieveList(localDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_notes",
                    Note.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        }
    }

    private ObservableList<Badge> internalRetrieveBadges() {
        if (DevoxxSettings.USE_REMOTE_NOTES) {
            return DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_badges",
                    Badge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        } else {
            return DataProvider.retrieveList(localDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_badges",
                    Badge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        }
    }

    private void loadCfpAccount(User user, Runnable successRunnable) {
        if (cfpUserUuid.isEmpty().get()) {
            Services.get(SettingsService.class).ifPresent(settingsService -> {
                String devoxxCfpAccountUuid = settingsService.retrieve(DevoxxSettings.SAVED_ACCOUNT_ID);
                if (devoxxCfpAccountUuid == null) {
                    if (user.getLoginMethod() == LoginMethod.Type.CUSTOM) {
                        LOG.log(Level.INFO, "Logged in user " + user + " as account with uuid " + user.getNetworkId());
                        cfpUserUuid.set(user.getNetworkId());
                        settingsService.store(DevoxxSettings.SAVED_ACCOUNT_ID, user.getNetworkId());

                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                    } else {
                        RemoteFunctionObject fnVerifyAccount = RemoteFunctionBuilder.create("verifyAccount")
                                .param("0", getCfpURL())
                                .param("1", user.getNetworkId())
                                .param("2", user.getLoginMethod().name())
                                .param("3", user.getEmail())
                                .object();
                        GluonObservableObject<String> accountUuid = fnVerifyAccount.call(String.class);
                        accountUuid.setOnSucceeded(e -> {
                            LOG.log(Level.INFO, "Verified user " + user + " as account with uuid " + accountUuid);
                            cfpUserUuid.set(accountUuid.get());
                            settingsService.store(DevoxxSettings.SAVED_ACCOUNT_ID, accountUuid.get());

                            if (successRunnable != null) {
                                successRunnable.run();
                            }
                        });
                    }
                } else {
                    LOG.log(Level.INFO, "Verified user " + user + " retrieved from settings " + devoxxCfpAccountUuid);
                    cfpUserUuid.set(devoxxCfpAccountUuid);
                }
            });
        }
    }

    private void retrieveAuthenticatedUserSessionInformation() {
        if (isAuthenticated()) {
            retrieveNotes();
            if (getConference().isMyBadgeActive()) {
                retrieveBadges();
                retrieveSponsors();
            }
            
            if (DevoxxSettings.conferenceHasFavorite(getConference())) {
                retrieveFavoredSessions();
            }
        }
    }

    private void clearCfpAccount() {
        cfpUserUuid.set("");
        notes = null;
        badges = null;
        favoredSessions = null;
        internalFavoredSessions.clear();

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            settingsService.remove(DevoxxSettings.SAVED_ACCOUNT_ID);
            settingsService.remove(DevoxxSettings.BADGE_TYPE);
            settingsService.remove(DevoxxSettings.BADGE_SPONSOR);
        });
    }

    private static ZonedDateTime timeToZonedDateTime(long time, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zoneId);
    }

    private boolean isNumber(String string) {
        try {
            Integer.parseInt(string);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private void addLocalNotification() {
        if (!fetchCSVFromLocalStorage(LOCAL_NOTIFICATION_RATING).contains(getConference().getId())) {
            DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
            notifications.addRatingNotification(getConference());
            addCSVToLocalStorage(LOCAL_NOTIFICATION_RATING, getConference().getId());
        }
    }

    private void finishNotificationsPreloading() {
        if (!retrievingSessions.get() && !retrievingFavoriteSessions.get()) {
            DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
            notifications.preloadingNotificationsDone();
        }
    }

    // This piece of code exists to enable backward compatibility and
    // should be safe to delete after the new version stabilizes
    private static void deleteOldFiles(File rootDir) {
        File versionFile = new File(rootDir, DevoxxSettings.VERSION_NO);
        if (versionFile.exists()) return;
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File c : files) {
                delete(c);
            }
        }
        try {
            versionFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void delete(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) {
                    delete(c);
                }
            }
        }
        if (f.getName().endsWith(".cache") || f.getName().endsWith(".info")) {
            f.delete();
        }
    }
}
