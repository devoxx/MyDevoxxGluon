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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

public class TeamCell extends CharmListCell<TeamMember> {

    private final BorderPane root;
    private final BorderPane content;
    private final ImageView background;
    private final Text textName;
    private final Text textPosition;
    private final Button buttonLinkedIn;
    private final Button buttonTwitter;

    private static final int PHONE_HEIGHT = 222;
    private static final int TABLET_HEIGHT = 333;
    private final double maxH, padding;
    private static final Image DEFAULT_BACKGROUND_IMAGE = new Image(Util.class.getResource("backgroundImage.png").toString());
    private final HBox hboxButtons;

    public TeamCell() {

        StackPane panePicture = new StackPane();
        {
            background = new ImageView();
            background.setPreserveRatio(true);

            VBox paneVBoxInformation = new VBox();
            {
                textName = new Text();
                {
                    textName.setStyle("-fx-font: 24px Tahoma; " +
                            "-fx-font-weight: bold;" +
                            "-fx-fill: white;" +
                            "-fx-stroke: #10416c;" +
                            "-fx-stroke-width: 0.8;");
                }
                textPosition = new Text();
                {
                    textPosition.setStyle("-fx-font: 18px Tahoma; " +
                            "-fx-font-weight: bold;" +
                            "-fx-fill: white;" +
                            "-fx-stroke: #10416c;" +
                            "-fx-stroke-width: 0.8;");
                }
                paneVBoxInformation.getChildren().add(textName);
                paneVBoxInformation.getChildren().add(textPosition);
                paneVBoxInformation.setAlignment(Pos.BOTTOM_LEFT);
                panePicture.setAlignment(Pos.BOTTOM_LEFT);
            }
            panePicture.getChildren().add(background);
            panePicture.getChildren().add(paneVBoxInformation);
        }

        hboxButtons = new HBox();
        {
            buttonLinkedIn = new Button();
            {
                SVGPath linkedinSVGPath = new SVGPath();
                linkedinSVGPath.setContent("M21,21H17V14.25C17,13.19 15.81,12.31 14.75,12.31C13.69,12.31 13,13.19 13,14.25V21H9V9H13V11C13.66,9.93 15.36,9.24 16.5,9.24C19,9.24 21,11.28 21,13.75V21M7,21H3V9H7V21M5,3A2,2 0 0,1 7,5A2,2 0 0,1 5,7A2,2 0 0,1 3,5A2,2 0 0,1 5,3Z");
                linkedinSVGPath.setStyle("-fx-fill: white;");
                buttonLinkedIn.setGraphic(linkedinSVGPath);
                buttonLinkedIn.getStyleClass().add("icon-toggle");
                buttonLinkedIn.setStyle("-fx-background-color: #10416c;");
            }
            buttonTwitter = new Button();
            {
                SVGPath linkedinSVGPath = new SVGPath();
                linkedinSVGPath.setContent("M22.46,6C21.69,6.35 20.86,6.58 20,6.69C20.88,6.16 21.56,5.32 21.88,4.31C21.05,4.81 20.13,5.16 19.16,5.36C18.37,4.5 17.26,4 16,4C13.65,4 11.73,5.92 11.73,8.29C11.73,8.63 11.77,8.96 11.84,9.27C8.28,9.09 5.11,7.38 3,4.79C2.63,5.42 2.42,6.16 2.42,6.94C2.42,8.43 3.17,9.75 4.33,10.5C3.62,10.5 2.96,10.3 2.38,10C2.38,10 2.38,10 2.38,10.03C2.38,12.11 3.86,13.85 5.82,14.24C5.46,14.34 5.08,14.39 4.69,14.39C4.42,14.39 4.15,14.36 3.89,14.31C4.43,16 6,17.26 7.89,17.29C6.43,18.45 4.58,19.13 2.56,19.13C2.22,19.13 1.88,19.11 1.54,19.07C3.44,20.29 5.7,21 8.12,21C16,21 20.33,14.46 20.33,8.79C20.33,8.6 20.33,8.42 20.32,8.23C21.16,7.63 21.88,6.87 22.46,6Z");
                linkedinSVGPath.setStyle("-fx-fill: white;");
                buttonTwitter.setGraphic(linkedinSVGPath);
                buttonTwitter.getStyleClass().add("icon-toggle");
                buttonTwitter.setStyle("-fx-background-color: #10416c;");
            }
            hboxButtons.getChildren().add(buttonLinkedIn);
            hboxButtons.getChildren().add(buttonTwitter);
            hboxButtons.setPadding(new Insets(5, 10, 0, 10));
            hboxButtons.setSpacing(5);
            hboxButtons.setAlignment(Pos.CENTER_RIGHT);
            hboxButtons.setMaxWidth(300);
        }

        content = new BorderPane();
        {
            content.setCenter(panePicture);
            content.setBottom(hboxButtons);
        }
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
        }
        setGraphic(root);
    }

    private void setTeamMember(TeamMember teamMember) {
        textName.setText(teamMember.getName());
        textPosition.setText(teamMember.getPosition());
        buttonLinkedIn.setOnMouseClicked(e -> Util.launchExternalBrowser(() -> teamMember.getLinkedin()));
        buttonTwitter.setOnMouseClicked(e -> Util.launchExternalBrowser(() -> teamMember.getTwitter()));
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
