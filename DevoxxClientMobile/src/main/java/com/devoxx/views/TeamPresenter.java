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
import com.devoxx.DevoxxView;
import com.devoxx.model.TeamMember;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.cell.TeamCell;
import com.devoxx.views.helper.Placeholder;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.connect.GluonObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javax.inject.Inject;

public class TeamPresenter extends GluonPresenter<DevoxxApplication> {

    private static final String PLACEHOLDER_TITLE = DevoxxBundle.getString("OTN.PLACEHOLDER.TITLE");
    private static final String PLACEHOLDER_MESSAGE = DevoxxBundle.getString("OTN.PLACEHOLDER.MESSAGE");
    private boolean loadDataFromService = true;


    @FXML
    private View team;

    @FXML
    private CharmListView<TeamMember, Integer> teamListView;

    @Inject
    private Service service;

    public void initialize() {
        team.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.TEAM.getTitle());
            teamListView.setSelectedItem(null);
            if (loadDataFromService) {
                retrieveTeamList();
                loadDataFromService = false;
            }
        });

        teamListView.getStyleClass().add("sponsor-logo-list-view");
        teamListView.setPlaceholder(new Placeholder(PLACEHOLDER_TITLE, PLACEHOLDER_MESSAGE, DevoxxView.SPONSORS_LOGO.getMenuIcon()));
        //teamListView.setHeadersFunction(s -> s.getLevel().getPriority());
        //teamListView.setHeaderCellFactory(p -> new SponsorLogoHeaderCell());
        teamListView.setCellFactory(p -> new TeamCell());
    }

    private void retrieveTeamList() {
        final AppBar appBar = getApp().getAppBar();
        appBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        appBar.setProgressBarVisible(true);

        teamListView.setPlaceholder(new Placeholder(PLACEHOLDER_TITLE, PLACEHOLDER_MESSAGE, DevoxxView.SPONSORS_LOGO.getMenuIcon()));

        GluonObservableList<TeamMember> teamMemberList = service.retrieveTeam();
        teamMemberList.setOnSucceeded(e -> {
            appBar.setProgressBarVisible(false);
            teamListView.setItems(teamMemberList);
            if (teamMemberList.isEmpty()) {
                teamListView.setPlaceholder(Placeholder.empty("Team", service.getConference().getName()));
            }
        });
        teamMemberList.setOnFailed(e -> {
            appBar.setProgressBarVisible(false);
            final Button retry = new Button("Retry");
            retry.setOnAction(ae -> retrieveTeamList());
            teamListView.setPlaceholder(Placeholder.failure("Team", retry));
        });
    }
}
