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
import com.devoxx.model.BadgeType;
import com.devoxx.model.Sponsor;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.connect.GluonObservableObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Optional;

import static com.devoxx.model.BadgeType.valueOf;

public class BadgesPresenter extends GluonPresenter<DevoxxApplication> {
    
    @FXML
    private View badgesView;

    @FXML
    private VBox content;

    @FXML
    private TextField code;
    @FXML
    private Button sponsor;

//    @FXML
//    private Button attendee;

    @Inject
    private Service service;
    
    public void initialize() {
        
        badgesView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.BADGES.getTitle());
            
            sponsor.setOnAction(e -> showSponsor());
//            attendee.setOnAction(e -> showAttendee());

            loadContent();
        });
    }

    private void loadContent() {
        final BadgeType badgeType = loadPreviousSelection();
        if (badgeType == null) {
            badgesView.setCenter(content);
        } else {
            switch (badgeType) {
                case SPONSOR:
                    // BadgeType is Sponsor but sponsor details are incomplete
                    // we should show sponsor/attendee selection
                    Optional<Sponsor> savedSponsor = fetchSavedSponsor();
                    if (savedSponsor.isPresent()) {
                        showSponsor();
                    } else {
                        badgesView.setCenter(content);
                    }
                    break;
                case ATTENDEE:
                    showAttendee();
                    break;
            }
        }
    }

    private BadgeType loadPreviousSelection() {
        final Optional<SettingsService> settingsService = Services.get(SettingsService.class);
        if (settingsService.isPresent()) {
            final SettingsService service = settingsService.get();
            final String badgeType = service.retrieve(DevoxxSettings.BADGE_TYPE);
            if (badgeType != null) {
                return valueOf(badgeType);
            }
        }
        return null;
    }

    private void showAttendee() {
        if (service.isAuthenticated()) {
            DevoxxView.ATTENDEE_BADGE.switchView();
        } else {
            DevoxxView.ATTENDEE_BADGE.switchView(ViewStackPolicy.USE).ifPresent(presenter -> ((AttendeeBadgePresenter)presenter).addToStack());
        }
    }

    private void showSponsor() {
        Optional<Sponsor> savedSponsor = fetchSavedSponsor();
        if (savedSponsor.isPresent()) {
            DevoxxView.SPONSOR_BADGE.switchView().ifPresent(presenter -> ((SponsorBadgePresenter) presenter).setSponsor(savedSponsor.get()));
        } else {
            GluonObservableObject<Sponsor> sponsor = service.retrieveSponsorByCode(code.getText());
            if (sponsor.isNotNull().getValue()) {
                DevoxxView.SPONSOR_BADGE.switchView().ifPresent(presenter -> ((SponsorBadgePresenter) presenter).setSponsor(sponsor.get()));
            }
        }
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
