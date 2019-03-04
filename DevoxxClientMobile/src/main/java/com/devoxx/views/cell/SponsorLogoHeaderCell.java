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

import com.devoxx.model.Sponsor;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.ListTile;
import javafx.css.PseudoClass;

import static com.devoxx.views.helper.SponsorPriority.fetchPseudoClassForPriority;

public class SponsorLogoHeaderCell extends CharmListCell<Sponsor> {

    private PseudoClass oldPseudoClass;
    private final ListTile tile;

    public SponsorLogoHeaderCell() {
        this.tile = new ListTile();
        setText(null);
        getStyleClass().add("sponsor-logo-header-cell");
    }

    @Override
    public void updateItem(Sponsor item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null && !empty) {
            System.out.println();
            changePseudoClass(fetchPseudoClassForPriority(item.getLevel().getPriority()));
            tile.textProperty().setAll(item.getLevel().getName().toUpperCase());
            setGraphic(tile);
        } else {
            setGraphic(null);
        }
    }

    private void changePseudoClass(PseudoClass pseudoClass) {
        if (oldPseudoClass!=null)pseudoClassStateChanged(oldPseudoClass, false);
        pseudoClassStateChanged(pseudoClass, true);
        oldPseudoClass = pseudoClass;
    }
}
