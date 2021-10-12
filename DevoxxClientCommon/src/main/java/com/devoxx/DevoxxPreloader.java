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
package com.devoxx;

import com.gluonhq.attach.util.Platform;
import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class DevoxxPreloader extends Preloader {

    private static final Logger LOG = Logger.getLogger(DevoxxPreloader.class.getName());

    // Fix for #254
    static {
        if (Platform.isAndroid()) {
            try {
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                Object sVmRuntime = getRuntime.invoke(null);
                if (sVmRuntime != null && setHiddenApiExemptions != null) {
                    setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{new String[]{"L"}});
                }
            } catch (Throwable e) {
                LOG.severe("Error: " + e.getMessage());
            }
        }
    }
    private Stage stage;
    
    private static final boolean IS_DESKTOP = Platform.isDesktop();
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double width = IS_DESKTOP ? 350 : visualBounds.getWidth();
        double height = IS_DESKTOP ? 700 : visualBounds.getHeight();

        ImageView imageView = new ImageView(DevoxxPreloader.class.getResource("preload_small.png").toExternalForm());
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(width * 0.50);

        StackPane stackPane = new StackPane(imageView);
        stackPane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        Scene scene = new Scene(stackPane, width, height);

        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        // FIXME: hide stage only when IS_DESKTOP, when OTN-464 gets solved
        if (!Platform.isAndroid() && Type.BEFORE_START.equals(info.getType())) {
            // On iOS we shouldn't hide the stage, but for now we do it to solve OTN-454
            // This will leave a black screen for a short while until the new stage is shown
            stage.hide();
        }
    }
}
