/*
 * Copyright (c) 2016, 2018, Gluon Software
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
package com.devoxx.views.cell;

import com.devoxx.DevoxxView;
import com.devoxx.model.Favorite;
import com.devoxx.model.Session;
import com.devoxx.model.TalkSpeaker;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.SessionPresenter;
import com.devoxx.views.helper.SessionVisuals;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScheduleCell extends CharmListCell<Session> {

    private static final PseudoClass PSEUDO_CLASS_COLORED = PseudoClass.getPseudoClass("color");
    private static Map<String, PseudoClass> trackPseudoClassMap = new HashMap<>();
    private static List<String> pseudoClasses = Arrays.asList("track-color0",
            "track-color1", "track-color2", "track-color3",
            "track-color4", "track-color5", "track-color6",
            "track-color7", "track-color8", "track-color9"
    );
    private static int index = 0;
    
    private final Service service;
    private final ListTile listTile;
    private final SecondaryGraphic secondaryGraphic;
    private final Label trackLabel;
    private Session session;
    private boolean showDate;
    private PseudoClass oldPseudoClass;

    public ScheduleCell(Service service) {
        this(service, false);
    }

    public ScheduleCell(Service service, boolean showDate) {
        this.service = service;
        this.showDate = showDate;
        
        trackLabel = new Label();
        secondaryGraphic = new SecondaryGraphic();

        listTile = new ListTile();
        listTile.setWrapText(true);
        listTile.setPrimaryGraphic(new Group(trackLabel));
        listTile.setSecondaryGraphic(secondaryGraphic);

        setText(null);
        getStyleClass().add("schedule-cell");
    }

    @Override
    public void updateItem(Session item, boolean empty) {
        super.updateItem(item, empty);
        session = item;
        if (item != null && !empty) {
            updateListTile();
            secondaryGraphic.updateGraphic(session);
            setGraphic(listTile);

            // FIX for OTN-568
            listTile.setOnMouseReleased(event -> {
                DevoxxView.SESSION.switchView().ifPresent(presenter ->
                        ((SessionPresenter) presenter).showSession(session));
            });
        } else {
            setGraphic(null);
        }
    }

    private void updateListTile() {
        if (session.getTalk() != null) {
            if (session.getTalk().getTrack() != null) {
                final String trackId = session.getTalk().getTrackId().toUpperCase();
                trackLabel.setText(trackId);
                changePseudoClass(fetchPseudoClassForTrack(trackId));
            }
            
            if (session.getTalk().getTitle() != null) {
                listTile.setTextLine(0, session.getTalk().getTitle());
            }

            List<TalkSpeaker> speakers = session.getTalk().getSpeakers();
            listTile.setTextLine(1, convertSpeakersToString(speakers));
        }

        listTile.setTextLine(2, DevoxxBundle.getString("OTN.SCHEDULE.IN_AT",
                session.getRoomName(),
                DevoxxSettings.TIME_FORMATTER.format(session.getStartDate()),
                DevoxxSettings.TIME_FORMATTER.format(session.getEndDate())) +
                (showDate ? "\n" + DevoxxSettings.DATE_FORMATTER.format(session.getStartDate()) : ""));

        Optional<Favorite> favorite = Optional.empty();
        for (Favorite fav : service.retrieveFavorites()) {
            if (fav.getId().equals(session.getTalk().getId())) {
                favorite = Optional.of(fav);
                break;
            }
        }
        Favorite fav = favorite.orElseGet(() -> {
            Favorite emptyFavorite = new Favorite();
            emptyFavorite.setId(session.getTalk().getId());
            service.retrieveFavorites().add(emptyFavorite);
            return emptyFavorite;
        });

        // Hacky Code as it uses internals of ListTile
        Label label = (Label) ((VBox) listTile.getChildren().get(0)).getChildren().get(2);
        Label favLabel = new Label();
        favLabel.textProperty().bind(fav.favsProperty().asString());
        favLabel.visibleProperty().bind(fav.favsProperty().greaterThanOrEqualTo(10));
        favLabel.getStyleClass().add("fav-label");
        Node graphic = MaterialDesignIcon.FAVORITE.graphic();
        graphic.getStyleClass().add("fav-graphic");
        favLabel.setGraphic(graphic);
        favLabel.prefHeightProperty().bind(label.heightProperty());
        label.setGraphic(favLabel);
        label.setContentDisplay(ContentDisplay.RIGHT);

        pseudoClassStateChanged(PSEUDO_CLASS_COLORED, session.isDecorated());
    }

    private String convertSpeakersToString(List<TalkSpeaker> speakers) {
        if (speakers.size() > 0) {
            StringBuilder speakerTitle = new StringBuilder();
            for (int index = 0; index < speakers.size(); index++) {
                if (index < speakers.size() - 1) {
                    speakerTitle.append(speakers.get(index).getName()).append(", ");
                } else {
                    speakerTitle.append(speakers.get(index).getName());
                }
            }
            return speakerTitle.toString();
        }
        return "";
    }
    
     private PseudoClass fetchPseudoClassForTrack(String trackId) {
        PseudoClass pseudoClass = trackPseudoClassMap.get(trackId);
        if (pseudoClass == null) {
            if (index > pseudoClasses.size() - 1) {
                index = 0; // exhausted all colors, re-use
            }
            pseudoClass = PseudoClass.getPseudoClass(pseudoClasses.get(index++));
            trackPseudoClassMap.put(trackId, pseudoClass);
        }
        return pseudoClass;
    }

    private void changePseudoClass(PseudoClass pseudoClass) {
        pseudoClassStateChanged(oldPseudoClass, false);
        pseudoClassStateChanged(pseudoClass, true);
        oldPseudoClass = pseudoClass;
    }

    private class SecondaryGraphic extends Pane {

        private final Node chevron;
        private StackPane indicator;
        private Session currentSession;

        public SecondaryGraphic() {
            chevron = MaterialDesignIcon.CHEVRON_RIGHT.graphic();
            indicator = createIndicator(SessionVisuals.SessionListType.SCHEDULED, true);
            getChildren().addAll(chevron, indicator);
            InvalidationListener il = (Observable observable) -> {
                if (currentSession != null) {
                    updateGraphic(currentSession);
                }
            };
            if (service.isAuthenticated()) {
                service.retrieveScheduledSessions().addListener(il);
                service.retrieveFavoredSessions().addListener(il);
            }
        }

        @Override protected void layoutChildren() {
            final double w = getWidth();
            final double h = getHeight();

            double indicatorWidth = indicator.prefWidth(-1);
            double indicatorHeight = indicator.prefHeight(-1);
            indicator.resizeRelocate(w - indicatorWidth, 0, indicatorWidth, indicatorHeight);

            double chevronWidth = chevron.prefWidth(-1);
            double chevronHeight = chevron.prefHeight(-1);
            chevron.resizeRelocate(0, h / 2.0 - chevronHeight / 2.0, chevronWidth, chevronHeight);
        }

        public void updateGraphic(Session session) {
            // FIXME this doesn't work as the retrieve* calls return empty lists on first call, resulting in graphics
            // not being shown.
            currentSession = session;
            final boolean authenticated = service.isAuthenticated();

            if ( authenticated && service.retrieveScheduledSessions().contains(session)) {
                resetIndicator( indicator, SessionVisuals.SessionListType.SCHEDULED);
                indicator.setVisible(true);
            } else if ( authenticated && service.retrieveFavoredSessions().contains(session)) {
                resetIndicator( indicator, SessionVisuals.SessionListType.FAVORITES);
                indicator.setVisible(true);
            } else {
                indicator.setVisible(false);
            }
        }

        private void resetIndicator(StackPane indicator, SessionVisuals.SessionListType style) {
            if (!indicator.getStyleClass().contains(style.getStyleClass()) ) {
                final Node graphic = style.getOnGraphic();
                StackPane.setAlignment(graphic, Pos.TOP_RIGHT);
                indicator.getChildren().set(0, graphic);

                indicator.getStyleClass().remove( style.other().getStyleClass());
                indicator.getStyleClass().add( style.getStyleClass());
            }
        }

        private StackPane createIndicator(SessionVisuals.SessionListType style, boolean topRight) {
            Node graphic = style.getOnGraphic();
            StackPane node = new StackPane(graphic);
            StackPane.setAlignment(graphic, topRight ? Pos.TOP_RIGHT : Pos.BOTTOM_RIGHT);

            node.setVisible(false);
            node.getStyleClass().addAll("indicator", style.getStyleClass());

            GridPane.setVgrow(node, Priority.ALWAYS);
            GridPane.setHalignment(node, HPos.RIGHT);

            return node;
        }
    }
}
