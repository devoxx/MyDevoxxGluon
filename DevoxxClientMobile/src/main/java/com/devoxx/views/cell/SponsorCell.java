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
package com.devoxx.views.cell;

import com.devoxx.DevoxxView;
import com.devoxx.model.BadgeType;
import com.devoxx.model.Sponsor;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.SponsorBadgePresenter;
import com.devoxx.views.helper.Util;
import com.gluonhq.attach.settings.SettingsService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.application.Platform;
import javafx.util.Duration;

public class SponsorCell extends CharmListCell<Sponsor> {

    private final ListTile tile;

    public SponsorCell() {
        tile = new ListTile();
        tile.setSecondaryGraphic(MaterialDesignIcon.CHEVRON_RIGHT.graphic());

        setText(null);
        getStyleClass().add("sponsor-cell");
    }

    @Override
    public void updateItem(Sponsor sponsor, boolean empty) {
        super.updateItem(sponsor, empty);
        if (sponsor != null && !empty) {
            tile.textProperty().setAll(sponsor.getName());
            setGraphic(tile);

            // FIX for OTN-568
            tile.setOnMouseReleased(event -> {
                SettingsService.create().ifPresent(service -> {
                    service.store(DevoxxSettings.BADGE_TYPE, BadgeType.SPONSOR.toString());
                    service.store(DevoxxSettings.BADGE_SPONSOR, sponsor.toCSV());
                });
                // Remove BadgePresenter from View Stack
                MobileApplication.getInstance().switchToPreviousView();
                DevoxxView.SPONSOR_BADGE.switchView().ifPresent(presenter ->
                        ((SponsorBadgePresenter)presenter).setSponsor(sponsor));
                Platform.runLater(() -> Util.showToast(DevoxxBundle.getString("OTN.BADGES.LOGIN.SPONSOR", sponsor.getName()), Duration.seconds(5)));
            });
        } else {
            setGraphic(null);
        }
    }
}
