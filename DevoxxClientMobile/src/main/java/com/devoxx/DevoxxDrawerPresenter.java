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
package com.devoxx;

import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxCountry;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.gluonhq.charm.glisten.application.MobileApplication.HOME_VIEW;

@Singleton
public class DevoxxDrawerPresenter extends GluonPresenter<DevoxxApplication> {

    private static final PseudoClass PSEUDO_CLASS_VOXXED = PseudoClass.getPseudoClass("voxxed");

    private final NavigationDrawer drawer;
    private final Header header;
    private final NavigationDrawer.Item logOut;
    
    @Inject
    private Service service;
    
    public DevoxxDrawerPresenter() {
        drawer = getApp().getDrawer();
        header = new Header();
        
        drawer.setHeader(header);

        for (AppView view : DevoxxView.REGISTRY.getViews()) {
            if (view.isShownInDrawer()) {
                drawer.getItems().add(view.getMenuItem());
            }
        }

        logOut = new NavigationDrawer.Item(DevoxxBundle.getString("OTN.DRAWER.LOG_OUT"), MaterialDesignIcon.CANCEL.graphic());
        logOut.managedProperty().bind(logOut.visibleProperty());
        logOut.selectedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                if (service.logOut()) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.LOGGED_OUT_MESSAGE"));
                    toast.show();

                    // switch to home view
                    getApp().switchView(HOME_VIEW);
                }
            }
        });
        drawer.getItems().add(logOut);
        drawer.openProperty().addListener((o, nv, ov) -> {
            if (nv) {
                logOut.setVisible(service.isAuthenticated() && !DevoxxSettings.AUTO_AUTHENTICATION);
            }
        });
    }

    @PostConstruct
    public void postConstruct() {
        header.text.setText(conferenceNameWithCountry(service.getConference()));
        checkAndAddBadgesItem(service.getConference());
        service.conferenceProperty().addListener((obs, ov, nv) -> {
            header.text.pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, nv.getEventType() == Conference.Type.VOXXED);
            header.text.setText(conferenceNameWithCountry(nv));
            checkAndAddBadgesItem(nv);
        });
    }

    private void checkAndAddBadgesItem(Conference conference) {
        if (conference == null) return;
        if (DevoxxSettings.conferenceHasBadgeView(conference)) {
            for (Node item : drawer.getItems()) {
                if (((NavigationDrawer.Item) item).getTitle().equals(DevoxxBundle.getString("OTN.VIEW.NOTES"))) {
                    final int index = drawer.getItems().indexOf(item) + 1;
                    if (! ((NavigationDrawer.Item) drawer.getItems().get(index)).getTitle().equals(DevoxxBundle.getString("OTN.VIEW.BADGES"))) {
                        drawer.getItems().add(index, DevoxxView.BADGES.getMenuItem());
                    }
                    break;
                }
            }
        } else {
            for (Node item : drawer.getItems()) {
                if (((NavigationDrawer.Item) item).getTitle().equals(DevoxxBundle.getString("OTN.VIEW.BADGES"))) {
                    drawer.getItems().remove(item);
                    break;
                }
            }
        }
    }

    private String conferenceNameWithCountry(Conference conference) {
        if (conference != null) {
            switch (conference.getEventType()) {
                case DEVOXX:
                    return conference.getEventType().getDisplayName() + " " + DevoxxCountry.getConfShortName(conference.getCountry());
                case VOXXED:
                    return conference.getEventType().getDisplayName() + " " + conference.getCountry();
            }
        }
        return "";
    }

    private class Header extends Region {

        private final ImageView background;
//        private final Button profileButton;
        private final Label text;
        private final double aspectRatio;

        public Header() {
            // background image
            background = Util.getMediaBackgroundImageView();
            final Image image = background.getImage();
            aspectRatio = image == null ? 0.3 : image.getHeight() / image.getWidth();
            background.setFitWidth(getWidth());
            // text
            text = new Label();
            text.getStyleClass().add("primary-title");

//            // profile button
//            profileButton  = MaterialDesignIcon.SETTINGS.button(e -> {
//                getApp().hideLayer(MENU_LAYER);
//                DevoxxView.PROFILE.switchView();
//            });
//            // the profile button is only visible when the user is logged in
//            profileButton.setVisible(false);
//            profileButton.managedProperty().bind(profileButton.visibleProperty());
            
            getChildren().addAll(background, text/*, profileButton*/);
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();

            background.setFitWidth(w);

            // Position text in bottom-left
            final double labelHeight = text.prefHeight(w);
            text.resizeRelocate(0, h - labelHeight, w, labelHeight);

            // put profile down bottom-right
//            final double profileBtnWidth = profileButton.prefWidth(-1);
//            final double profileBtnHeight = profileButton.prefHeight(-1);
//            profileButton.resizeRelocate(x - profileBtnWidth, h - profileBtnHeight - padding, profileBtnWidth, profileBtnHeight);
        }

        @Override
        protected double computePrefHeight(double width) {
            return width * aspectRatio;
        }
        
    }
}