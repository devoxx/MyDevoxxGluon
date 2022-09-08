/*
 * Copyright (c) 2016, 2022, Gluon Software
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
import com.devoxx.model.Conference;
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.ScheduleCell;
import com.devoxx.views.cell.ScheduleHeaderCell;
import com.devoxx.views.helper.FilterSessionsPresenter;
import com.devoxx.views.helper.LoginPrompter;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.BottomNavigation;
import com.gluonhq.charm.glisten.control.BottomNavigationButton;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.Message;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.GlistenStyleClasses;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static com.devoxx.util.DevoxxSettings.SESSION_FILTER;
import static com.devoxx.views.helper.Util.addCSVToLocalStorage;
import static com.devoxx.views.helper.Util.fetchCSVFromLocalStorage;
import static com.devoxx.views.helper.Util.isOnGoing;

public class SessionsPresenter  extends GluonPresenter<DevoxxApplication> {
    
    private static final String SESSIONS_PLACEHOLDER_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.ALL_SESSIONS.PLACEHOLDER_MESSAGE");
    private static final String SESSIONS_PLACEHOLDER_FILTER_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.ALL_SESSIONS.PLACEHOLDER_FILTER_MESSAGE");

    private static final String FAVORITE_LOGIN_PROMPT_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.LOGIN_PROMPT");
    private static final String FAVORITE_EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.EMPTY_LIST_MESSAGE");

    private static final MaterialDesignIcon SESSIONS_ICON = MaterialDesignIcon.DASHBOARD;
    private static final MaterialDesignIcon FAVORITE_ICON = SessionListType.FAVORITES.getOnIcon();

    private static final PseudoClass PSEUDO_FILTER_ENABLED = PseudoClass.getPseudoClass("filter-enabled");

    private enum ContentDisplayMode {
        ALL, FAVORITE
    }

    @FXML
    private View sessions;

    private CharmListView<Session, LocalDate> scheduleListViewAll;
    private CharmListView<Session, LocalDate> scheduleListViewFav;

    @Inject
    private Service service;
    @Inject
    private SessionVisuals sessionVisuals;

    private final Button refreshButton = MaterialDesignIcon.REFRESH.button();

    private final ObjectProperty<Predicate<Session>> filterPredicateProperty = new SimpleObjectProperty<>();
    private SidePopupView filterPopup;
    private FilteredList<Session> filteredSessionsAll;
    private FilteredList<Session> filteredSessionsFav;
    private final ObservableList<Session> sessionsAll = FXCollections.observableArrayList();
    private final ObservableList<Session> sessionsFav = FXCollections.observableArrayList();
    private ContentDisplayMode contentDisplayMode = ContentDisplayMode.ALL;

    private EventHandler<ActionEvent> currentHandler;

    private GluonView filterSessionsView;
    private FilterSessionsPresenter filterPresenter;
    
    private BottomNavigationButton favoriteButton;

    public void initialize() {

        // Filter
        final Button filterButton = createFilterButton();
        filterSessionsView = new GluonView(FilterSessionsPresenter.class);
        filterPresenter = (FilterSessionsPresenter) filterSessionsView.getPresenter();
        filterPredicateProperty.bind(filterPresenter.searchPredicateProperty());
        filterButton.pseudoClassStateChanged(PSEUDO_FILTER_ENABLED, filterPresenter.isFilterApplied());
        filterPresenter.filterAppliedProperty().addListener((ov, oldValue, newValue) -> {
            filterButton.pseudoClassStateChanged(PSEUDO_FILTER_ENABLED, newValue);
        });

        createView();
        service.conferenceProperty().addListener((obs, ov, nv) -> {
            sessions.setBottom(null);
            createView();
        });

        sessions.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.SESSIONS.getTitle());
            appBar.getActionItems().addAll(getApp().getSearchButton(), filterButton);

            // Will never be null
            if (DevoxxSettings.conferenceHasFavorite(service.getConference())) {
                if (currentHandler != null) {
                    currentHandler.handle(null);
                }
            }

            if (getScheduleListViewForMode(contentDisplayMode) != null) {
                getScheduleListViewForMode(contentDisplayMode).setSelectedItem(null);
            }

            // check if a reload was requested, each time the sessions view is opened
            service.checkIfReloadRequested();
            service.refreshFavorites();
            showRatingDialog();

            // Only do this when no rating panel is currently showing
            // If the conference is still on going and viewers are seeing past conferences
            if (sessions.getTop() == null && service.getConference() != null &&
                    isShowingPastSessions(service.getConference(), contentDisplayMode)) {
                sessions.setTop(createFilterGrid());
            }
        });

        MobileApplication.getInstance().addLayerFactory(DevoxxApplication.POPUP_FILTER_SESSIONS_MENU, () -> {
            if (filterPopup == null) {
                filterPopup = new SidePopupView(filterSessionsView.getView(), Side.TOP, true);
                filterPresenter.showingProperty().bind(filterPopup.showingProperty());
            }
            return filterPopup;
        });
    }

    private void createView() {
        // If favorite sessions are disabled, hide bottom navigation
        if (DevoxxSettings.conferenceHasFavorite(service.getConference())) {
            sessions.setBottom(createBottomNavigation());
        } else {
            sessions.setCenter(createSessionsList(ContentDisplayMode.ALL));
        }
    }

    private void showRatingDialog() {
        if (service.showRatingDialog()) {
            final GridPane reviewGrid = createReviewGrid();
            sessions.setTop(reviewGrid);
        } else {
            sessions.setTop(null);
        }
    }

    private BottomNavigation createBottomNavigation() {
        BottomNavigation bottomNavigation = new BottomNavigation();

        // Show all sessions
        AppBar appBar = getApp().getAppBar();
        EventHandler<ActionEvent> allHandler = e -> {
            sessions.setCenter(createSessionsList(ContentDisplayMode.ALL));
            appBar.getActionItems().remove(refreshButton);
        };
        BottomNavigationButton sessionsButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.SESSIONS"), SESSIONS_ICON.graphic(), allHandler);
        
        // show favorite sessions
        EventHandler<ActionEvent> favoriteHandler = e -> {
            if (!service.isAuthenticated()) {
                sessions.setCenter(new LoginPrompter(service, FAVORITE_LOGIN_PROMPT_MESSAGE, FAVORITE_ICON, () -> sessions.setCenter(createSessionsList(ContentDisplayMode.FAVORITE))));
            } else {
                sessions.setCenter(createSessionsList(ContentDisplayMode.FAVORITE));
                if (!appBar.getActionItems().contains(refreshButton)) {
                    appBar.getActionItems().add(0, refreshButton);
                }
            }
        };
        favoriteButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.MY_FAVORITES"), FAVORITE_ICON.graphic(), favoriteHandler);

        bottomNavigation.getActionItems().addAll(sessionsButton, favoriteButton);

        // listen to the selected toggle so we ensure it is selected when the view is returned to
        sessionsButton.selectedProperty().addListener((o,ov,nv) -> {
            if (nv) {
                currentHandler = allHandler;
            }
        });
        favoriteButton.selectedProperty().addListener((o,ov,nv) -> {
            if (nv) {
                currentHandler = favoriteHandler;
            }
        });
        sessionsButton.setSelected(true);

        return bottomNavigation;
    }

    private Button createFilterButton() {
        Button filterButton = MaterialDesignIcon.FILTER_LIST.button();
        filterButton.getStyleClass().add("filter-button");
        filterButton.setOnAction(a -> MobileApplication.getInstance().showLayer(DevoxxApplication.POPUP_FILTER_SESSIONS_MENU));
        return filterButton;
    }

    private Node createSessionsList(ContentDisplayMode mode) {
        contentDisplayMode = mode;

        if (getFilteredSessionsList(mode) != null && getFilteredSessionsList(mode).predicateProperty().isBound()) {
            getFilteredSessionsList(mode).predicateProperty().unbind();
        }

        CharmListView<Session, LocalDate> scheduleListViewForMode = getScheduleListViewForMode(mode);

        switch (mode) {
            case ALL: {
                scheduleListViewForMode.placeholderProperty().bind(Bindings.createObjectBinding(() -> {
                    if (filterPresenter.isFilterApplied()) {
                        return new Placeholder(SESSIONS_PLACEHOLDER_FILTER_MESSAGE, SESSIONS_ICON);
                    }
                    return new Placeholder(SESSIONS_PLACEHOLDER_MESSAGE, SESSIONS_ICON);
                }, filterPresenter.filterAppliedProperty()));
                sessionsAll.setAll(service.retrieveSessions());
                break;
            }
            case FAVORITE: {
                scheduleListViewForMode.placeholderProperty().bind(Bindings.createObjectBinding(() -> {
                    if (filterPresenter.isFilterApplied()) {
                        return new Placeholder(SESSIONS_PLACEHOLDER_FILTER_MESSAGE, FAVORITE_ICON);
                    }
                    return new Placeholder(FAVORITE_EMPTY_LIST_MESSAGE, FAVORITE_ICON);
                }, filterPresenter.filterAppliedProperty()));
                sessionsFav.setAll(service.retrieveFavoredSessions());
                refreshButton.setOnAction(e -> {
                    new Toast(DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.REFRESH")).show();
                    AppBar appBar = MobileApplication.getInstance().getAppBar();
                    appBar.setProgressBarVisible(true);
                    service.reloadSessionsFromCFP(SessionListType.FAVORITES,
                            () -> {
                                sessionsFav.setAll(service.retrieveFavoredSessions());
                                appBar.setProgressBarVisible(false);
                            },
                            () -> appBar.setProgressBarVisible(false));
                });
                break;
            }
        }
        getFilteredSessionsList(mode).predicateProperty().bind(filterPredicateProperty);
        return scheduleListViewForMode;
    }

    private CharmListView<Session, LocalDate> getScheduleListViewForMode(ContentDisplayMode mode) {
        if (mode == ContentDisplayMode.FAVORITE) {
            if (scheduleListViewFav == null) {
                scheduleListViewFav = getScheduleListView(mode);
            }
            return scheduleListViewFav;
        }
        if (scheduleListViewAll == null) {
            scheduleListViewAll = getScheduleListView(mode);
        }
        return scheduleListViewAll;
    }

    private CharmListView<Session, LocalDate> getScheduleListView(ContentDisplayMode mode) {
        CharmListView<Session, LocalDate> scheduleListView = new CharmListView<>();
        scheduleListView.getStyleClass().add("schedule-view");
        scheduleListView.setHeadersFunction(s -> s.getStartDate().toLocalDate());
        scheduleListView.setHeaderCellFactory(c -> new ScheduleHeaderCell());
        scheduleListView.setCellFactory(p -> new ScheduleCell(service, sessionVisuals, false, true));
        Comparator<Session> sessionComparator = (s1, s2) -> {
            int compareStartDate = s1.getStartDate().compareTo(s2.getStartDate());
            if (compareStartDate == 0) {
                int compareEndDate = s1.getEndDate().compareTo(s2.getEndDate());
                if (compareEndDate == 0) {
                    if (s1.getTalk() != null && s2.getTalk() != null) {
                        return s1.getTalk().getTalkType().compareTo(s2.getTalk().getTalkType());
                    }
                }
                return compareEndDate;
            }
            return compareStartDate;
        };
        scheduleListView.setComparator(sessionComparator);
        scheduleListView.itemsProperty().addListener((InvalidationListener) c -> {
            List<Session> copyOfSessions = new ArrayList<>(getFilteredSessionsList(mode));
            Collections.sort(copyOfSessions, sessionComparator);
            updateSessionsDecoration(copyOfSessions);
        });
        scheduleListView.setItems(getFilteredSessionsList(mode));
        return scheduleListView;
    }

    private FilteredList<Session> getFilteredSessionsList(ContentDisplayMode mode) {
        if (mode == ContentDisplayMode.FAVORITE) {
            if (filteredSessionsFav == null) {
                filteredSessionsFav = new FilteredList<>(sessionsFav);
            }
            return filteredSessionsFav;
        }
        if (filteredSessionsAll == null) {
            filteredSessionsAll = new FilteredList<>(sessionsAll);
        }
        return filteredSessionsAll;
    }

    public void selectFavorite() {
        favoriteButton.fire();
    }

    /**
     * Adds either white or gray background to session cells
     * grouping them according to their time-slot.
     */
    private void updateSessionsDecoration(List<Session> sessions) {
        TimeSlot previousTimeSlot = null;
        Session previousSessionWithType = null;
        boolean colorFlag = true;
        for (Session session : sessions) {
            ZonedDateTime sessionStartDateTime = session.getStartDate();
            ZonedDateTime sessionEndDateTime = session.getEndDate();
            TimeSlot timeSlot = new TimeSlot(sessionStartDateTime, sessionEndDateTime);
            if (!timeSlot.equals(previousTimeSlot)) {
                previousTimeSlot = timeSlot;
                colorFlag = !colorFlag;
            }
            session.setDecorated(colorFlag);

            session.setShowSessionType(false);
            if (session.getBreak() != null) continue;
            if (previousSessionWithType == null ||
                    session.getStartDate().toLocalDate().toEpochDay() > previousSessionWithType.getStartDate().toLocalDate().toEpochDay() ||
                    (session.getTalk() != null && previousSessionWithType.getTalk() != null &&
                            !session.getTalk().getTalkType().equals(previousSessionWithType.getTalk().getTalkType()))) {
                previousSessionWithType = session;
                session.setShowSessionType(true);
            }
        }
    }
    
    private GridPane createReviewGrid() {
        final GridPane gridPane = new GridPane();
        final HBox buttons = new HBox();
        final Label header = new Label(DevoxxBundle.getString("OTN.FEEDBACK.QUESTION.DEVOXX"));
        final Button no = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.NO"));
        final Button yes = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.YES"));
        final Button later = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.LATER"));
        final ImageView devoxxLogo = createDevoxxLogo();

        buttons.getChildren().addAll(no, yes);
        header.getStyleClass().add("heading");
        gridPane.getStyleClass().add("review-grid");
        buttons.getStyleClass().add("buttons");
        no.getStyleClass().add("no");
        yes.getStyleClass().add("yes");
        later.setOnAction(e -> {
            sessions.setTop(null);
            addCSVToLocalStorage(DevoxxSettings.RATING, service.getConference().getId());
        });
        no.setOnAction(e -> {
            header.setText(DevoxxBundle.getString("OTN.FEEDBACK.QUESTION.IMPROVE"));
            final Button feedback = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.BUTTON.FEEDBACK"));
            feedback.setOnAction(fe -> {
                later.fire();
                DevoxxView.FEEDBACK.switchView();
            });
            buttons.getChildren().setAll(later, feedback);
        });
        yes.setOnAction(e -> {
            header.setText(DevoxxBundle.getString("OTN.FEEDBACK.QUESTION.RATE"));
            final Button review = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.BUTTON.RATE"));
            review.setOnAction(re -> {
                later.fire();
                Util.requestRating();
            });
            buttons.getChildren().setAll(later, review);
        });
        
        gridPane.add(devoxxLogo, 0, 0, 1, 2);
        gridPane.add(header, 1, 0);
        gridPane.add(buttons, 1, 1);
        GridPane.setHgrow(buttons, Priority.ALWAYS);
        return gridPane;
    }

    private GridPane createFilterGrid() {
        final GridPane gridPane = new GridPane();
        final HBox buttons = new HBox();
        final Label header = new Label(DevoxxBundle.getString("OTN.FILTER.FILTER_GRID.MSG"));
        final Button no = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.NO"));
        final Button yes = createFlatButton(DevoxxBundle.getString("OTN.FEEDBACK.YES"));
        final Node icon = MaterialDesignIcon.FILTER_LIST.graphic();
        icon.getStyleClass().add("filter-grid-icon");

        buttons.getChildren().addAll(no, yes);
        header.getStyleClass().add("heading");
        gridPane.getStyleClass().add("review-grid");
        buttons.getStyleClass().add("buttons");
        no.getStyleClass().add("no");
        yes.getStyleClass().add("yes");

        no.setOnAction(e -> {
            Toast toast = new Toast(DevoxxBundle.getString("OTN.FILTER.FILTER_GRID.TOAST.NO"), Message.LENGTH_LONG);
            toast.show();

            sessions.setTop(null);
            addCSVToLocalStorage(SESSION_FILTER, service.getConference().getId());
        });
        yes.setOnAction(e -> {
            addCSVToLocalStorage(SESSION_FILTER, service.getConference().getId());
            filterPresenter.hidePastSession();
            sessions.setTop(null);
            Toast toast = new Toast(DevoxxBundle.getString("OTN.FILTER.FILTER_GRID.TOAST.YES"), Message.LENGTH_LONG);
            toast.show();
        });

        gridPane.add(icon, 0, 0, 1, 2);
        gridPane.add(header, 1, 0);
        gridPane.add(buttons, 1, 1);
        GridPane.setHgrow(buttons, Priority.ALWAYS);
        return gridPane;
    }

    private ImageView createDevoxxLogo() {
        final ImageView devoxxLogo = new ImageView();
        devoxxLogo.getStyleClass().add("logo");
        devoxxLogo.setPreserveRatio(true);
        devoxxLogo.setFitWidth(64);
        return devoxxLogo;
    }
    
    private Button createFlatButton(String text) {
        final Button button = new Button(text);
        GlistenStyleClasses.applyStyleClass(button, GlistenStyleClasses.BUTTON_FLAT);
        return button;
    }

    private boolean isShowingPastSessions(Conference conference, ContentDisplayMode mode) {
        // Conference is yet to end
        // We haven't shown the filter dialog for this conference in the past
        if (isOnGoing(conference) && !fetchCSVFromLocalStorage(SESSION_FILTER).contains(conference.getId())) {
            for (Session filteredSession : getFilteredSessionsList(mode)) {
                if (filteredSession.getEndDate().plusHours(1).isBefore(ZonedDateTime.now(conference.getConferenceZoneId()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Time-slot of a session based on it's start and end date-time.
     */
    private class TimeSlot {

        private final ZonedDateTime startDateTime;
        private final ZonedDateTime endDateTime;

        TimeSlot(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if(object instanceof TimeSlot) {
                TimeSlot obj = (TimeSlot) object;
                return startDateTime.equals(obj.startDateTime) && endDateTime.equals(obj.endDateTime);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (startDateTime.hashCode() ^ endDateTime.hashCode()) + (int) ChronoUnit.MILLIS.between(startDateTime, endDateTime);
        }
    }
}
