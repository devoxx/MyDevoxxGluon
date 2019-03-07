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
package com.devoxx.views.layer;

import com.devoxx.DevoxxView;
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.glisten.application.GlassPane;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.layout.Layer;
import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.devoxx.views.helper.Util.*;
import com.gluonhq.charm.glisten.control.LifecycleEvent;
import static javafx.animation.Animation.Status.STOPPED;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ConferenceLoadingLayer extends Layer {

    private static final Duration TIMEOUT = Duration.seconds(15);
    private static final Map<Conference, ConferenceLoadingLayer> map = new HashMap<>();
    private static final PseudoClass PSEUDO_CLASS_VOXXED = PseudoClass.getPseudoClass("voxxed");

    private final Service service;
    private final GlassPane glassPane;
    private final DisplayService displayService;

    private final Label conferenceLabel;
    private final Region background;
    private final ProgressIndicator progressIndicator;

    private final PauseTransition timeout;
    private final InvalidationListener sessionsListener;
    private final BooleanProperty shown;

    private ConferenceLoadingLayer(Service service, Conference conference) {
        this.service = service;
        glassPane = MobileApplication.getInstance().getGlassPane();
        displayService = Services.get(DisplayService.class).orElse(null);

        progressIndicator = new ProgressIndicator();
        progressIndicator.pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, Conference.Type.VOXXED == conference.getEventCategory());

        conferenceLabel = new Label(conference.getName());

        background = new Region();
        background.getStyleClass().add("background");

        setAutoHide(false);
        getStyleClass().add("conf-layer");
        getChildren().addAll(background, progressIndicator, conferenceLabel);

        timeout = new PauseTransition(TIMEOUT);
        timeout.setOnFinished(e -> hide());
        // Listener helps for quick response in cases where data has been cached
        sessionsListener = o -> {
            if (service.retrieveSessions().size() > 0) {
                doHide();
            }
        };
        
        shown = new SimpleBooleanProperty();
        addEventFilter(LifecycleEvent.SHOWN, e -> shown.set(true));
    }
    
    public static void show(Service service, Conference conference) {
        ConferenceLoadingLayer cll = map.get(conference);
        if (cll == null) {
            cll = new ConferenceLoadingLayer(service, conference);
            map.put(conference, cll);
        }
        cll.show();
    }

    public static void hide(Conference conference) {
        Optional.ofNullable(map.get(conference)).ifPresent(c -> c.doHide());
    }
    
    @Override
    public void show() {
        shown.set(false);
        timeout.playFromStart();
        service.retrieveSessions().addListener(sessionsListener);
        super.show();
    }

    private void doHide() {
        if (shown.get()) {
            hide();
        } else {
            shown.addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable o) {
                    if (shown.get()) {
                        shown.removeListener(this);
                        hide();
                    }
                }
            });
        }
    }
    
    @Override
    public void hide() {
        if (timeout.getStatus() != STOPPED) timeout.stop();
        service.retrieveSessions().removeListener(sessionsListener);
        if (!isShowing()) return;
        DevoxxView.SESSIONS.switchView();
        if (service.getConference() != null && isConferenceFromPast(service.getConference())) {
            showPastConferenceMessage();
        } else {
            hidePastConferenceMessage();
        }
        super.hide();
    }

    @Override
    public void layoutChildren() {
        final double glassPaneWidth = snapSize(glassPane.getWidth());
        final double glassPaneHeight = snapSize(glassPane.getHeight());
        background.resizeRelocate(0, 0, glassPaneWidth, glassPaneHeight);

        double radius = Math.min(glassPaneWidth, glassPaneHeight) / 2 - 20;
        if (displayService != null && displayService.isTablet()) {
            radius = Math.min(glassPaneWidth, glassPaneHeight) / 4;
        }
        progressIndicator.setRadius(radius);
        progressIndicator.resizeRelocate(glassPaneWidth / 2 - radius, glassPaneHeight / 2 - radius, radius * 2, radius * 2);

        double labelWidth = Math.min(conferenceLabel.prefWidth(-1), radius - 10);
        double labelHeight = conferenceLabel.prefHeight(labelWidth);
        conferenceLabel.resizeRelocate(glassPaneWidth / 2 - labelWidth / 2, glassPaneHeight / 2 - labelHeight / 2, labelWidth, labelHeight);

        super.layoutChildren();
    }
}
