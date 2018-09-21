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

import com.airhacks.afterburner.injection.Injector;
import com.devoxx.model.*;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.RuntimeArgsService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.data.*;
import com.gluonhq.cloudlink.client.push.PushClient;
import com.gluonhq.cloudlink.client.user.LoginMethod;
import com.gluonhq.cloudlink.client.user.User;
import com.gluonhq.cloudlink.client.user.UserClient;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.converter.JsonInputConverter;
import com.gluonhq.connect.provider.DataProvider;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.util.DevoxxSettings.DAYS_PAST_END_DATE;
import static com.devoxx.views.helper.Util.*;

public class DevoxxService implements Service {

    private static final Logger LOG = Logger.getLogger(DevoxxService.class.getName());
    private static final String REMOTE_FUNCTION_FAILED_MSG = "Remote function '%s' failed.";

//    private static final String DEVOXX_CFP_DATA_URL = "https://s3-eu-west-1.amazonaws.com/cfpdevoxx/cfp.json";

    private static File rootDir;
    static {
        try {
            rootDir = Services.get(StorageService.class)
                    .flatMap(StorageService::getPrivateStorage)
                    .orElseThrow(() -> new IOException("Private storage file not available"));
            Services.get(RuntimeArgsService.class).ifPresent(ras -> {
                ras.addListener(RuntimeArgsService.LAUNCH_PUSH_NOTIFICATION_KEY, (f) -> {
                    LOG.log(Level.INFO, ">>> received a silent push notification with contents: " + f);
                    LOG.log(Level.INFO, "[DBG] writing reload file");
                    File file = null;
                    if (isReloadNotification(f)) {
                        LOG.log(Level.INFO, "Reload notification found");
                        file = new File (rootDir, DevoxxSettings.RELOAD);
                    } else if (isRatingNotification(f)) {
                        LOG.log(Level.INFO, "Rating notification found");
                        file = new File (rootDir, DevoxxSettings.RATING);
                    }
                    if (file != null) {
                        try (BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                            br.write(f);
                            LOG.log(Level.INFO, "[DBG] writing silent notification file done");
                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                            LOG.log(Level.INFO, "[DBG] exception writing reload file " + ex);
                        }
                    }
                });
            });
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private final ReadOnlyObjectWrapper<Conference> conference = new ReadOnlyObjectWrapper<>();

    private final UserClient authenticationClient;
    private final PushClient pushClient;
    private final DataClient localDataClient;
    private final DataClient cloudDataClient;
    private final DataClient nonAuthenticatedCloudDataClient;

    private final StringProperty cfpUserUuid = new SimpleStringProperty(this, "cfpUserUuid", "");

    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    /**
     * The sessions field is crucial. It is returned in the
     * retrieveSessions call that is used by the SessionsPresenter. Hence, the content of the sessions
     * directly reflect to the UI.
     */
    private final ReadOnlyListWrapper<Session> sessions = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSessions = new AtomicBoolean(false);

    private final ReadOnlyListWrapper<Speaker> speakers = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSpeakers = new AtomicBoolean(false);

    private ReadOnlyListWrapper<Track> tracks = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<ProposalType> proposalTypes = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<Floor> exhibitionMaps = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    // user specific data
    private ObservableList<Session> favoredSessions;
    private ObservableList<Note> notes;
    private ObservableList<Badge> badges;
    private ObservableList<SponsorBadge> sponsorBadges;

    private GluonObservableObject<Favorites> allFavorites;
    private ListChangeListener<Session> internalFavoredSessionsListener = null;
    private ObservableList<Session> internalFavoredSessions = FXCollections.observableArrayList();
    private ObservableList<Favorite> favorites = FXCollections.observableArrayList();

    public DevoxxService() {
        ready.set(false);

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

        nonAuthenticatedCloudDataClient = DataClientBuilder.create()
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
                retrieveProposalTypesInternal();
                retrieveExhibitionMapsInternal();

                favorites.clear();
                refreshFavorites();

                if (isConferenceFromPast(nv)) {
                    showPastConferenceMessage();
                } else {
                    hidePastConferenceMessage();
                }
            }
        });

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            String configuredConferenceId = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (configuredConferenceId != null) {
                try {
                    retrieveConference(configuredConferenceId);
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Found old conference id format, removing it");
                    settingsService.remove(DevoxxSettings.SAVED_CONFERENCE_ID);
                }
            }
        });
    }

    /**
     * A conference is considered from past if
     * {@link DevoxxSettings#DAYS_PAST_END_DATE} days have passed since its end date
     */
    private boolean isConferenceFromPast(Conference conference) {
        return conference.getDaysUntilEnd() < -(DAYS_PAST_END_DATE);
    }

    @Override
    public void authenticate(Runnable successRunnable) {
        authenticationClient.authenticate(user -> loadCfpAccount(user, successRunnable));
    }

    @Override
    public void authenticate(Runnable successRunnable, Runnable failureRunnable) {
        authenticationClient.authenticate(user -> loadCfpAccount(user, successRunnable), message -> {
            if (failureRunnable != null) {
                failureRunnable.run();
            }
        });
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
    public GluonObservableList<Conference> retrieveConferences(Conference.Type type) {
        RemoteFunctionList fnConferences = RemoteFunctionBuilder.create("conferences")
                .param("time", "future")
                .param("type", type.name())
                .list();
        final GluonObservableList<Conference> conferences = fnConferences.call(Conference.class);
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
                String conferenceIdForReload = readConferenceIdFromFile(reload);
                LOG.log(Level.INFO, "Reload requested for conference: " + conferenceIdForReload + ", current conference: " + getConference().getId());
                LOG.log(Level.INFO, "[DBB] reload exists for conference: '" + conferenceIdForReload + "', current conference: '" + getConference().getId()+"'");
                if (!conferenceIdForReload.isEmpty() && conferenceIdForReload.equalsIgnoreCase(getConference().getId())) {
                    reload.delete();
                    retrieveSessionsInternal();
                    retrieveSpeakersInternal();
                    LOG.log(Level.INFO, "[DBB] data reloading for "+conferenceIdForReload);
                }
            }
        }
    }

    @Override
    public boolean showRatingDialog() {
        if (rootDir != null) {
            File rating = new File(rootDir, DevoxxSettings.RATING);
            LOG.log(Level.INFO, "Rating requested? " + rating.exists());
            if (rating.exists()) {
                rating.delete();
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

        RemoteFunctionList fnSessions = RemoteFunctionBuilder.create("sessions")
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
        sessionsList.setOnFailed(e -> {
            retrievingSessions.set(false);
            sessionsList.removeListener(sessionsListChangeListener);
            LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "sessions"), e.getSource().getException());
        });
        sessionsList.setOnSucceeded(e -> {
            retrievingSessions.set(false);
            sessionsList.removeListener(sessionsListChangeListener);
            retrieveAuthenticatedUserSessionInformation();
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
        return cfpURL.endsWith("/api") ? cfpURL : cfpURL + "/api";
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
    public ReadOnlyListProperty<ProposalType> retrieveProposalTypes() {
        return proposalTypes.getReadOnlyProperty();
    }

    private void retrieveProposalTypesInternal() {
        RemoteFunctionObject fnProposalTypes = RemoteFunctionBuilder.create("proposalTypes")
                .param("cfpEndpoint", getCfpURL())
                .param("conferenceId", getConference().getCfpVersion())
                .object();

        GluonObservableObject<ProposalTypes> gluonProposalTypes = fnProposalTypes.call(ProposalTypes.class);
        gluonProposalTypes.exceptionProperty().addListener(o -> LOG.log(Level.SEVERE, gluonProposalTypes.getException().getMessage()));
        gluonProposalTypes.addListener((obs, ov, nv) -> proposalTypes.setAll(gluonProposalTypes.get().getProposalTypes()));
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Floor> retrieveExhibitionMaps() {
        return exhibitionMaps.getReadOnlyProperty();
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
    public ObservableList<Session> retrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (favoredSessions == null) {
            try {
                DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
                // stop recreating notifications, after the list of scheduled sessions is fully retrieved
                favoredSessions = internalRetrieveFavoredSessions(notifications::preloadingFavoriteSessionsDone);
                // start recreating notifications as soon as the scheduled sessions are being retrieved
                notifications.preloadFavoriteSessions();
            } catch (IllegalStateException ise) {
                LOG.log(Level.WARNING, "Can't instantiate Notifications when running a background service");
            }
        }

        return favoredSessions;
    }

    private ObservableList<Session> internalRetrieveFavoredSessions(Runnable onStateSucceeded) {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user that was verified at Devoxx CFP must be available when calling this method.");
        }

        RemoteFunctionObject fnFavored = RemoteFunctionBuilder.create("favored")
                .param("0", getCfpURL())
                .param("1", cfpUserUuid.get())
                .object();

        GluonObservableObject<Favored> functionSessions = fnFavored.call(Favored.class);
        functionSessions.setOnSucceeded(e -> {
            for (SessionId sessionId : functionSessions.get().getFavored()) {
                findSession(sessionId.getId()).ifPresent(internalFavoredSessions::add);
            }
            internalFavoredSessionsListener = initializeSessionsListener(internalFavoredSessions, "favored");
            ready.set(true);
            onStateSucceeded.run();
        });
        functionSessions.setOnFailed(e -> LOG.log(Level.WARNING, String.format(REMOTE_FUNCTION_FAILED_MSG, "favored"), e.getSource().getException()));

        return internalFavoredSessions;
    }

    private ListChangeListener<Session> initializeSessionsListener(ObservableList<Session> sessions, String functionPrefix) {
        ListChangeListener<Session> listChangeListener = c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Session session : c.getRemoved()) {
                        LOG.log(Level.INFO, "Removing Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        RemoteFunctionObject fnRemove = RemoteFunctionBuilder.create(functionPrefix + "Remove")
                                .param("0", getCfpURL())
                                .param("1", cfpUserUuid.get())
                                .param("2", session.getTalk().getId())
                                .object();
                        GluonObservableObject<String> response = fnRemove.call(String.class);
                        response.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to remove session " + session.getTalk().getId() + " from " + functionPrefix + ": " + response.getException().getMessage()));
                    }
                }
                if (c.wasAdded()) {
                    for (Session session : c.getAddedSubList()) {
                        LOG.log(Level.INFO, "Adding Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        RemoteFunctionObject fnAdd = RemoteFunctionBuilder.create(functionPrefix + "Add")
                                .param("0", getCfpURL())
                                .param("1", cfpUserUuid.get())
                                .param("2", session.getTalk().getId())
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
    public ObservableList<SponsorBadge> retrieveSponsorBadges(Sponsor sponsor) {
        
        if (sponsorBadges == null) {
            sponsorBadges = internalRetrieveSponsorBadges(sponsor);
        }

        return sponsorBadges;
    }

    @Override
    public void saveSponsorBadge(SponsorBadge sponsorBadge) {
        RemoteFunctionObject fnSponsorBadge = RemoteFunctionBuilder.create("saveSponsorBadge")
                .param("0", safeStr(sponsorBadge.getSponsor().getSlug()))
                .param("1", safeStr(sponsorBadge.getBadgeId()))
                .param("2", safeStr(sponsorBadge.getFirstName()))
                .param("3", safeStr(sponsorBadge.getLastName()))
                .param("4", safeStr(sponsorBadge.getCompany()))
                .param("5", safeStr(sponsorBadge.getEmail()))
                .param("6", safeStr(sponsorBadge.getDetails()))
                .param("7", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .object();
        GluonObservableObject<String> sponsorBadgeResult = fnSponsorBadge.call(String.class);
        sponsorBadgeResult.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                LOG.log(Level.INFO, "Response from save sponsor badge: " + sponsorBadgeResult.get());
            }
        });
        sponsorBadgeResult.setOnFailed(e -> LOG.log(Level.WARNING, "Failed to call save sponsor badge: ", e.getSource().getException()));
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
                    .param("1", String.valueOf(vote.getValue()))
                    .param("2", authenticatedUser.getEmail())
                    .param("3", vote.getTalkId())
                    .param("4", vote.getDelivery())
                    .param("5", vote.getContent())
                    .param("6", vote.getOther())
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

    private GluonObservableList<SponsorBadge> internalRetrieveSponsorBadges(Sponsor sponsor) {
        return DataProvider.retrieveList(nonAuthenticatedCloudDataClient.createListDataReader(getConference().getId() + "_" + sponsor.getSlug() + "_sponsor_badges",
                SponsorBadge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
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
            if (DevoxxSettings.conferenceHasBadgeView(getConference())) {
                retrieveBadges();
                retrieveSponsors();
            }
            
            if (DevoxxSettings.conferenceHasFavorite(getConference())) {
                retrieveFavoredSessions();
            }
        } else {
            ready.set(true);
        }
    }

    private void clearCfpAccount() {
        cfpUserUuid.set("");
        notes = null;
        badges = null;
        sponsorBadges = null;
        favoredSessions = null;
        internalFavoredSessions.clear();
        ready.set(false);

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            settingsService.remove(DevoxxSettings.SAVED_ACCOUNT_ID);
            settingsService.remove(DevoxxSettings.BADGE_TYPE);
            settingsService.remove(DevoxxSettings.BADGE_SPONSOR);
        });
    }

    private String readConferenceIdFromFile(File reload) {
        StringBuilder fileContent = new StringBuilder((int) reload.length());
        try (Scanner scanner = new Scanner(reload)) {
            String lineSeparator = System.getProperty("line.separator");
            while (scanner.hasNextLine()) {
                fileContent.append(scanner.nextLine()).append(lineSeparator);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, "read reload file '" + fileContent.toString() + "'");
        return findConferenceIdFromString(fileContent.toString());
    }

    private String findConferenceIdFromString(String fileContent) {
        try {
            String trimmedContent = fileContent.replaceAll("\"", "")
                    .replaceAll(" ", "")
                    .replaceAll("\\}", ",");
            String[] keyValue = trimmedContent.split(",");
            for (String aKeyValue : keyValue) {
                if (aKeyValue.contains("body")) {
                    return aKeyValue.split(":")[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private static boolean isReloadNotification(String fileContent) {
        // fileContent: {"id":"", "body":"test", "title":"reload"}
        try {
            JsonObject jsonObject = createJsonObject(fileContent);
            JsonString title = (JsonString) jsonObject.get("title");
            return title != null && title.getString().equalsIgnoreCase(DevoxxSettings.RELOAD);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    private static boolean isRatingNotification(String fileContent) {
        // fileContent: {"id":"", "body":"test", "title":"rating"}
        try {
            JsonObject jsonObject = createJsonObject(fileContent);
            JsonString title = (JsonString) jsonObject.get("title");
            return title != null && title.getString().equalsIgnoreCase(DevoxxSettings.RATING);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    private static JsonObject createJsonObject(String fileContent) {
        JsonReader reader = Json.createReader(new StringReader(fileContent));
        return (JsonObject) reader.read();
    }

    private static ZonedDateTime timeToZonedDateTime(long time, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zoneId);
    }
}
