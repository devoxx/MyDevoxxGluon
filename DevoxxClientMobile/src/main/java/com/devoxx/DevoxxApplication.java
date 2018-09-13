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

import com.airhacks.afterburner.injection.Injector;
import com.devoxx.model.Badge;
import com.devoxx.model.BadgeType;
import com.devoxx.model.Sponsor;
import com.devoxx.model.SponsorBadge;
import com.devoxx.service.DevoxxService;
import com.devoxx.service.Service;
import com.devoxx.util.*;
import com.devoxx.views.DevoxxSplash;
import com.devoxx.views.SessionsPresenter;
import com.devoxx.views.helper.ConnectivityUtils;
import com.devoxx.views.helper.SessionVisuals;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.*;
import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.afterburner.GluonInstanceProvider;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.usage.UsageClient;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.DevoxxView.SEARCH;

public class DevoxxApplication extends MobileApplication {

    private static final Logger LOG = Logger.getLogger(DevoxxApplication.class.getName());
    public static final String POPUP_FILTER_SESSIONS_MENU = "FilterSessionsMenu";

    private static final GluonInstanceProvider instanceSupplier = new GluonInstanceProvider() {{
        bindProvider(Service.class, DevoxxService::new);
        bindProvider(DevoxxSearch.class, DevoxxSearch::new);
        bindProvider(DevoxxNotifications.class, DevoxxNotifications::new);
        bindProvider(SessionVisuals.class, SessionVisuals::new);

        Injector.setInstanceSupplier(this);
    }};

    private final Button navMenuButton   = MaterialDesignIcon.MENU.button(e -> getDrawer().open());
    private final Button navBackButton   = MaterialDesignIcon.ARROW_BACK.button(e -> switchToPreviousView());
    private final Button navHomeButton   = MaterialDesignIcon.HOME.button(e -> goHome());
    private final Button navSearchButton = MaterialDesignIcon.SEARCH.button(e -> SEARCH.switchView());

    private Service service;

    private boolean skipVideo = false;
    private boolean signUp = false;

    @Override
    public void init() {

        // Config logging
        DevoxxLogging.config();

        new UsageClient().enable();

        // start service data preloading as soon as possible
        service = Injector.instantiateModelOrService(Service.class);

        Injector.instantiateModelOrService(DevoxxNotifications.class);

        for (AppView view : DevoxxView.REGISTRY.getViews()) {
            view.registerView(this);
        }

        Services.get(SettingsService.class).ifPresent(settings -> {
            String skip = settings.retrieve(DevoxxSettings.SKIP_VIDEO);
            if (!Strings.isNullOrEmpty(skip)) {
                skipVideo = Boolean.parseBoolean(skip);
            }
            String sign = settings.retrieve(DevoxxSettings.SIGN_UP);
            if (!Strings.isNullOrEmpty(sign)) {
                signUp = Boolean.parseBoolean(sign);
            }
        });
        if (!skipVideo) {
            Services.get(SettingsService.class).ifPresent(settings ->
                    settings.store(DevoxxSettings.SKIP_VIDEO, Boolean.TRUE.toString())
            );
            addViewFactory(SPLASH_VIEW, DevoxxSplash::new);
        }
    }

    @Override
    public void postInit(Scene scene) {

        // Check if conference is set and switch to Sessions view
        Services.get(SettingsService.class).ifPresent(settingsService -> {
            String configuredConference = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (configuredConference != null) {
                DevoxxView.SESSIONS.switchView();
            }
        });

        String deviceFactorSuffix = Services.get(DeviceService.class)
                .map(s -> {
                    if (Platform.isAndroid() && s.getModel() != null) {
                        for (String device : DevoxxSettings.DEVICES_WITH_SANS_CSS) {
                            if (s.getModel().toLowerCase(Locale.ROOT).startsWith(device)) {
                                return "_sans";
                            }
                        }
                    }
                    return "";
                })
                .orElse("");

        String formFactorSuffix = Services.get(DisplayService.class)
                .map(s -> s.isTablet() ? "_tablet" : s.hasNotch() ? "_notch" : "")
                .orElse("");

        String stylesheetName = String.format("devoxx_%s%s%s.css",
                Platform.getCurrent().name().toLowerCase(Locale.ROOT),
                deviceFactorSuffix,
                formFactorSuffix);
        scene.getStylesheets().add(DevoxxApplication.class.getResource(stylesheetName).toExternalForm());
        
        if (Platform.isDesktop()) {
            Window window = scene.getWindow();
            ((Stage) window).getIcons().add(new Image(DevoxxApplication.class.getResourceAsStream("/icon.png")));
            window.setWidth(350);
            window.setHeight(700);
        }
        
        Injector.instantiateModelOrService(DevoxxDrawerPresenter.class);

        scene.getWindow().showingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    initConnectivityServices();
                    scene.getWindow().showingProperty().removeListener(this);
                }
            }
        });
        
        if (signUp) {
            Services.get(SettingsService.class).ifPresent(settings -> settings.remove(DevoxxSettings.SIGN_UP));
            DevoxxView.SESSIONS.switchView().ifPresent(s -> ((SessionsPresenter) s).selectFavorite());
        }
    }

    private void initConnectivityServices() {
        Services.get(ConnectivityService.class).ifPresent(connectivityService -> {
            connectivityService.connectedProperty().addListener((observable, oldValue, newValue) -> {
                ConnectivityUtils.showConnectivityIndication(newValue);
            });

            ConnectivityUtils.showConnectivityIndication(connectivityService.isConnected());
        });

    }


    public Button getNavMenuButton() {
        return navMenuButton;
    }

    public Button getNavBackButton() {
        return navBackButton;
    }

    public Button getNavHomeButton() {
        return navHomeButton;
    }
    
    public Button getSearchButton() {
        return navSearchButton;
    }
    
    public Button getShareButton(BadgeType badgeType, Sponsor sponsor) {
        return MaterialDesignIcon.SHARE.button(e -> {
            Services.get(ShareService.class).ifPresent(s -> {
                File root = Services.get(StorageService.class).flatMap(storage -> storage.getPublicStorage("Documents")).orElse(null);
                if (root != null) {
                    File file = new File(root, "Devoxx" + DevoxxCountry.getConfShortName(service.getConference().getCountry()) + "-badges.csv");
                    if (file.exists()) {
                        file.delete();
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        if (BadgeType.ATTENDEE == badgeType) {
                            writer.write("ID,First Name,Last Name,Company,Email,Details");
                            writer.newLine();
                            for (Badge badge : service.retrieveBadges()) {
                                writer.write(badge.toCSV());
                                writer.newLine();
                            }
                        } else if (BadgeType.SPONSOR == badgeType) {
                            writer.write("ID,First Name,Last Name,Company,Email,Details,Slug");
                            writer.newLine();
                            for (SponsorBadge badge : service.retrieveSponsorBadges(sponsor)) {
                                writer.write(badge.toCSV());
                                writer.newLine();
                            }
                        } else {
                            LOG.log(Level.WARNING, "Error invalid badgeType: " + badgeType);
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.WARNING, "Error writing csv file ", ex);
                    }
                    s.share(DevoxxBundle.getString("OTN.BADGES.SHARE.SUBJECT"),
                            DevoxxBundle.getString("OTN.BADGES.SHARE.MESSAGE", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.now())),
                            "text/plain", file);
                } else {
                    LOG.log(Level.WARNING, "Error accessing local storage");
                }
            });
        }); 
    }
}
