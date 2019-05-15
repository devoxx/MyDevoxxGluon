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
package com.devoxx.views.helper;

import com.devoxx.model.Favorite;
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.util.JBcnBundle;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.visual.GlistenStyleClasses;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.gluonhq.charm.glisten.visual.MaterialDesignIcon.FAVORITE;
import static com.gluonhq.charm.glisten.visual.MaterialDesignIcon.FAVORITE_BORDER;

@Singleton
public class SessionVisuals {

    private static final Logger LOG = Logger.getLogger(SessionVisuals.class.getName());

    public enum SessionListType {
        FAVORITES("favorites",  "favorite",  FAVORITE_BORDER, FAVORITE);

        private final String id;
        private final MaterialDesignIcon offIcon;
        private final MaterialDesignIcon onIcon;
        private final String style;

        SessionListType(String description, String style, MaterialDesignIcon offIcon, MaterialDesignIcon onIcon) {
            id = description;
            this.style = style;
            this.offIcon = offIcon;
            this.onIcon = onIcon;
        }

        public String getStyleClass() {
            return style;
        }

        public Node getOffGraphic() {
            return offIcon.graphic();
        }

        public Node getOnGraphic() {
            return onIcon.graphic();
        }

        public MaterialDesignIcon getOnIcon() {
            return onIcon;
        }
    }

    @Inject
    private Service service;
    
    @Inject
    private DevoxxNotifications devoxxNotifications;

    private boolean usingOfflineEmptyLists = true;
    private final Map<SessionListType, ObservableList<Session>> cloudLists = new HashMap<>();

    // Method marked with @PostConstruct is called immediately after
    // instance is created and and members are injected
    // It should be used for initialization instead of constructor in DI contexts
    @PostConstruct
    private void init() {
        retrieveLists();
    }

    public Service getService() {
        return service;
    }

    public String formatMultilineInfo(Session session) {
        ZonedDateTime startDate = session.getStartDate();
        ZonedDateTime endDate = session.getEndDate();
        return DevoxxBundle.getString("OTN.VISUALS.FORMAT.MULTILINE",
        		JBcnBundle.getTrackName(session.getTalk().getTrack()),
                service.getConference().getConferenceDayIndex(startDate),
                startDate.format(DevoxxSettings.TIME_FORMATTER),
                endDate.format(DevoxxSettings.TIME_FORMATTER),
                session.getRoomName());
    }


    public String formatOneLineInfo(Session session) {
        ZonedDateTime startDate = session.getStartDate();
        ZonedDateTime endDate = session.getEndDate();
        return DevoxxBundle.getString("OTN.VISUALS.FORMAT.ONELINE",
                service.getConference().getConferenceDayIndex(startDate),
                startDate.format(DevoxxSettings.TIME_FORMATTER),
                endDate.format(DevoxxSettings.TIME_FORMATTER),
                session.getLocation());

    }

    public Node getFavoriteButton(Session session) {
        ToggleButton favButton = buildButton(SessionListType.FAVORITES, session);
        Label favCounter = new Label();
        favCounter.getStyleClass().add("fav-counter");
        Favorite fav = getFavoriteFor(session);

        favCounter.textProperty().bind(fav.favsProperty().asString());
        favCounter.managedProperty().bind(fav.favsProperty().greaterThanOrEqualTo(10));
        HBox favBox = new HBox(favButton, favCounter);
        favBox.getStyleClass().add("fav-box");
        return favBox;
    }

    public Favorite getFavoriteFor(Session session) {
        Optional<Favorite> favorite = Optional.empty();
        for (Favorite fav : service.retrieveFavorites()) {
            if (fav.getId().equals(session.getTalk().getId())) {
                favorite = Optional.of(fav);
                break;
            }
        }
        return favorite.orElseGet(() -> {
            Favorite emptyFavorite = new Favorite();
            emptyFavorite.setId(session.getTalk().getId());
            service.retrieveFavorites().add(emptyFavorite);
            return emptyFavorite;
        });
    }

    private ToggleButton buildButton(SessionListType listType, Session session) {
        ToggleButton button = new ToggleButton("", listType.getOffGraphic());
        button.getStyleClass().addAll(
                listType.getStyleClass(),
                GlistenStyleClasses.BUTTON_FLAT,
                GlistenStyleClasses.BUTTON_ROUND);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        // should be a separate listener or styles are messed up
        button.selectedProperty().addListener((o, ov, selected) -> {
            button.setGraphic(selected ? listType.getOnGraphic() : listType.getOffGraphic());
        });

        // setup  binding between button and appropriate list of session ids
        boolean isSelected = listContains(session, listType);
        button.setSelected(isSelected);
        button.selectedProperty().addListener((o, ov, selected) -> {
            if (button.getUserData() == LOCK) return;

            if (!service.isAuthenticated()) {
                service.authenticate(() -> handleAuthenticatedAction(listType, button, selected), () -> {
                    quietUpdateBtnState(button, () -> button.setSelected(ov));
                });
            } else {
                handleAuthenticatedAction(listType, button, selected);
            }
        });
        updateButton(button, isSelected, session);
        return button;
    }

    private void handleAuthenticatedAction(SessionListType listType, ToggleButton button, boolean selected) {
        // we must use the session stored in the button - not the one provided
        // as an argument to the buildButton method
        Session actualSession = (Session) button.getProperties().get(SESSION_KEY);

        if (selected) {
            listAdd(actualSession, listType);
        } else {
            listRemove(actualSession, listType);
        }
        if (listType == SessionListType.FAVORITES) {
            service.refreshFavorites();
        }
    }

    private static final Object LOCK = new Object();
    private static final String SESSION_KEY = "SESSION";

    private ObservableList<Session> getList(SessionListType listType) {
        if (usingOfflineEmptyLists && service.isAuthenticated()) {
            // OTN-513 - First time user logs in: stop the listener
            devoxxNotifications.preloadingNotificationsDone();
            
            retrieveLists();
        }
        return cloudLists.get(listType);
    }

    /**
     * Is session a part of the list
     *
     * @param session
     * @param listType
     * @return
     */
    private boolean listContains(Session session, SessionListType listType) {
        return getList(listType).contains(session);
    }

    /**
     * Add session to a list
     *
     * @param session
     * @param listType
     */
    private void listAdd(Session session, SessionListType listType) {
        if (!service.isAuthenticated()) {
            service.authenticate(() -> listAdd(session, listType));
        } else {
            retrieveLists();

            if (!listContains(session, listType)) {
                if (listType == SessionListType.FAVORITES) {
                    devoxxNotifications.addFavoriteSessionNotifications(session); 
                }
                Services.get(SettingsService.class).ifPresent(settings -> {
                    String skip = settings.retrieve(DevoxxSettings.SKIP_FAV_DIALOG);
                    if (skip == null || skip.isEmpty() || !Boolean.parseBoolean(skip)) {
                        final Dialog<TextFlow> information = createFavoriteDialog();
                        information.showAndWait();
                        settings.store(DevoxxSettings.SKIP_FAV_DIALOG, Boolean.TRUE.toString());
                    }
                }); 
            }

            getList(listType).add(session);
            showToast(listType, true); 
        } 
    }

    /**
     * Remove session from a list
     *
     * @param session
     * @param listType
     */
    private void listRemove(Session session, SessionListType listType) {
        if (!service.isAuthenticated()) {
            service.authenticate(() -> listRemove(session, listType));
        } else {
            retrieveLists();

            if (listType == SessionListType.FAVORITES) {
                devoxxNotifications.removeFavoriteSessionNotifications(session);
            }

            getList(listType).remove(session);
            showToast(listType, false);
        }
    }

    private void retrieveLists() {
        if (service.isAuthenticated()) {
            cloudLists.put(SessionListType.FAVORITES, service.retrieveFavoredSessions());
            usingOfflineEmptyLists = false;
        } else {
            cloudLists.put(SessionListType.FAVORITES, FXCollections.emptyObservableList());
            usingOfflineEmptyLists = true;
        }
    }

    private void updateButton(ToggleButton button, boolean selected, Session session) {
        quietUpdateBtnState(button, () -> {
            button.setSelected(selected);
            button.getProperties().put(SESSION_KEY, session);
        });
    }

    private void quietUpdateBtnState(ToggleButton button, Runnable r) {
        button.setUserData(LOCK);
        r.run();
        button.setUserData(null);
    }

    private void showToast(SessionListType listType, boolean added) {
        Toast toast = new Toast();
        if (added) {
            if (listType.equals(SessionListType.FAVORITES)) {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_MARKED_AS_FAVORITE"));
            }
        } else {
            if (listType.equals(SessionListType.FAVORITES)) {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_UNFAVORITED"));
            }
        }
        toast.show();
    }

    private Dialog<TextFlow> createFavoriteDialog() {
        final Dialog<TextFlow> information = new Dialog<>();
        final Node graphicFav = MaterialDesignIcon.FAVORITE.graphic();
        final Text favText = new Text(DevoxxBundle.getString("OTN.VISUALS.SESSION.FAVORITE_DIALOG.MSG") + " ");
        graphicFav.getStyleClass().add("fav-dialog-icon");
        favText.getStyleClass().add("text");

        final TextFlow textFlowFav = new TextFlow(favText, graphicFav);
        final Label schFavText = new Label(DevoxxBundle.getString("OTN.VISUALS.SESSION.FAVORITE_DIALOG.MSG_INTRO"));
        schFavText.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.setOnAction(e -> information.hide());
        information.getButtons().add(okButton);

        final VBox content = new VBox(10, schFavText, textFlowFav);
        content.getStyleClass().add("sch-fav-dialog");
        information.setContent(content);
        information.setTitleText(DevoxxBundle.getString("OTN.BUTTON.MY_FAVORITES"));
        return information;
    }
}
