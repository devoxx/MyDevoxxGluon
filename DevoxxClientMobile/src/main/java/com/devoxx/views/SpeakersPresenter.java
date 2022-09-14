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
import com.devoxx.service.Service;
import com.devoxx.model.Speaker;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.cell.SpeakerHeaderCell;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SpeakerComparator;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.views.cell.SpeakerCell;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.StringConverter;

import javax.inject.Inject;


public class SpeakersPresenter extends GluonPresenter<DevoxxApplication> {
    private static final String PLACEHOLDER_MESSAGE = DevoxxBundle.getString("OTN.SPEAKERS.PLACEHOLDER_MESSAGE");
    private static final String RETRY_MESSAGE = DevoxxBundle.getString("OTN.SPEAKERS.PLACEHOLDER_RETRY_MESSAGE");

    @FXML
    private View speakers;
    @FXML
    private CharmListView<Speaker, String> speakersListView;
    
    @Inject
    private Service service;

    public void initialize() {
        speakers.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.SPEAKERS.getTitle());
            appBar.getActionItems().addAll(getApp().getSearchButton());
            speakersListView.setSelectedItem(null);

            speakersListView.setItems(service.retrieveSpeakers());
        });

        speakersListView.getStyleClass().add("speakers-list-view");

        Placeholder placeholder = new Placeholder(PLACEHOLDER_MESSAGE, DevoxxView.SPEAKERS.getMenuIcon());
        final Button retry = new Button(RETRY_MESSAGE, MaterialDesignIcon.REFRESH.graphic());
        retry.setOnAction(e -> service.checkIfReloadRequested());
        placeholder.getChildren().add(retry);
        speakersListView.setPlaceholder(placeholder);

        speakersListView.setComparator(new SpeakerComparator());
           
        speakersListView.setHeadersFunction(speaker -> (speaker.getFirstName() != null ? speaker.getFirstName().substring(0, 1).toLowerCase() : ""));
        
        speakersListView.setConverter(new StringConverter<String>() {

            @Override
            public String toString(String object) {
                // For now, just get upper case of name's first letter
                return object.toUpperCase();
            }

            @Override
            public String fromString(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        
        speakersListView.setCellFactory(p -> new SpeakerCell());
        speakersListView.setHeaderCellFactory(p -> new SpeakerHeaderCell(speakersListView));
//        speakersListView.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                DevoxxView.SPEAKER.switchView().ifPresent( presenter ->
//                        ((SpeakerPresenter)presenter).setSpeaker(newValue));
//            }
//        });
    }
}
