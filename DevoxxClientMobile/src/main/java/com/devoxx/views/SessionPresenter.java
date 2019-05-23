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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.control.DataLabel;
import com.devoxx.model.*;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.dialog.VotePane;
import com.devoxx.views.helper.*;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;

import static com.devoxx.util.DevoxxSettings.TWITTER_URL;

public class SessionPresenter extends GluonPresenter<DevoxxApplication> {

    @FXML
    private View sessionView;

    @Inject
    private Service service;

    @Inject
    private SessionVisuals sessionVisuals;

    private AvatarPane<Speaker> speakerAvatarPane;
    private SessionNotesEditor sessionNotesEditor;

    private Node favoriteBtn;
    private Toggle lastSelectedButton;

    public void initialize() {
        sessionView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.SESSION.getTitle());
            if(lastSelectedButton != null) {
                lastSelectedButton.setSelected(true);
            }
            // Fix for DEVOXX-54
            // This will not hamper the flow since both the buttons are
            // updated whenever a new session is chosen
            if(favoriteBtn != null) {
                appBar.getActionItems().add(favoriteBtn);
            }
        });
        sessionView.setOnHiding(event -> {
            if (service.isAuthenticated() && sessionNotesEditor != null) {
                sessionNotesEditor.saveNotes();
            }
        });
    }

    public void showSession(Session activeSession) {
        showSession(activeSession, Pane.INFO);
    }

    public void showSession(Session activeSession, Pane visiblePane) {
        // details about session (name, time, location)
        sessionView.setTop(createSessionDetails(activeSession));

        // center area is scrollable, populated by the bottom nav bar,
        // already has ScrollPane via FXML

        // navigation between the three views (info, speakers, notes)
        final BottomNavigation bottomNavigation = createBottomNavigation(activeSession);
        sessionView.setBottom(bottomNavigation);

        for (BottomNavigationButton button : bottomNavigation.getActionItems()) {
            if (button.getUserData().equals(visiblePane)) {
                button.fire();
            }
        }

        // update app bar
        final AppBar appBar = sessionView.getApplication().getAppBar();
        appBar.getActionItems().remove(favoriteBtn);
        if (DevoxxSettings.conferenceHasFavorite(service.getConference())) {
            favoriteBtn = sessionVisuals.getFavoriteButton(activeSession);
            appBar.getActionItems().add(favoriteBtn);
        }
    }

    private BottomNavigation createBottomNavigation(final Session session) {

        BottomNavigation bottomNavigation = new BottomNavigation();

        final BottomNavigationButton infoButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.INFO"), MaterialDesignIcon.INFO.graphic(), e -> {
            // when clicked create a label in a scrollpane. Label will contain
            // session summary for this session.
            Label sessionSummary = new Label(session.getSummary());
            sessionSummary.setWrapText(true);
            sessionSummary.getStyleClass().add("session-summary");

            Hyperlink hyperlink = new Hyperlink(session.getRequirements());
            hyperlink.setWrapText(true);
            hyperlink.setOnMousePressed(me -> Util.launchExternalBrowser(() -> session.getRequirements()));

            final FlowPane flowPane = new FlowPane();
            flowPane.getStyleClass().add("tag-container");
            final List<Tag> tags = session.getTalk().getTags();
            if (tags != null) {
                for (Tag tag : tags) {
                    if (tag.getValue() != null && !tag.getValue().isEmpty())
                        flowPane.getChildren().add(createTag(tag.getValue()));
                }
            }

            final ScrollPane scrollPane = createScrollPane(sessionSummary);
            final VBox vBox = new VBox(scrollPane, hyperlink, flowPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            sessionView.setCenter(vBox);
        });
        infoButton.setUserData(Pane.INFO);

        final BottomNavigationButton speakerButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.SPEAKERS"), MaterialDesignIcon.ACCOUNT_CIRCLE.graphic(), e -> {
            // when clicked we create an avatar pane containing all speakers.
            // The entire avatar pane is not scrollable, as we want the speaker
            // avatars to remain fixed. Instead, we make the avatar content area
            // scrollable below.

            // create avatar pane for speakers in this session.
            // We create it now (rather than in showSession) so that the animations play every time.
            speakerAvatarPane = createSpeakerAvatarPane(fetchSpeakers(session));
            sessionView.setCenter(speakerAvatarPane);
        });
        speakerButton.setUserData(Pane.SPEAKER);

        final BottomNavigationButton noteButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.NOTES"), MaterialDesignIcon.MESSAGE.graphic(), e -> {
            if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
                loadAuthenticatedNotesView(session);
            } else {
                LoginPrompter loginPromptView = new LoginPrompter(
                        service,
                        DevoxxBundle.getString("OTN.SESSION.LOGIN_TO_RECORD_NOTES"),
                        MaterialDesignIcon.MESSAGE,
                        () -> loadView(session, Pane.NOTE));
                sessionView.setCenter(loginPromptView);
            }
        });
        noteButton.setUserData(Pane.NOTE);

        final BottomNavigationButton voteButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.VOTE"), MaterialDesignIcon.THUMBS_UP_DOWN.graphic(), e -> {
            if (service.isAuthenticated()) {
                if (isVotingPossible(session)) {
                    // sessionView.setCenter(createVotePane(session));
                    DevoxxView.VOTE.switchView().ifPresent(votePresenter -> {
                        ((VotePresenter)votePresenter).showVote(session); 
                    });
                } else {
                    sessionView.setCenter(new Placeholder(DevoxxBundle.getString("OTN.SESSION.VOTE_TIMING"), MaterialDesignIcon.WARNING));
                }
            } else {
                LoginPrompter loginPromptView = new LoginPrompter(
                        service,
                        DevoxxBundle.getString("OTN.SESSION.LOGIN_TO_VOTE"),
                        MaterialDesignIcon.THUMBS_UP_DOWN,
                        () -> loadView(session, Pane.VOTE));
                sessionView.setCenter(loginPromptView);
            }
        });
        voteButton.setUserData(Pane.VOTE);

        if (session.getTalk() == null || session.getTalk().getSpeakers() == null) {
            bottomNavigation.getActionItems().addAll(infoButton, noteButton);
        } else {
            bottomNavigation.getActionItems().addAll(infoButton, speakerButton, noteButton);
        }
        
        if (DevoxxSettings.conferenceHasVoting(service.getConference())) {
            bottomNavigation.getActionItems().add(voteButton);
        }
        infoButton.fire();

        return bottomNavigation;
    }

    private void loadAuthenticatedNotesView(final Session session) {
        sessionNotesEditor = new SessionNotesEditor(session.getTalk().getId(), service);
        sessionView.setCenter(sessionNotesEditor);
    }
    
    private ObservableList<Speaker> fetchSpeakers(Session activeSession) {
        ObservableList<Speaker> speakers = FXCollections.observableArrayList();
        final ReadOnlyListProperty<Speaker> speakersList = service.retrieveSpeakers();
        if (activeSession.getTalk().getSpeakers() != null) {
            for (TalkSpeaker talkSpeaker : activeSession.getTalk().getSpeakers()) {
                Link link = talkSpeaker.getLink();
                if (link != null && link.getHref() != null && !link.getHref().isEmpty()) {
                    String speakerUUID = link.getHref().substring(link.getHref().lastIndexOf('/') + 1);
                    for (Speaker speaker : speakersList) {
                        if (speaker.getUuid().equals(speakerUUID)) {
                            speakers.add(speaker);
                        }
                    }
                }
            }
        }
        return speakers;
    }
    
    private ReadOnlyObjectProperty<Speaker> fetchSpeakerDetail(String speakerUUID) {
        return service.retrieveSpeaker(speakerUUID);
    }

    private AvatarPane<Speaker> createSpeakerAvatarPane(ObservableList<Speaker> speakers) {
        AvatarPane<Speaker> avatarPane = new AvatarPane<>(speakers);
        avatarPane.setExpanded(true);
        avatarPane.setCollapsible(false);
        avatarPane.setAvatarFactory(Util::getSpeakerAvatar);
        avatarPane.setContentFactory(speaker -> {
            if (speaker == null) {
                return new Placeholder(DevoxxBundle.getString("OTN.SESSION.NO_SPEAKERS"), MaterialDesignIcon.SPEAKER);
            }
            fetchSpeakerDetail(speaker.getUuid());
            
            Label name = new Label(speaker.getFullName());
            name.getStyleClass().add("name");
            name.setWrapText(true);
            GridPane.setHgrow(name, Priority.ALWAYS);

            Label twitter = new Label(speaker.getTwitter());
            twitter.getStyleClass().addAll("speaker-title", "link");
            twitter.setWrapText(true);
            GridPane.setHgrow(twitter, Priority.ALWAYS);
            twitter.setOnMousePressed(e -> Services.get(BrowserService.class).ifPresent(b -> {
                if(speaker.getTwitter() != null && !speaker.getTwitter().isEmpty() && speaker.getTwitter().startsWith("@")) {
                    try {
                        String url = TWITTER_URL + speaker.getTwitter().substring(1);
                        b.launchExternalBrowser(url);
                    } catch (IOException | URISyntaxException ex) {
                        Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                        toast.show();
                    }
                }
            }));            
            
            Label blog = new Label(speaker.getBlog());
            blog.getStyleClass().addAll("speaker-title","link");
            blog.setWrapText(true);
            GridPane.setHgrow(blog, Priority.ALWAYS);
            blog.setOnMousePressed(e -> Services.get(BrowserService.class).ifPresent(b -> {
                if(speaker.getBlog() != null && !speaker.getBlog().isEmpty() && speaker.getBlog().startsWith("http")) {
                    try {
                        String url = speaker.getBlog();
                        b.launchExternalBrowser(url);
                    } catch (IOException | URISyntaxException ex) {
                        Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                        toast.show();
                    }
                }
            }));
            

            Label company = new Label(speaker.getCompany());
            company.getStyleClass().add("company");
            company.setWrapText(true);
            GridPane.setHgrow(company, Priority.ALWAYS);

            Label summary;
            if (speaker.isDetailsRetrieved()) {
                summary = new Label(speaker.getSummary());
            } else {
                summary = new DataLabel();
                speaker.detailsRetrievedProperty().addListener(o ->  summary.setText(speaker.getSummary()));
            }
            summary.getStyleClass().add("summary");
            summary.setWrapText(true);
            GridPane.setHgrow(summary, Priority.ALWAYS);
            GridPane.setVgrow(summary, Priority.ALWAYS);

            Button speakerBtn = MaterialDesignIcon.CHEVRON_RIGHT.button(e -> {
                DevoxxView.SPEAKER.switchView().ifPresent(presenter -> {
                    SpeakerPresenter speakerPresenter = (SpeakerPresenter) presenter;
                    speakerPresenter.setSpeaker(speaker);
                });
            });
            speakerBtn.getStyleClass().add("speaker-btn");
            GridPane.setHgrow(speakerBtn, Priority.NEVER);

            // put everything in its right place
            GridPane gridPane = new GridPane();
            gridPane.getStyleClass().add("content-box");
            gridPane.add(name, 0, 0);
            gridPane.add(company, 0, 1);
            gridPane.add(twitter, 0, 2);
            gridPane.add(blog, 0, 3);
            gridPane.add(speakerBtn, 1, 0, 1, 3);
            gridPane.add(summary, 0, 4, 2, 1);


            return createScrollPane(gridPane);
        });
        return avatarPane;
    }

    private ScrollPane createScrollPane(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        return sp;
    }

    private Node createSessionDetails(Session session) {
        Label sessionTitle = new Label(session.getTitle());
        sessionTitle.getStyleClass().add("header");

        Label sessionInfo = new Label(sessionVisuals.formatMultilineInfo(session));
        sessionInfo.getStyleClass().add("info");

        VBox vbox = new VBox(sessionTitle, sessionInfo);
        vbox.getStyleClass().add("session-info");

        // Check for audience level
        if (session.getTalk().getAudienceLevel() != null) {
            Label audienceLevel = fetchLabelForAudienceLevel(session.getTalk().getAudienceLevel());
            audienceLevel.getStyleClass().add("audience-level");
            vbox.getChildren().add(audienceLevel);
        }
        return vbox;
    }

    private Label fetchLabelForAudienceLevel(String audienceLevel) {
        if (audienceLevel.equalsIgnoreCase("beginner")) {
            return new Label(DevoxxBundle.getString("OTN.SESSION.AUDIENCE_LEVEL.BEGINNER"));
        } else if (audienceLevel.equalsIgnoreCase("middle")) {
            return new Label(DevoxxBundle.getString("OTN.SESSION.AUDIENCE_LEVEL.INTERMEDIATE"));
        } else {
            return new Label(DevoxxBundle.getString("OTN.SESSION.AUDIENCE_LEVEL.EXPERT"));
        }
    }

    private Label createTag(String tagText) {
        final Label tag = new Label(tagText, MaterialDesignIcon.LOCAL_OFFER.graphic());
        tag.getStyleClass().add("tag");
        return tag;
    }

    /**
     * Voting is only enabled if:
     * 1. VOTING_TESTS flag is set to "true"
     * 2. The session has started and it has not passed 12 hours since the conference has ended.
     */
    private boolean isVotingPossible(Session session) {
        ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());        
        return service.getRemoteConfiguration().isTestVotingEnabled() ||
                now.isAfter(session.getStartDate()) &&
                        now.isBefore(service.getConference().getEndDateTime().plusHours(48));
    }

    private VotePane createVotePane(Session activeSession) {
        return new VotePane(service, activeSession);
    }

    private void loadView(Session session, Pane pane) {
        DevoxxView.SESSION.switchView().ifPresent(presenter -> {
            SessionPresenter sessionPresenter = (SessionPresenter) presenter;
            sessionPresenter.showSession(session, pane);
        });
    }

    public enum Pane {
        INFO,
        SPEAKER,
        NOTE,
        VOTE
    }
}
