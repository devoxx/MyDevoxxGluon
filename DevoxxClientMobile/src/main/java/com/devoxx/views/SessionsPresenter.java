/*
 * Copyright (c) 2016, 2017, Gluon Software
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
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.ScheduleCell;
import com.devoxx.views.cell.ScheduleHeaderCell;
import com.devoxx.views.helper.FilterSessionsPresenter;
import com.devoxx.views.helper.LoginPrompter;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.BottomNavigation;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SessionsPresenter  extends GluonPresenter<DevoxxApplication> {
    private static final String SESSIONS_PLACEHOLDER_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.ALL_SESSIONS.PLACEHOLDER_MESSAGE");

    private static final String SCHEDULE_LOGIN_PROMPT_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.SCHEDULED_SESSIONS.LOGIN_PROMPT");
    private static final String SCHEDULE_EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.SCHEDULED_SESSIONS.EMPTY_LIST_MESSAGE");

    private static final String FAVORITE_LOGIN_PROMPT_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.LOGIN_PROMPT");
    private static final String FAVORITE_EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.EMPTY_LIST_MESSAGE");

    private static final MaterialDesignIcon SESSIONS_ICON = MaterialDesignIcon.DASHBOARD;
    private static final MaterialDesignIcon SCHEDULER_ICON = MaterialDesignIcon.STAR;
    private static final MaterialDesignIcon FAVORITE_ICON = SessionListType.FAVORITES.getOnIcon();

    private static final PseudoClass PSEUDO_FILTER_ENABLED = PseudoClass.getPseudoClass("filter-enabled");

    private static enum ContentDisplayMode {
        ALL, SCHEDULED, FAVORITE
    }

    @FXML
    private View sessions;

    private CharmListView<Session, LocalDate> scheduleListView;

    @Inject
    private Service service;

    private final Button refreshButton = MaterialDesignIcon.REFRESH.button();

    private final ObjectProperty<Predicate<Session>> filterPredicateProperty = new SimpleObjectProperty<>();
    private SidePopupView filterPopup;
    private FilteredList<Session> filteredSessions;

    private Toggle lastSelectedButton;
    private EventHandler<ActionEvent> currentHandler;

    private GluonView filterSessionsView;
    private FilterSessionsPresenter filterPresenter;
    
    private ToggleButton favoriteButton;

    public void initialize() {
        // navigation between all sessions and the users scheduled sesssions
        if (DevoxxSettings.FAV_AND_SCHEDULE_ENABLED) {
            final BottomNavigation bottomNavigation = createBottomNavigation();
            sessions.setBottom(bottomNavigation);
        } else {
            sessions.setCenter(createSessionsList(ContentDisplayMode.ALL));
        }

        final Button filterButton = createFilterButton();

        sessions.setOnShowing(event -> {

            // Check if we need to switch to News
            final Optional<SettingsService> service = Services.get(SettingsService.class);
            if (service.isPresent()) {
                Boolean switchToNewsView = Boolean.parseBoolean(service.get().retrieve(DevoxxSettings.SWITCH_TO_NEWS));
                if (switchToNewsView) {
                    DevoxxView.NEWS.switchView();
                } else {
                    initialize(filterButton);
                }
            } else {
                initialize(filterButton);
            }
        });

        // Filter
        filterSessionsView = new GluonView(FilterSessionsPresenter.class);
        filterPresenter = (FilterSessionsPresenter) filterSessionsView.getPresenter();
        filterPredicateProperty.bind(filterPresenter.searchPredicateProperty());
        filterButton.pseudoClassStateChanged(PSEUDO_FILTER_ENABLED, filterPresenter.isFilterApplied());
        filterPresenter.filterAppliedProperty().addListener((ov, oldValue, newValue) -> {
            filterButton.pseudoClassStateChanged(PSEUDO_FILTER_ENABLED, newValue);
        });
        MobileApplication.getInstance().addLayerFactory(DevoxxApplication.POPUP_FILTER_SESSIONS_MENU, () -> {
            if (filterPopup == null) {
                filterPopup = new SidePopupView(filterSessionsView.getView(), Side.TOP, true);
                filterPresenter.showingProperty().bind(filterPopup.showingProperty());
            }
            return filterPopup;
        });
    }
    
    private void initialize(Button filterButton) {
        AppBar appBar = getApp().getAppBar();
        appBar.setNavIcon(getApp().getNavMenuButton());
        appBar.setTitleText(DevoxxView.SESSIONS.getTitle());
        appBar.getActionItems().setAll(getApp().getSearchButton(), filterButton);

        // Will never be null
        if (DevoxxSettings.FAV_AND_SCHEDULE_ENABLED) {
            lastSelectedButton.setSelected(true);
            if (currentHandler != null) {
                currentHandler.handle(null);
            }
        }

        if (scheduleListView != null) {
            scheduleListView.setSelectedItem(null);
        }

        // check if a reload was requested, each time the sessions view is opened
        service.checkIfReloadRequested();
        service.refreshFavorites();
    }

    private BottomNavigation createBottomNavigation() {
        BottomNavigation bottomNavigation = new BottomNavigation();

        // Show all sessions
        AppBar appBar = getApp().getAppBar();
        EventHandler<ActionEvent> allHandler = e -> {
            sessions.setCenter(createSessionsList(ContentDisplayMode.ALL));
            appBar.getActionItems().remove(refreshButton);
        };
        ToggleButton sessionsButton = bottomNavigation.createButton(DevoxxBundle.getString("OTN.BUTTON.SESSIONS"), SESSIONS_ICON.graphic(), allHandler);

        // show scheduled sessions
        EventHandler<ActionEvent> scheduleHandler = e -> {
            if (!service.isAuthenticated()) {
                sessions.setCenter(new LoginPrompter(service, SCHEDULE_LOGIN_PROMPT_MESSAGE, SCHEDULER_ICON, () -> sessions.setCenter(createSessionsList(ContentDisplayMode.SCHEDULED))));
            } else {
                sessions.setCenter(createSessionsList(ContentDisplayMode.SCHEDULED));
                if (!appBar.getActionItems().contains(refreshButton)) {
                    appBar.getActionItems().add(0, refreshButton);
                }
            }
        };
        ToggleButton scheduleButton = bottomNavigation.createButton(DevoxxBundle.getString("OTN.BUTTON.MY_SCHEDULE"), SCHEDULER_ICON.graphic(), scheduleHandler);

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
        favoriteButton = bottomNavigation.createButton(DevoxxBundle.getString("OTN.BUTTON.MY_FAVORITES"), FAVORITE_ICON.graphic(), favoriteHandler);

        bottomNavigation.getActionItems().addAll(sessionsButton, scheduleButton, favoriteButton);

        // listen to the selected toggle so we ensure it is selected when the view is returned to
        favoriteButton.getToggleGroup().selectedToggleProperty().addListener((o,ov,nv) -> {
            lastSelectedButton = nv;
            if (nv == sessionsButton) {
                currentHandler = allHandler;
            } else if (nv == scheduleButton) {
                currentHandler = scheduleHandler;
            } else if (nv == favoriteButton) {
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
        if (scheduleListView == null) {
            scheduleListView = new CharmListView<>();
            scheduleListView.getStyleClass().add("schedule-view");
            scheduleListView.setHeadersFunction(s -> s.getStartDate().toLocalDate());
            scheduleListView.setHeaderCellFactory(c -> new ScheduleHeaderCell());
            scheduleListView.setCellFactory(p -> new ScheduleCell(service));
            Comparator<Session> sessionComparator = (s1, s2) -> {
                int compareStartDate = s1.getStartDate().compareTo(s2.getStartDate());
                if (compareStartDate == 0) {
                    return s1.getEndDate().compareTo(s2.getEndDate());
                }
                return compareStartDate;
            };
            scheduleListView.setComparator(sessionComparator);
            scheduleListView.itemsProperty().addListener((InvalidationListener) c -> {
                List<Session> copyOfSessions = new ArrayList<>(filteredSessions);
                Collections.sort(copyOfSessions, sessionComparator);
                updateSessionsDecoration(copyOfSessions);
            });
//            scheduleListView.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//                if (newValue != null) {
//                    DevoxxView.SESSION.switchView().ifPresent(presenter ->
//                        ((SessionPresenter) presenter).showSession(newValue));
//                }
//            });
        }

        if(filteredSessions!= null && filteredSessions.predicateProperty().isBound()) {
            filteredSessions.predicateProperty().unbind();
        }

        switch (mode) {
            case ALL: {
                scheduleListView.setPlaceholder(new Placeholder(SESSIONS_PLACEHOLDER_MESSAGE, SESSIONS_ICON));
                filteredSessions = new FilteredList<>(service.retrieveSessions());
                break;
            }
            case SCHEDULED: {
                scheduleListView.setPlaceholder(new Placeholder(SCHEDULE_EMPTY_LIST_MESSAGE, SCHEDULER_ICON));
                filteredSessions = new FilteredList<>(service.retrieveScheduledSessions());
                refreshButton.setOnAction(e -> {
                    new Toast(DevoxxBundle.getString("OTN.CONTENT_CATALOG.SCHEDULED_SESSIONS.REFRESH")).show();
                    filteredSessions = new FilteredList<>(service.reloadSessionsFromCFP(SessionListType.SCHEDULED));
                    filteredSessions.predicateProperty().bind(filterPredicateProperty);
                    scheduleListView.setItems(filteredSessions);
                });
                break;
            }
            case FAVORITE: {
                scheduleListView.setPlaceholder(new Placeholder(FAVORITE_EMPTY_LIST_MESSAGE, FAVORITE_ICON));
                filteredSessions = new FilteredList<>(service.retrieveFavoredSessions());
                refreshButton.setOnAction(e -> {
                    new Toast(DevoxxBundle.getString("OTN.CONTENT_CATALOG.FAVORITE_SESSIONS.REFRESH")).show();
                    filteredSessions = new FilteredList<>(service.reloadSessionsFromCFP(SessionListType.FAVORITES));
                    filteredSessions.predicateProperty().bind(filterPredicateProperty);
                    scheduleListView.setItems(filteredSessions);
                });
                break;
            }
        }
        filteredSessions.predicateProperty().bind(filterPredicateProperty);
        scheduleListView.setItems(filteredSessions);

        return scheduleListView;
    }
    
    public void selectFavorite() {
        favoriteButton.fire();
    }

    private void updateSessionsDecoration(List<Session> sessions) {
        TimeSlot previousTimeSlot = null;
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
        }
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
