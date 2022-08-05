/*
 * Copyright (c) 2022, Gluon Software
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
import com.devoxx.util.DevoxxBundle;
import com.gluonhq.attach.settings.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
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
import javafx.scene.control.Hyperlink;

import javax.json.JsonObject;

import static com.devoxx.util.DevoxxSettings.SAVED_ACCOUNT_USERNAME;

public class ActivatePresenter extends GluonPresenter<DevoxxApplication> {
    
    @FXML
    private View activateView;
    @FXML
    private TextField username;
    @FXML
    private Button submit;
    @FXML
    private Hyperlink link;

    private Runnable success;
    private Runnable failure;

    public void initialize() {
        activateView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.AUTHENTICATE.getTitle() + " 1/2");
        });

        submit.disableProperty().bind(username.textProperty().isNull().or(username.textProperty().isEmpty()));
        SettingsService.create().ifPresent(settingsService -> {
            String userAccount = settingsService.retrieve(SAVED_ACCOUNT_USERNAME);
            if (userAccount != null && !userAccount.isEmpty()) {
                username.setText(userAccount);
                link.setVisible(true);
            }
        });
    }

    public void activateAndAuthenticate(Runnable success, Runnable failure) {
        this.success = success;
        this.failure = failure;
    }

    public void submit() {
        final AppBar appBar = getApp().getAppBar();
        appBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        appBar.setProgressBarVisible(true);
        SettingsService.create().ifPresent(settingsService ->
                settingsService.store(SAVED_ACCOUNT_USERNAME, username.getText()));
        activateUser(username.getText());
    }

    private void activateUser(String username) {
        RemoteFunctionObject fnActivate = RemoteFunctionBuilder.create("activation_6digit_key")
                .param("email", username)
                .object();

        final GluonObservableObject<JsonObject> response = fnActivate.call(JsonObject.class);
        AppBar appBar = getApp().getAppBar();
        response.setOnSucceeded(e -> {
            Toast toast = new Toast(DevoxxBundle.getString("OTN.ACTIVATION.SUCCESS"));
            toast.show();
            appBar.setProgressBarVisible(false);
            authenticate();
        });
        response.setOnFailed(e -> {
            if (failure != null) {
                failure.run();
            }
            Toast toast = new Toast(DevoxxBundle.getString("OTN.ACTIVATION.FAILURE"));
            toast.show();
            appBar.setProgressBarVisible(false);
        });
    }

    @FXML
    private void authenticate() {
        DevoxxView.AUTHENTICATE.switchView().ifPresent(view ->
                ((AuthenticatePresenter) view).authenticate(success, failure));
    }

}
