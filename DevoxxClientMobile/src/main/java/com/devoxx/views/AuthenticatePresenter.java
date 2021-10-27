/*
 * Copyright (c) 2019, Gluon Software
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
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.control.TextField;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.cloudlink.client.data.RemoteFunctionBuilder;
import com.gluonhq.cloudlink.client.data.RemoteFunctionObject;
import com.gluonhq.connect.GluonObservableObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Base64;

import static com.devoxx.util.DevoxxSettings.*;

public class AuthenticatePresenter extends GluonPresenter<DevoxxApplication> {
    
    @FXML
    private View authenticateView;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button login;
    @Inject
    private Service service;

    private Runnable success;
    private Runnable failure;

    public void initialize() {
        authenticateView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.AUTHENTICATE.getTitle());
        });
    }

    public void login() {
        final AppBar appBar = getApp().getAppBar();
        appBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        appBar.setProgressBarVisible(true);
        User user = new User(username.getText(), password.getText());
        authenticateUser(user);
    }

    public void forgetPassword() {
        Services.get(BrowserService.class).ifPresent(b -> {
            try {
                b.launchExternalBrowser(forgetPasswordURL());
            }  catch (IOException | URISyntaxException ex) {
                Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                toast.show();
            }
        });
    }

    public void register() {
        Services.get(BrowserService.class).ifPresent(b -> {
            try {
                b.launchExternalBrowser(registerURL());
            }  catch (IOException | URISyntaxException ex) {
                Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                toast.show();
            }
        });
    }

    public void authenticate(Runnable success, Runnable failure) {
        this.success = success;
        this.failure = failure;
    }

    private void authenticateUser(com.devoxx.model.User user) {
        final String cfpURL = service.getConference().getCfpURL();
        RemoteFunctionObject fnAuthenticate = RemoteFunctionBuilder.create("authenticate")
                .param("cfpEndpoint", cfpURL.substring(0, cfpURL.length() - 1))
                .param("username", user.getUsername())
                .param("password", user.getPassword())
                .object();

        final GluonObservableObject<JsonObject> response = fnAuthenticate.call(JsonObject.class);
        AppBar appBar = getApp().getAppBar();
        response.setOnSucceeded(e -> {
            JsonObject tokenId = response.get();
            storeUserDetails(tokenId.getString("id_token"));
            MobileApplication.getInstance().switchToPreviousView();
            if (success != null) {
                success.run();
            }
            appBar.setProgressBarVisible(false);
        });
        response.setOnFailed(e -> {
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
        Services.get(SettingsService.class).ifPresent(settingsService -> {
            settingsService.store(SAVED_ACCOUNT_TOKEN, token);
            settingsService.store(SAVED_ACCOUNT_EXPIRY, String.valueOf(exp));
            settingsService.store(SAVED_ACCOUNT_USERNAME, username);
        });
    }

    private String forgetPasswordURL() {
        return cfpURLWithoutAPI() + "#/reset/request";
    }

    private String registerURL() {
        return cfpURLWithoutAPI() + "#/register";
    }

    private String cfpURLWithoutAPI() {
        String cfpURL = service.getConference().getCfpURL();
        cfpURL = cfpURL.replace("api", "").replace("//", "/");
        return cfpURL;
    }
}
