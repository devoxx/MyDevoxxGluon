/*
 * Copyright (c) 2019, 2022, Gluon Software
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
import com.devoxx.model.User;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.gluonhq.attach.settings.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Icon;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.control.TextField;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.ResourceBundle;

import static com.devoxx.util.DevoxxSettings.*;

public class AuthenticatePresenter extends GluonPresenter<DevoxxApplication> {
    
    @FXML
    private View authenticateView;
    @FXML
    private Icon icon;
    @FXML
    private Label email;
    @FXML
    private TextField password;
    @FXML
    private Button login;
    @FXML
    private ResourceBundle resources;

    @Inject
    private Service service;

    private Runnable success;
    private Runnable failure;
    private String userAccount;

    public void initialize() {
        authenticateView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.AUTHENTICATE.getTitle()  + " 2/2");
        });

        userAccount = SettingsService.create().map(settingsService ->
                        settingsService.retrieve(SAVED_ACCOUNT_USERNAME))
                .orElse("");
        email.setText(MessageFormat.format(resources.getString("OTN.AUTHENTICATION.USERNAME"), userAccount));
        login.disableProperty().bind(password.textProperty().isNull().or(password.textProperty().isEmpty()));
    }

    public void login() {
        icon.setContent(MaterialDesignIcon.LOCK_OPEN);
        final AppBar appBar = getApp().getAppBar();
        appBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        appBar.setProgressBarVisible(true);
        User user = new User(userAccount, password.getText());
        authenticateUser(user);
    }

    public void authenticate(Runnable success, Runnable failure) {
        this.success = success;
        this.failure = failure;
    }

    private void authenticateUser(com.devoxx.model.User user) {

        AppBar appBar = getApp().getAppBar();
        service.authenticateUser(user, tokenId -> {
            storeUserDetails(tokenId);
            MobileApplication.getInstance().switchToPreviousView();
            if (success != null) {
                success.run();
            }
            appBar.setProgressBarVisible(false);
        }, () -> {
            icon.setContent(MaterialDesignIcon.LOCK);
            if (failure != null) {
                failure.run();
            }
            Toast toast = new Toast(DevoxxBundle.getString("OTN.AUTHENTICATION.FAILURE"));
            toast.show();
            appBar.setProgressBarVisible(false);
        });
    }

    private void storeUserDetails(String token) {
        String encryptedPayload = token.substring(token.indexOf(".") + 1, token.lastIndexOf("."));
        String payload = new String(Base64.getDecoder().decode(encryptedPayload));
        JsonReader reader = Json.createReader(new StringReader(payload));
        final JsonObject jsonObject = reader.readObject();
        long exp = jsonObject.getJsonNumber("exp").longValue();
        final String username = jsonObject.getString("sub");
        SettingsService.create().ifPresent(settingsService -> {
            settingsService.store(SAVED_ACCOUNT_TOKEN, token);
            settingsService.store(SAVED_ACCOUNT_EXPIRY, String.valueOf(exp));
        });
    }

}
