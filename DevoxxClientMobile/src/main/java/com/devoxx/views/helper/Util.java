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
package com.devoxx.views.helper;

import com.devoxx.model.Speaker;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.ImageCache;
import com.devoxx.views.ExhibitionMapPresenter;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.layout.Layer;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.media.MediaClient;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class Util {

    // speaker avatars
    private static final Image DEFAULT_IMAGE = new Image(SpeakerCard.class.getResource("speaker.jpeg").toString());
    
    // drawer's header default image
    private static final Image DEFAULT_BACKGROUND_IMAGE = new Image(Util.class.getResource("backgroundImage.png").toString());

    // TODO: Provide default image
    public static final Image DEFAULT_EXHIBITION_MAP = new Image(ExhibitionMapPresenter.class.getResource("circle.png").toString());

    private static MediaClient mediaClient = new MediaClient();
    private static Layer pastConferenceMessage;

    private Util() {}

    public static ImageView getMediaBackgroundImageView() {
        final ImageView imageView = new ImageView(getMediaBackgroundImage());
        imageView.setPreserveRatio(true);
        return imageView;
    }

    public static Image getMediaBackgroundImage() {
        Image loadImage = null;
        try {
            loadImage = mediaClient.loadImage("backgroundImage");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (loadImage == null || loadImage.getHeight() == 0) {
            return DEFAULT_BACKGROUND_IMAGE;
        }
        return loadImage;
    }

    public static Avatar getSpeakerAvatar(Speaker speaker, String... newStyleClasses) {
        Avatar avatar = new Avatar();
        avatar.getStyleClass().addAll(newStyleClasses);

        Image image = ImageCache.get(speaker.getAvatarURL(), () -> DEFAULT_IMAGE, downloadedImage -> avatar.setImage(downloadedImage));
        avatar.setImage(image);

        avatar.setCache(true);

        return avatar;
    }

    public static FloatingActionButton createFAB(MaterialDesignIcon icon, EventHandler<ActionEvent> onAction ) {
        FloatingActionButton floatingActionButton = new FloatingActionButton(icon.text, onAction);
        floatingActionButton.setFloatingActionButtonHandler(FloatingActionButton.BOTTOM_RIGHT);
        return floatingActionButton;
    }

    public static FloatingActionButton createWebLaunchFAB(Supplier<String> urlSupplier) {
        return createFAB(MaterialDesignIcon.LAUNCH, e -> {
            Services.get(BrowserService.class).ifPresent(b -> {
                try {
                    String url = urlSupplier.get();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://".concat(url);
                    }
                    b.launchExternalBrowser(url);
                } catch (IOException | URISyntaxException ex) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                    toast.show();
                }
            });
        });
    }
    
    public static void requestRating() {
        String url = null;
        if(Platform.isAndroid()) {
            url = DevoxxSettings.ANDROID_REVIEW_URL;
        } else if (Platform.isIOS()) {
            url = DevoxxSettings.IOS_REVIEW_URL;
        }
        String finalUrl = url;
        Services.get(BrowserService.class).ifPresent(b -> {
            try {
                if (finalUrl != null) {
                    b.launchExternalBrowser(finalUrl);
                }
            } catch (IOException | URISyntaxException e1) {
                Toast toast = new Toast("Failed to launch Store");
                toast.show();
            }
        });
    }

    public static void resizeImageView(ImageView imageView, double availableWidth, double maxHeight) {
        Image image = imageView.getImage();

        if(image == null || image.getWidth() == 0.0 || image.getHeight() == 0.0) {
            imageView.setFitWidth(0.0);
            imageView.setFitHeight(0.0);
            return;
        }

        double aspectRatio = image.getWidth() / image.getHeight();
        double estimatedHeight = availableWidth / aspectRatio;
        if(estimatedHeight <= maxHeight) {
            imageView.setFitWidth(availableWidth);
            imageView.setFitHeight(estimatedHeight);
        } else {
            imageView.setFitWidth(maxHeight * aspectRatio);
            imageView.setFitHeight(maxHeight);
        }
    }

    public static void resizeImageViewAndImageSpacer(Region imageSpacer, ImageView imageView, double maxWidth, double maxHeight) {
        Image image = imageView.getImage();
        if(image == null || image.getHeight() == 0.0) return;

        // Both ImageView and ImageSpacer should take up available width
        imageView.setFitWidth(maxWidth);
        imageSpacer.setPrefWidth(maxWidth);

        double aspectRatio = image.getWidth() / image.getHeight();
        double prefHeight = maxWidth / aspectRatio;

        imageSpacer.setMinHeight(prefHeight);
        imageSpacer.setPrefHeight(prefHeight);
        imageSpacer.setMaxHeight(maxHeight);

        if(prefHeight > maxHeight) {
            imageSpacer.setMinHeight(maxHeight);
            imageView.setTranslateY(maxHeight - imageView.getLayoutBounds().getHeight());
        } else {
            imageView.setTranslateY(0);
        }
    }
    
    public static void removeKeysFromSettings(String... toRemove) {
        Services.get(SettingsService.class).ifPresent(service -> {
            for (String s : toRemove) {
                service.remove(s);
            }
        });
    }
    
    public static void showToast(String message, Duration duration)  {
        final Toast toast = new Toast(message);
        toast.setDuration(duration);
        toast.show();
    }

    public static String safeStr(String s) {
        return s == null? "": s.trim();
    }

    public static void hidePastConferenceMessage() {
        if (pastConferenceMessage != null) {
            pastConferenceMessage.hide();
        }
    }

    public static void showPastConferenceMessage() {
        if (pastConferenceMessage == null) {
            Label label = new Label(DevoxxBundle.getString("OTN.CONF.PAST.MESSAGE"));
            Button button = MaterialDesignIcon.CLOSE.button();
            Region region = new Region();
            HBox hBox = new HBox(label, region, button);
            hBox.getStyleClass().add("content");
            HBox.setHgrow(region, Priority.ALWAYS);

            pastConferenceMessage = new Layer() {
                @Override
                public void layoutChildren() {
                    MobileApplication mobileApplication = MobileApplication.getInstance();
                    AppBar appBar = mobileApplication.getAppBar();
                    double prefWidth = mobileApplication.getGlassPane().getWidth();
                    double prefHeight = hBox.prefHeight(prefWidth);
                    hBox.resizeRelocate(appBar.getLayoutBounds().getMinX(), appBar.getLayoutBounds().getHeight(), prefWidth, prefHeight);
                    super.layoutChildren();
                }
            };
            pastConferenceMessage.getStyleClass().add("past-conference");
            pastConferenceMessage.setAutoHide(false);
            pastConferenceMessage.getChildren().add(hBox);
            button.setOnAction(e -> pastConferenceMessage.hide());
        }
        pastConferenceMessage.show();
    }
}
