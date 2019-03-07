/**
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
import com.devoxx.model.SponsorBadge;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.devoxx.model.Badge;
import com.devoxx.views.BadgePresenter;

public class BadgeCell<T extends Badge> extends CharmListCell<T> {
    private static final int MAX_TEXT_SIZE = 100;

    private final ListTile tile;

    public BadgeCell() {
        tile = new ListTile();
        tile.setPrimaryGraphic(MaterialDesignIcon.CONTACTS.graphic());
        tile.setSecondaryGraphic(MaterialDesignIcon.CHEVRON_RIGHT.graphic());
        setText(null);
        getStyleClass().add("badge-cell");
    }

    @Override public void updateItem(T badge, boolean empty) {
        super.updateItem(badge, empty);

        if (badge != null && !empty) {
            tile.textProperty().setAll(badge.getName(), 
                        badge.getEmail(),
                        truncateText(badge.getDetails()));
            setGraphic(tile);

            // FIX for OTN-568
            tile.setOnMouseReleased(event -> {
                DevoxxView.BADGE.switchView(ViewStackPolicy.USE).ifPresent(presenter -> {
                    if (badge instanceof SponsorBadge) {
                        ((BadgePresenter) presenter).setBadge(badge, BadgeType.SPONSOR);
                    } else {
                        ((BadgePresenter) presenter).setBadge(badge, BadgeType.ATTENDEE);
                    }
                });
            });
        } else {
            setGraphic(null);
        }
    }

    private String truncateText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String truncatedText = text;
        if (text.length() > MAX_TEXT_SIZE) {
            truncatedText = text.substring(0, MAX_TEXT_SIZE - 1) + "...";
        }
        return truncatedText;
    }
}
