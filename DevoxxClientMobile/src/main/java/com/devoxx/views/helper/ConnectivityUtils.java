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

import com.gluonhq.attach.settings.SettingsService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.StatusBar;
import com.gluonhq.charm.glisten.control.Dialog;
import com.devoxx.util.DevoxxBundle;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ConnectivityUtils {
    private static final StatusBar STATUS_BAR = MobileApplication.getInstance().getStatusBar();
    private static final PseudoClass PSEUDO_CLASS_ERROR = PseudoClass.getPseudoClass("error");
    private static final String DONT_SHOW_AGAIN_KEY = "OFFLINE.DIALOG.DONT_SHOW_AGAIN";

    public static void showConnectivityIndication(boolean online) {
        if (online) {
            showOnlineIndication();
        } else {
            showOfflineIndication();
        }
    }
    
    public static void showOfflineIndication() {
        STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, true);
        
        boolean show = SettingsService.create()
                .map(s -> !"checked".equals(s.retrieve(DONT_SHOW_AGAIN_KEY)))
                .orElse(true);
        
        if (show) {
            Dialog dialog = new Dialog();
            VBox vBox = new VBox();
            vBox.getStyleClass().add("offline-dialog-content");
            Label label = new Label(DevoxxBundle.getString("OTN.OFFLINE.DIALOG.CONTENT"));
            label.setWrapText(true);
            CheckBox checkBox = new CheckBox(DevoxxBundle.getString("OTN.DIALOG.DONT_SHOW_AGAIN"));
            vBox.getChildren().addAll(label, checkBox);
            dialog.setTitleText(DevoxxBundle.getString("OTN.OFFLINE.DIALOG.TITLE"));
            dialog.setContent(vBox);
            Button okButton = new Button(DevoxxBundle.getString("OTN.OFFLINE.DIALOG.OK"));
            okButton.setOnAction(event -> {
                if (checkBox.isSelected()) {
                    SettingsService.create().ifPresent(s -> s.store(DONT_SHOW_AGAIN_KEY, "checked"));
                }
                dialog.hide();
            });
            dialog.getButtons().add(okButton);
            dialog.showAndWait();
        }
        
    }

    public static void showOnlineIndication() {
        STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, false);
    }
    
}