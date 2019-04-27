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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Floor;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.cell.FloorCell;
import com.devoxx.views.helper.Placeholder;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;

import javax.inject.Inject;
import java.util.Comparator;

public class ExhibitionMapsPresenter extends GluonPresenter<DevoxxApplication> {
    private static final String PLACEHOLDER_MESSAGE = DevoxxBundle.getString("OTN.EXHIBITIONMAPS.PLACEHOLDER_MESSAGE");

    @FXML
    private View exhibitionMaps;
    @FXML
    private CharmListView<Floor, String> exhibitionMapsListView;

    private FilteredList<Floor> filteredFloors;

    @Inject
    private Service service;

    public void initialize() {
        exhibitionMaps.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.EXHIBITION_MAPS.getTitle());
            exhibitionMapsListView.setSelectedItem(null);
            exhibitionMapsListView.setComparator((f1, f2) -> Integer.compare(Integer.parseInt(f1.getId()), Integer.parseInt(f2.getId())));

            filteredFloors = new FilteredList<>(service.retrieveExhibitionMaps());
            exhibitionMapsListView.setItems(filteredFloors);
        });

        exhibitionMapsListView.getStyleClass().add("exhibitionmaps-list-view");

        exhibitionMapsListView.setPlaceholder(new Placeholder(PLACEHOLDER_MESSAGE, DevoxxView.EXHIBITION_MAPS.getMenuIcon()));

        exhibitionMapsListView.setCellFactory(p -> new FloorCell());
    }
}