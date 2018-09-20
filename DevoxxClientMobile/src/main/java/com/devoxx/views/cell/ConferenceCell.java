/**
 * Copyright (c) 2018, Gluon Software
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
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.ETagImageTask;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.CharmListCell;
import javafx.beans.binding.DoubleBinding;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConferenceCell extends CharmListCell<Conference> {

    private static final Logger LOG = Logger.getLogger(ConferenceCell.class.getName());

    private static final PseudoClass PSEUDO_CLASS_VOXXED = PseudoClass.getPseudoClass("voxxed");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName("Conference Image Loader");
        thread.setDaemon(true);
        return thread;
    });

    private final Service service;
    private final Label name;
    private final Label eventType;
    private final Label dateLabel;

    private final ImageView background;
    private final VBox top;
    private final BorderPane content;
    private final StackPane root;
    private final DoubleBinding widthProperty;

    public ConferenceCell(Service service) {
        this.service = service;

        widthProperty = MobileApplication.getInstance().getGlassPane().widthProperty().subtract(15);

        name = new Label();
        name.getStyleClass().add("name");
        
        eventType = new Label();
        eventType.getStyleClass().add("type");
        eventType.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(Conference.Type.VOXXED.name())) {
                pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, true);
            } else {
                pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, false);
            }
        });
        
        dateLabel = new Label();
        dateLabel.getStyleClass().add("date");
        
        background = new ImageView();
        background.setFitHeight(222);
        
        top = new VBox(eventType, name);
        top.getStyleClass().add("top");

        content = new BorderPane();
        content.getStyleClass().add("content");
        content.setTop(top);
        content.setBottom(dateLabel);
        
        root = new StackPane(background, content);
        getStyleClass().add("conference-cell"); 
    }

    private ETagImageTask imageTask;

    @Override
    public void updateItem(Conference item, boolean empty) {
        super.updateItem(item, empty);
        
        background.setImage(null);
        if (item != null && !empty) {
            eventType.setText(item.getEventType().name());
            
            name.setText(item.getName());
            if (item.getFromDate().equals(item.getEndDate())) {
                dateLabel.setText(LocalDate.parse(item.getFromDate()).format(DATE_TIME_FORMATTER));
            } else {
                dateLabel.setText(LocalDate.parse(item.getFromDate()).getDayOfMonth() + " - " +
                        LocalDate.parse(item.getEndDate()).format(DATE_TIME_FORMATTER));
            }

            if (imageTask != null) {
                imageTask.cancel();
            }

            imageTask = new ETagImageTask("conference_" + item.getId(), item.getImageURL());
            imageTask.setOnSucceeded(e -> {
                background.setImage(imageTask.getValue());
            });
            imageTask.exceptionProperty().addListener((o, ov, nv) -> {
                LOG.log(Level.SEVERE, nv.getMessage());
            });
            executor.submit(imageTask);
            imageTask.image().ifPresent(background::setImage);

            background.fitWidthProperty().bind(widthProperty.subtract(2));
            
            content.setOnMouseReleased(e -> {
                if (!item.equals(service.getConference())) {
                    service.retrieveConference(item.getId());
                    Services.get(SettingsService.class).ifPresent(settingsService -> {
                        settingsService.store(DevoxxSettings.SAVED_CONFERENCE_TYPE, item.getEventType().name());
                        settingsService.store(DevoxxSettings.SAVED_CONFERENCE_ID, String.valueOf(item.getId()));
                    });
                }
                DevoxxView.SESSIONS.switchView();
            });
            setGraphic(root);
        } else {
            setGraphic(null);
        }
    }
}
