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
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.views.cell.ConferenceCell;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.StatusBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.layout.layer.MenuPopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static com.devoxx.views.helper.Util.hidePastConferenceMessage;
import static com.devoxx.views.helper.Util.isEmptyString;
import static com.gluonhq.charm.glisten.layout.layer.PopupView.PopupSide.RIGHT;

public class ConfSelectorPresenter extends GluonPresenter<DevoxxApplication> {

    private static final StatusBar STATUS_BAR = MobileApplication.getInstance().getStatusBar();
    private static final PseudoClass PSEUDO_CLASS_STATUS_VOXXED = PseudoClass.getPseudoClass("voxxed");
    
    private final Map<String, Image> imagesMap = new HashMap<>();
    
    private final ResourceBundle bundle = ResourceBundle.getBundle("com/devoxx/views/confselector");

    @FXML
    private CharmListView<Conference, LocalDateTime> selector;

    @FXML
    private View confSelectorView;

    @FXML
    private StackPane filterPane;

    @FXML
    public Label header;

    @Inject
    private Service service;

    private FilteredList<Conference> filteredList;

    private final Comparator<Conference> futureConferenceComparator = (s1, s2) -> LocalDate.parse(s1.getFromDate()).compareTo(LocalDate.parse(s2.getFromDate()));
    private final Comparator<Conference> pastConferenceComparator = (s1, s2) -> LocalDate.parse(s2.getFromDate()).compareTo(LocalDate.parse(s1.getFromDate()));

    public void initialize() {
        filteredList = new FilteredList<>(service.retrieveConferences());
        filteredList.setPredicate(conference -> !isEmptyString(conference.getApiURL()));
        selector.setItems(filteredList);

        ProgressIndicator placeholder = new ProgressIndicator();
        placeholder.setRadius(20);
        selector.setPlaceholder(placeholder);
        selector.setCellFactory(p -> new ConferenceCell(service, imagesMap));
        selector.setComparator(futureConferenceComparator);

        confSelectorView.setOnShowing(event -> {
            getApp().getAppBar().setVisible(false);
            if (! confSelectorView.getStyleClass().contains("hidden-app-bar")) {
                confSelectorView.getStyleClass().add("hidden-app-bar");
            }
            hidePastConferenceMessage();
        });
        confSelectorView.setOnHiding(event -> imagesMap.clear());

        final Button filter = MaterialDesignIcon.FILTER_LIST.button();
        MenuPopupView popup = getMenuPopupView(filter);
        
        filter.setOnAction(e -> {
            Platform.runLater(() -> {
                if (popup.isShowing()) {
                    popup.hide();
                } else {
                    popup.show();
                }
            });
            e.consume();
        });
        filterPane.getChildren().add(filter);
    }

    private MenuPopupView getMenuPopupView(Button filter) {
        Menu menu = new Menu();
        MenuItem devoxx = new MenuItem(Conference.Type.DEVOXX.toString());
        MenuItem voxxed = new MenuItem(Conference.Type.VOXXED.toString());
        MenuItem futureEvents = new MenuItem(bundle.getString("OTN.CONFERENCE_SELECTOR.HEADER.DEVOXX_VOXXED"));
        MenuItem pastEvents = new MenuItem(bundle.getString("OTN.CONFERENCE_SELECTOR.HEADER.PAST_EVENTS"));
        menu.getItems().addAll(futureEvents, devoxx, voxxed, pastEvents);
        
        devoxx.setOnAction(e -> {
            updateFutureEvent(Conference.Type.DEVOXX);
            STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_VOXXED, false);
        });
        voxxed.setOnAction(e -> {
            updateFutureEvent(Conference.Type.VOXXED);
            STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_VOXXED, true);
        });
        futureEvents.setOnAction(e -> {
            updateFutureEvent(null);
            STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_VOXXED, false);
        });
        pastEvents.setOnAction(e -> {
            selector.setComparator(pastConferenceComparator);
            selector.setItems(service.retrievePastConferences());
            header.setText(bundle.getString("OTN.CONFERENCE_SELECTOR.HEADER.PAST_EVENTS"));
            STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_VOXXED, false);
        });

        final MenuPopupView menuPopupView = new MenuPopupView(filter, menu);
        menuPopupView.getStyleClass().add("conf-selector");
        menuPopupView.setSide(RIGHT);
        return menuPopupView;
    }

    private void updateFutureEvent(Conference.Type eventType) {
        selector.setItems(filteredList);
        selector.setComparator(futureConferenceComparator);
        if (eventType != null) {
            header.setText(eventType.getDisplayName());
            filteredList.setPredicate(conference -> !isEmptyString(conference.getApiURL()) && conference.getEventCategory() == eventType);
        } else {
            header.setText(bundle.getString("OTN.CONFERENCE_SELECTOR.HEADER.DEVOXX_VOXXED"));
            filteredList.setPredicate(conference -> !isEmptyString(conference.getApiURL()));
        }
    }
}
