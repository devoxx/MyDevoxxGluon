/*
 * Copyright (c) 2018, Gluon Software
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

import static com.devoxx.model.BadgeType.ATTENDEE;

import java.util.Optional;

import javax.inject.Inject;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Badge;
import com.devoxx.model.BadgeType;
import com.devoxx.service.Service;
import com.devoxx.util.BasicSecureFilter;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.BadgeCell;
import com.devoxx.views.helper.LoginPrompter;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BarcodeScanService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;

public class AttendeeBadgePresenter extends GluonPresenter<DevoxxApplication> {

    private static final String ANONYMOUS_MESSAGE = DevoxxBundle.getString("OTN.BADGES.ANONYMOUS_MESSAGE");
    private static final String EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.BADGES.EMPTY_LIST_MESSAGE");
    
    @FXML
    private View attendeeView;
    
    @Inject
    private Service service;

    private FloatingActionButton scan;
    private CharmListView<Badge, String> attendeeBadges;
    private boolean addToStack;

    public void initialize() {
        
        attendeeView.setOnShowing(e -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.ATTENDEE_BADGE.getTitle());
            
            authenticate();
        });
        
        attendeeView.setOnHiding(event -> {
            if (scan != null) {
                scan.hide();
            }
        });
    }

    /**
     * Sets a flag which removes the last added view to the stack
     * when authentication is successful.
     */
    public void addToStack() {
        addToStack = true;
    }

    private void authenticate() {
        if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
            loadAuthenticatedView();
        } else {
            loadAnonymousView();
        }
    }

    private void loadAnonymousView() {
        attendeeView.setCenter(new LoginPrompter(service, ANONYMOUS_MESSAGE, DevoxxView.ATTENDEE_BADGE.getMenuIcon(), () -> {
            loadAuthenticatedView();
            Platform.runLater(() -> Util.showToast(DevoxxBundle.getString("OTN.BADGES.LOGIN.ATTENDEE"), Duration.seconds(5)));
        }));
    }

    private void loadAuthenticatedView() {
        if (addToStack) {
            // Remove last added view from view stack
            addToStack = false;
            MobileApplication.getInstance().switchToPreviousView();
            DevoxxView.ATTENDEE_BADGE.switchView();
        }
        Services.get(SettingsService.class).ifPresent(service -> {
            service.store(DevoxxSettings.BADGE_TYPE, BadgeType.ATTENDEE.toString());
        });
        showAttendee();
    }

    private void showAttendee() {
        final ObservableList<Badge> badges = service.retrieveBadges();

        if (attendeeBadges == null) {
            attendeeBadges = new CharmListView<>(badges);
            attendeeBadges.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.BADGES.getMenuIcon()));
            attendeeBadges.setCellFactory(param -> new BadgeCell<>());
        }
        attendeeView.setCenter(attendeeBadges);

        final Button shareButton = getApp().getShareButton(BadgeType.ATTENDEE, null);
        shareButton.disableProperty().bind(attendeeBadges.itemsProperty().emptyProperty());

        final AppBar appBar = getApp().getAppBar();
        appBar.setTitleText(DevoxxBundle.getString("OTN.VIEW.BADGES"));
        appBar.setNavIcon(getApp().getNavMenuButton());
        appBar.getActionItems().setAll(getApp().getSearchButton(), shareButton);
        appBar.getMenuItems().setAll(getBadgeChangeMenuItem("Logout"));
        
        if (scan == null) {
            scan = new FloatingActionButton("", e -> {
                if (DevoxxSettings.BADGE_TESTS) {
                    addBadge(badges, Util.getDummyQR());
                    return;
                }
                Services.get(BarcodeScanService.class).ifPresent(s -> {
                    final Optional<String> scanQr = s.scan(DevoxxBundle.getString("OTN.BADGES.ATTENDEE.QR.TITLE"), null, null);
                    scanQr.ifPresent(qr -> addBadge(badges, qr));
                });
            });
            scan.getStyleClass().add("badge-scanner");
        }
        scan.show();
    }

    private void addBadge(ObservableList<Badge> badges, String qr) {
    	BasicSecureFilter filter = new BasicSecureFilter();
        Badge badge = Badge.parseBadge(filter.decrypt(qr));
        if (badge.getEmail() != null) {
            boolean exists = false;
            for (Badge b : badges) {
                if (b.getEmail().equals(badge.getEmail())) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.QR.EXISTS"));
                    toast.show();
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                attendeeBadges.itemsProperty().add(badge);
                // Keep AttendeeBadgeView on stack 
                DevoxxView.BADGE.switchView(ViewStackPolicy.USE).ifPresent(presenter -> ((BadgePresenter) presenter).setBadge(badge, ATTENDEE));
            }
        } else {
            Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.BAD.QR"));
            toast.show();
        }
    }

    private MenuItem getBadgeChangeMenuItem(String text) {
        final MenuItem scanAsDifferentUser = new MenuItem(text);
        scanAsDifferentUser.setOnAction(ev -> {
            Util.removeKeysFromSettings(DevoxxSettings.BADGE_TYPE);
            DevoxxView.BADGES.switchView();
        });
        return scanAsDifferentUser;
    }
}
