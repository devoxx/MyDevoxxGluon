/**
 * Copyright (c) 2016, Gluon Software
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
import com.devoxx.model.Speaker;
import com.devoxx.views.SpeakerPresenter;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeakerCell extends CharmListCell<Speaker> {

    private final ListTile tile;

    public SpeakerCell() {
        tile = new ListTile();
        // Fix for OTN-393: This fails on iOS because the MISSING_RESOURCE_ERROR issue
        // when there are non Ascii characters and trying to break the line throws
        // a RuntimeException
//        tile.setWrapText(true);
        tile.setSecondaryGraphic(MaterialDesignIcon.CHEVRON_RIGHT.graphic());
        setText(null);
        getStyleClass().add("speaker-cell");
    }

    @Override
    public void updateItem(Speaker speaker, boolean empty) {
        super.updateItem(speaker, empty);
        if (speaker != null && !empty) {
            final Avatar avatar = Util.getSpeakerAvatar(speaker, "list-avatar");
            avatar.setMouseTransparent(true);
            tile.setPrimaryGraphic(avatar);

            //To keep height of the tile same for all cases we add "one space text instead of nulls
            List<String> lines = new ArrayList<>(Arrays.asList(
                    speaker.getFullName() == null ? " " : speaker.getFullName(),
        //                speaker.getJobTitle() == null? " ": speaker.getJobTitle(),
                    speaker.getCompany() == null ? " " : speaker.getCompany()));
            lines.removeAll(Arrays.asList("", null));
            for (int i = 0; i < 3 - lines.size(); i++) {
                lines.add("");
            }

            tile.textProperty().setAll(lines);

            // FIX for OTN-568
            tile.setOnMouseReleased(event -> {
                DevoxxView.SPEAKER.switchView().ifPresent(presenter ->
                        ((SpeakerPresenter) presenter).setSpeaker(speaker));
            });
            setGraphic(tile);
        } else {
            setGraphic(null);
        }
    }
}
