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

import com.devoxx.model.TeamMember;
import com.devoxx.util.ImageCache;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.CharmListCell;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class TeamCell extends CharmListCell<TeamMember> {

    private final BorderPane root;
    private final BorderPane content;
    private final ImageView background;
    private final Text textName;
    private final Text textPosition;

    private static final int PHONE_HEIGHT = 222;
    private static final int TABLET_HEIGHT = 333;
    private final double maxH, padding;
    private static final Image DEFAULT_BACKGROUND_IMAGE = new Image(Util.class.getResource("backgroundImage.png").toString());

    public TeamCell() {

        StackPane panePicture = new StackPane();
        {
            background = new ImageView();
            background.setPreserveRatio(true);

            VBox paneBottomTeamMemberInformation = new VBox();
            {
                textName = new Text();
                {
                    textName.setStyle("-fx-font: 24px Tahoma; " +
                            "-fx-font-weight: bold;"+
                            //"-fx-fill: linear-gradient(white, gray);"+
                            "-fx-fill: white;"+
                            "-fx-stroke: #10416c;" +
                            "-fx-stroke-width: 0.8;");
                }
                textPosition = new Text();
                {
                    textPosition.setStyle("-fx-font: 18px Tahoma; " +
                            "-fx-font-weight: bold;"+
                            "-fx-fill: white;"+
                            "-fx-stroke: #10416c;" +
                            "-fx-stroke-width: 0.8;");
                }
                paneBottomTeamMemberInformation.getChildren().add(textName);
                paneBottomTeamMemberInformation.getChildren().add(textPosition);
                paneBottomTeamMemberInformation.setAlignment(Pos.BOTTOM_LEFT);
                panePicture.setAlignment(Pos.BOTTOM_LEFT);
            }
            panePicture.getChildren().add(background);
            panePicture.getChildren().add(paneBottomTeamMemberInformation);
        }



        content = new BorderPane(panePicture);
        root = new BorderPane(content);
        getStyleClass().add("sponsors-logo-cell");

        MobileApplication.getInstance().getGlassPane().widthProperty().addListener((obs, ov, nv) -> fitImage());

        final boolean isTablet = Services.get(DisplayService.class)
                .map(DisplayService::isTablet)
                .orElse(false);
        maxH = isTablet ? TABLET_HEIGHT : PHONE_HEIGHT;
        padding = isTablet ? 50 : 40;
    }

    @Override
    public void updateItem(TeamMember teamMember, boolean empty) {
        super.updateItem(teamMember, empty);

        if (teamMember != null && !empty) {
            if (teamMember.getImage() != null) {
                Image image = ImageCache.get(teamMember.getImage().getSrc(), () -> DEFAULT_BACKGROUND_IMAGE,
                        downloadedImage -> background.setImage(downloadedImage));                
                background.setImage(image);
                background.setCache(true);
                background.setUserData(teamMember);
                fitImage();
                setTeamMember(teamMember);
            }
            //background.setOnMouseReleased(e -> Util.launchExternalBrowser(() -> teamMember.getHref()));
        }
        setGraphic(root);
    }

    private void setTeamMember(TeamMember teamMember) {
        String output="";
        output += teamMember.getName() + " ";
        output += teamMember.getBio() + " ";
        output += teamMember.getLinkedin() + " ";
        output += teamMember.getTwitter() + " ";
        output += teamMember.getPosition() + " ";
        output += teamMember.getRole() + " ";
        System.out.println(output);

        textName.setText(teamMember.getName());
        textPosition.setText(teamMember.getPosition());
    }

    private void fitImage() {
        Image image = background.getImage();
        if (image != null) {
            double factor = image.getHeight() / image.getWidth();
            final double maxW = MobileApplication.getInstance().getGlassPane().getWidth() - padding;
            double fitHeight = maxH;
            if (factor < fitHeight / maxW) {
                background.setFitWidth(10000);
                background.setFitHeight(fitHeight);
            } else {
                background.setFitWidth(maxW);
                background.setFitHeight(10000);
            }
            background.setFitWidth(maxW);
            background.setFitHeight(fitHeight);
        }
    }
}
