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
package com.devoxx.views.helper;

import com.devoxx.model.Speaker;
import com.devoxx.util.DevoxxBundle;
import static com.devoxx.util.DevoxxSettings.TWITTER_URL;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.Toast;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URISyntaxException;

public class SpeakerCard extends Region {

    private static final double PADDING = 2;
    
    private final Avatar avatar;
    private final ImageView imageView;
    private final Region imageSpacer;
    private final Label speakerName;
    private final Label speakerCompany;
    private final Label speakerTitle;
    private final Label speakerBlog;

    public SpeakerCard(Speaker speaker) {

        getStyleClass().add("speaker-card");

        imageView = Util.getMediaBackgroundImageView();
        imageView.setPreserveRatio(true);
        imageSpacer = new Region();

        speakerName = new Label(speaker.getFullName());
        speakerName.getStyleClass().add("speaker-name");

        speakerCompany = new Label(speaker.getCompany());
        speakerCompany.getStyleClass().add("speaker-company");

        speakerTitle = new Label(speaker.getTwitter());
        speakerTitle.getStyleClass().addAll("speaker-title", "link");
        speakerTitle.setOnMousePressed(e -> Services.get(BrowserService.class).ifPresent(b -> {
            if(speaker.getTwitter() != null && !speaker.getTwitter().isEmpty() && speaker.getTwitter().startsWith("@")) {
                try {
                    String url = TWITTER_URL + speaker.getTwitter().substring(1);
                    b.launchExternalBrowser(url);
                } catch (IOException | URISyntaxException ex) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                    toast.show();
                }
            }
        }));
        
        speakerBlog = new Label(speaker.getBlog());
        speakerBlog.getStyleClass().addAll("speaker-title","link");
        speakerBlog.setOnMousePressed(e -> Services.get(BrowserService.class).ifPresent(b -> {
            if(speaker.getBlog() != null && !speaker.getBlog().isEmpty() && speaker.getBlog().startsWith("http")) {
                try {
                    String url = speaker.getBlog();
                    b.launchExternalBrowser(url);
                } catch (IOException | URISyntaxException ex) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                    toast.show();
                }
            }
        }));

        avatar = Util.getSpeakerAvatar(speaker);

        getChildren().addAll(imageView, imageSpacer, avatar, speakerName, speakerCompany, speakerTitle, speakerBlog);
        Platform.runLater(this::requestLayout);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected void layoutChildren() {
        final double w = getWidth() - (snappedLeftInset() + snappedRightInset());
        final double h = getHeight() - (snappedTopInset() + snappedBottomInset());

        // size and position the background imageView
        imageView.setFitWidth(w);
        imageView.relocate(0, 0);

        // If max height is not set, consider the height of ImageView
        resizeImageViewAndImageSpacer(w);
        //place imageSpacer on top of ImageView
        imageSpacer.resizeRelocate(0, 0, imageSpacer.getPrefWidth(), imageSpacer.getMinHeight());
        final double imageSpacerHeight = imageSpacer.getLayoutBounds().getHeight();

        // place avatar half way off the bottom of the imageView, and
        // centered horizontally
        final double avatarWidth = snapSize(avatar.prefWidth(-1));
        final double avatarHeight = snapSize(avatar.prefHeight(avatarWidth));
        final double avatarStartY = snapPosition(imageSpacerHeight - avatarHeight / 2.0);
        final double avatarEndY = snapPosition(avatarStartY + avatarHeight);
        avatar.relocate(w / 2.0 - avatarWidth / 2.0, avatarStartY);

        // place speaker name beneath avatar
        final double speakerNameHeight = speakerName.prefHeight(w);
        final double speakerNameEndY = avatarEndY + PADDING + speakerNameHeight;
        speakerName.resizeRelocate(0, imageSpacerHeight/2 - speakerNameHeight/2,//avatarStartY - avatarHeight/2,
                w, speakerNameHeight);

        double y = avatarEndY + PADDING;

        if (speakerCompany.getText() != null && !speakerCompany.getText().isEmpty()) {
            speakerCompany.resizeRelocate(0, y, w, speakerCompany.prefHeight(w));
            y += speakerCompany.prefHeight(w) + PADDING;
        }

        if (speakerTitle.getText() != null && !speakerTitle.getText().isEmpty()) {
            speakerTitle.resizeRelocate(0, y, w, speakerTitle.prefHeight(w));
            y += speakerTitle.prefHeight(w) + PADDING;
        }
        
        if (speakerBlog.getText() != null && !speakerBlog.getText().isEmpty()) {
        	speakerBlog.resizeRelocate(0, y, w, speakerBlog.prefHeight(w));
            y += speakerBlog.prefHeight(w) + PADDING;
        }

    }

    @Override
    protected double computePrefHeight(double width) {
        resizeImageViewAndImageSpacer(width);
        if(getMaxHeight() < 0) {
            return imageView.getBoundsInLocal().getHeight() + computeHeightExceptImageSpacer(width);
        }
        return imageSpacer.getPrefHeight() + computeHeightExceptImageSpacer(width);
    }

    private double computeHeightExceptImageSpacer(double width) {
        final boolean showSpeakerCompany = speakerCompany.getText() != null && !speakerCompany.getText().isEmpty();
        final boolean showSpeakerTitle = speakerTitle.getText() != null && !speakerTitle.getText().isEmpty();
        final boolean showSpeakerBlog = speakerBlog.getText() != null && !speakerBlog.getText().isEmpty();
        return avatar.prefHeight(-1) / 2.0
                + (showSpeakerCompany ? speakerCompany.prefHeight(width) : 0)
                + (showSpeakerTitle ? speakerTitle.prefHeight(width) : 0)
                + (showSpeakerBlog ? speakerBlog.prefHeight(width) : 0)
                + 20;
    }

    private void resizeImageViewAndImageSpacer(double w) {
        final double maxImageHeight = getMaxHeight() < 0 ? imageView.getBoundsInLocal().getHeight() : getMaxHeight() - computeHeightExceptImageSpacer(w);
        Util.resizeImageViewAndImageSpacer(imageSpacer, imageView, w, maxImageHeight);
    }
}
