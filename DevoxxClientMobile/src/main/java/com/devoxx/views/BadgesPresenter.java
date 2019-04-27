/*
 * Copyright (c) 2017, 2018 Gluon Software
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
import com.devoxx.model.Sponsor;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.connect.GluonObservableObject;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Optional;

public class BadgesPresenter extends GluonPresenter<DevoxxApplication> {

    @FXML
    private View badgesView;

    @FXML
    private VBox content;

    @FXML
    private TextField code;
    @FXML
    private Button btnAccess;

    @Inject
    private Service service;

    public void initialize() {

        code.setMaxWidth(140);
        code.textProperty().addListener((final ObservableValue<? extends String> ov, final String oldValue, final String newValue) -> {
                    if (code.getText().length() > 4) {
                        String s = code.getText().substring(0, 4);
                        code.setText(s);
                    }
                }
        );

        btnAccess.setOnAction(e -> showSponsor());

        badgesView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.BADGES.getTitle());

            loadContent();
        });
    }

    private void loadContent() {
        Optional<Sponsor> savedSponsor = fetchSavedSponsor();
        if (savedSponsor.isPresent()) {
            DevoxxView.SPONSOR_BADGE.switchView().ifPresent(presenter -> ((SponsorBadgePresenter) presenter).setSponsor(savedSponsor.get()));
        } else {
            badgesView.setCenter(content);
        }
    }

    private void showSponsor() {
        GluonObservableObject<Sponsor> sponsor = service.retrieveSponsorByCode(code.getText());
        sponsor.setOnSucceeded(event -> {
            final Optional<SettingsService> settingsService = Services.get(SettingsService.class);
            if (settingsService.isPresent()) {
                SettingsService service = settingsService.get();
                service.store(DevoxxSettings.BADGE_SPONSOR, sponsor.get().toCSV());
            }
            DevoxxView.SPONSOR_BADGE.switchView();
        });
    }

    private Optional<Sponsor> fetchSavedSponsor() {
        final Optional<SettingsService> settingsService = Services.get(SettingsService.class);
        if (settingsService.isPresent()) {
            SettingsService service = settingsService.get();
            return Optional.ofNullable(Sponsor.fromCSV(service.retrieve(DevoxxSettings.BADGE_SPONSOR)));
        }
        return Optional.empty();
    }
}
