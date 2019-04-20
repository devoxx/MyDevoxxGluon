/**
 * Copyright (c) 2017, 2018 Gluon Software
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

import java.util.Objects;

import javax.inject.Inject;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Badge;
import com.devoxx.model.BadgeType;
import com.devoxx.model.SponsorBadge;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Placeholder;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class BadgePresenter extends GluonPresenter<DevoxxApplication> {
    private static final int WAIT_TIME = 3000; // In milliseconds

    @FXML
    private View badgeView;
    
    @Inject
    private Service service;
    
    @FXML
    private TextField name;

    @FXML
    private TextField email;

    @FXML
    private TextField language;
    
    @FXML
    private TextField age;

    @FXML
    private TextField gender;

    @FXML
    private TextField company;    
    
    @FXML
    private TextField city;
    
    @FXML
    private TextField country;

    @FXML
    private TextField programmingLanguages;

    @FXML
    private TextField jobTitle;


    
    @FXML
    private TextArea details;
    
    private Badge badge;
    private Timeline timer;
    private boolean textChanged;
    private BadgeType badgeType;

    public void initialize() {
        badgeView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.BADGE.getTitle());
            appBar.getActionItems().add(MaterialDesignIcon.DELETE.button(e -> {
                final Dialog<Button> dialog = createDialog();
                dialog.showAndWait();
            }));
        });
        
        timer = new Timeline();
        timer.setCycleCount(1);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(WAIT_TIME), event -> {
            if (textChanged) {
                saveBadge();
            }
        });
        timer.getKeyFrames().add(keyFrame);
        
        details.textProperty().addListener((observable, oldValue, newValue) -> {
            textChanged = true;
            timer.playFromStart();
        });
        
        badgeView.setOnHiding(event -> saveBadge());
        
        // Fix for keyboard not showing on Android.
        // As TextArea is the only focusable control, 
        // it receives focus by default.
        // Set the focus on the View, such that TextArea receives
        // focus when tapped.
        badgeView.setOnShown(event -> badgeView.requestFocus());
    }

    private Dialog<Button> createDialog() {
        Dialog<Button> dialog = new Dialog<>();
        Placeholder deleteDialogContent = new Placeholder(DevoxxBundle.getString("OTN.BADGE.DIALOG.REMOVE.TITLE"), 
                DevoxxBundle.getString("OTN.BADGE.DIALOG.REMOVE.CONTENT"), MaterialDesignIcon.HELP);

        deleteDialogContent.setPrefWidth(MobileApplication.getInstance().getView().getScene().getWidth() - 40);

        dialog.setContent(deleteDialogContent);
        Button yesButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.YES"));
        Button noButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.NO"));
        yesButton.setOnAction(ev -> {
            removeBadge();
            badge = null;
            dialog.hide();
            getApp().switchToPreviousView();
        });
        noButton.setOnAction(ev -> dialog.hide());
        dialog.getButtons().addAll(noButton, yesButton);
        return dialog;
    }

    private void removeBadge() {
        if (badgeType == BadgeType.ATTENDEE) {
            service.retrieveBadges().remove(badge);
        } else {
        	service.removeSponsorBadge((SponsorBadge)badge);
            // service.retrieveSponsorBadges(((SponsorBadge)badge).getSponsor()).remove(badge);
        }
    }

    public void setBadge(Badge badge, BadgeType badgeType) {
        Objects.requireNonNull(badge);
        Objects.requireNonNull(badge.getEmail());

        this.badge = badge;
        this.badgeType = badgeType;
        
        if (badgeType == BadgeType.ATTENDEE) {
            if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
                final ObservableList<Badge> badges = service.retrieveBadges();
                if (badges.contains(badge)) {
                    this.badge = badge;
                }
            }
        } else {
            final SponsorBadge sponsorBadge = (SponsorBadge) badge;
            final ObservableList<SponsorBadge> badges = service.retrieveSponsorBadges(sponsorBadge.getSponsor());
            if (badges.contains(badge)) {
                this.badge = badge;
            }
        }
        if (this.badge != null) {
            name.setText(badge.getName());            
            email.setText(badge.getEmail());
            language.setText(badge.getLanguage());
            age.setText(badge.getAge()+"");
            gender.setText(badge.getGender());
            company.setText(badge.getCompany());
            city.setText(badge.getCity());
            country.setText(badge.getCountry());
            programmingLanguages.setText(badge.getProgrammingLanguages());
            jobTitle.setText(badge.getJobTitle());
            details.setText(badge.getDetails());
        }
    }
    
    private void saveBadge() {
        if (! textChanged || badge == null) {
            return;
        }

        badge.setName(name.getText());
        badge.setEmail(email.getText());
        badge.setLanguage(language.getText());
        badge.setAge(Integer.parseInt(age.getText()));
        badge.setGender(gender.getText());
        badge.setCompany(company.getText());
        badge.setCity(city.getText());
        badge.setCountry(country.getText());
        badge.setProgrammingLanguages(programmingLanguages.getText());
        badge.setJobTitle(jobTitle.getText());
        
        badge.setDetails(details.getText());
        
        if (badge instanceof SponsorBadge) {
            // every scanned sponsor badge must be posted with the remote function
            service.saveSponsorBadge((SponsorBadge) badge);
        }

        textChanged = false;
    }

}
