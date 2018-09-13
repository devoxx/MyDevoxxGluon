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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.model.Floor;
import com.devoxx.service.Service;
import com.devoxx.views.helper.ETagImageTask;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.Alert;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.DevoxxView;
import com.devoxx.model.Exhibitor;
import com.devoxx.views.helper.ExhibitorMap;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.inject.Inject;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;

public class ExhibitionMapPresenter extends GluonPresenter<DevoxxApplication> {

    private static final Logger LOG = Logger.getLogger(ExhibitionMapPresenter.class.getName());

    private static final int MIN_SIZE = 200;
    private static final double MARGIN = 10;
    private double IMAGE_WIDTH;
    private double INNER_WIDTH;
    private double IMAGE_HEIGHT;
    private double INNER_HEIGHT;
    private double IMAGE_INSETS_X;
    private double IMAGE_INSETS_Y;

    @FXML
    private View exhibitionMap;

    @Inject
    private Service service;

    private ImageView imageView;

    private boolean zooming = false;
    private boolean enableDragging = false;
    private double initialMousePressedX, initialMousePressedY;
    private double mouseDownX, mouseDownY;
    private StackPane container;
    
    private final Alert alert;
    private final static int DELTA = 2;
    {
        alert = new Alert(AlertType.INFORMATION, "");
    }
    
    private final EventHandler<MouseEvent> pressedHandler = e -> {
        initialMousePressedX = e.getX();
        initialMousePressedY = e.getY();
        Point2D mouseDown = getImageCoordinates(initialMousePressedX, initialMousePressedY);
        mouseDownX = mouseDown.getX();
        mouseDownY = mouseDown.getY();
        enableDragging = true;
    };
    
    private final EventHandler<MouseEvent> draggedHandler = e -> {
        if (zooming || !enableDragging) {
            return;
        }
        final Point2D delta = getImageCoordinates(e.getX(), e.getY()).subtract(mouseDownX, mouseDownY);
        translateViewport(delta.getX(), delta.getY());
        Point2D mouseDown = getImageCoordinates(e.getX(), e.getY());
        mouseDownX = mouseDown.getX();
        mouseDownY = mouseDown.getY();
    };
    private final EventHandler<MouseEvent> releasedHandler = e -> enableDragging = false;
    private final EventHandler<ScrollEvent> scrollHandler = e -> zoom(Math.pow(1.01, -e.getDeltaY()), e.getX(), e.getY());
    private final EventHandler<ZoomEvent> zoomStartedHandler = e -> {
        enableDragging = false; 
        zooming = true;
    };
    private final EventHandler<ZoomEvent> zoomFinishedHandler = e -> zooming = false;
    private final EventHandler<ZoomEvent> zoomHandler = e -> zoom(1 / e.getZoomFactor(), e.getX(), e.getY());
    private final EventHandler<MouseEvent> clickedHandler = this::processClick;
    
    public void initialize() {
        exhibitionMap.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.EXHIBITION_MAP.getTitle());
        });
        exhibitionMap.setOnHiding(event -> {
            removeListeners();
            getApp().getAppBar().setProgressBarVisible(false);
        });
    }

    public void setFloor(Floor floor) {
        exhibitionMap.setCenter(null);

        AppBar appBar = getApp().getAppBar();
        appBar.setTitleText(floor.getName());
        appBar.setProgress(INDETERMINATE_PROGRESS);
        appBar.setProgressBarVisible(true);

        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.setPickOnBounds(true);
        container = new StackPane(imageView);
        exhibitionMap.setCenter(container);

        final ETagImageTask imageTask = new ETagImageTask("floor_" + floor.getId(), floor.getImageURL());
        imageTask.setOnSucceeded(e -> {
            final Image image = imageTask.getValue();
            if (image != null) {
                updateImage(image);
            }
            appBar.setProgressBarVisible(false);
        });
        imageTask.setOnFailed(e -> {
            LOG.log(Level.SEVERE, imageTask.getException().getMessage());
            appBar.setProgressBarVisible(false);
        });
        new Thread(imageTask).start();

        imageTask.image().ifPresent(image -> {
            updateImage(image);
            appBar.setProgressBarVisible(false);
        });
    }

    private void updateImage(Image image) {
        removeListeners();
        
        imageView.setImage(image);
        
        IMAGE_HEIGHT = image.getHeight();
        IMAGE_WIDTH = image.getWidth();
        
        INNER_HEIGHT = 1 * IMAGE_HEIGHT;
        INNER_WIDTH = 1 * IMAGE_WIDTH;
        
        IMAGE_INSETS_X = (IMAGE_WIDTH - INNER_WIDTH) / 2;
        IMAGE_INSETS_Y = (IMAGE_HEIGHT - INNER_HEIGHT) / 2;
        
        imageView.fitWidthProperty().bind(exhibitionMap.widthProperty().subtract(MARGIN));
        imageView.fitHeightProperty().bind(exhibitionMap.heightProperty().subtract(MARGIN));
        double fitScale = Math.max(INNER_WIDTH / (exhibitionMap.getWidth() - MARGIN),
                INNER_HEIGHT / (exhibitionMap.getHeight() - MARGIN));
        imageView.setViewport(new Rectangle2D(0, 0, (exhibitionMap.getWidth() - MARGIN) * fitScale,
                (exhibitionMap.getHeight() - MARGIN) * fitScale));
        
        addListeners();
        zoomIn();
    }
    
    private void addListeners() {
        imageView.addEventHandler(MouseEvent.MOUSE_PRESSED, pressedHandler);
        imageView.addEventHandler(MouseEvent.MOUSE_DRAGGED, draggedHandler);
        imageView.addEventHandler(MouseEvent.MOUSE_RELEASED, releasedHandler);
        
        if (Platform.isDesktop()) {
            imageView.addEventHandler(ScrollEvent.ANY, scrollHandler);
        } else {
            imageView.addEventHandler(ZoomEvent.ZOOM_STARTED, zoomStartedHandler);
            imageView.addEventHandler(ZoomEvent.ZOOM_FINISHED, zoomFinishedHandler);
            imageView.addEventHandler(ZoomEvent.ZOOM, zoomHandler);
        }
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, clickedHandler);
    }
    
    private void removeListeners() {
        imageView.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressedHandler);
        imageView.removeEventHandler(MouseEvent.MOUSE_DRAGGED, draggedHandler);
        imageView.removeEventHandler(MouseEvent.MOUSE_RELEASED, releasedHandler);
        if (Platform.isDesktop()) {
            imageView.removeEventHandler(ScrollEvent.ANY, scrollHandler);
        } else {
            imageView.removeEventHandler(ZoomEvent.ZOOM_STARTED, zoomStartedHandler);
            imageView.removeEventHandler(ZoomEvent.ZOOM_FINISHED, zoomFinishedHandler);
            imageView.removeEventHandler(ZoomEvent.ZOOM, zoomHandler);
        }
        imageView.removeEventHandler(MouseEvent.MOUSE_CLICKED, clickedHandler);
    }
    
    private Point2D getImageCoordinates(double eX, double eY) {
        double factorX = eX / imageView.getBoundsInLocal().getWidth();
        double factorY = eY / imageView.getBoundsInLocal().getHeight();
        Rectangle2D viewport = imageView.getViewport();
        return new Point2D(viewport.getMinX() + factorX * viewport.getWidth(), 
                viewport.getMinY() + factorY * viewport.getHeight());
    }
    
    private void translateViewport(double deltaX, double deltaY) {
        Rectangle2D viewport = imageView.getViewport();
        double minX = clamp(viewport.getMinX() - deltaX, 0, IMAGE_WIDTH - viewport.getWidth());
        double minY = clamp(viewport.getMinY() - deltaY, 0, IMAGE_HEIGHT - viewport.getHeight());
        imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    private void zoom(double factor, double pivotX, double pivotY) {
        Rectangle2D viewport = imageView.getViewport();
        double scale = clamp(factor,
            Math.min(MIN_SIZE / viewport.getWidth(), MIN_SIZE / viewport.getHeight()),
            Math.max(INNER_WIDTH / viewport.getWidth(), INNER_HEIGHT / viewport.getHeight()));
        Point2D pivot = getImageCoordinates(pivotX, pivotY);

        double newWidth = viewport.getWidth() * scale;
        double newHeight = viewport.getHeight() * scale;

        // to zoom over the pivot, we have for x, y:
        // (x - newMinX) / (x - viewport.getMinX()) = scale
        // solving for newMinX, newMinY:
        double newMinX = clamp(pivot.getX() - (pivot.getX() - viewport.getMinX()) * scale, 
                0, IMAGE_WIDTH - newWidth);
        double newMinY = clamp(pivot.getY() - (pivot.getY() - viewport.getMinY()) * scale, 
                0, IMAGE_HEIGHT - newHeight);

        imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
    }
    
    // The initial imageview is scaled up, we need to zoom it out
    private void zoomIn() {
        PauseTransition p = new PauseTransition(Duration.millis(30));
        p.setOnFinished(e -> {
            double factor = Math.max(INNER_WIDTH / imageView.getViewport().getWidth(), 
                                 INNER_HEIGHT / imageView.getViewport().getHeight());
            zoom(factor, imageView.getFitWidth()/2, imageView.getFitHeight()/2);
            translateViewport(-(IMAGE_WIDTH - imageView.getViewport().getWidth()) / 2, 
                              -(IMAGE_HEIGHT - imageView.getViewport().getHeight()) / 2);
        });
        p.play();
    }
    
    private double clamp(double value, double min, double max) {
        double minMax = Math.max(0, max);
        return value < min ? min : (value > minMax ? minMax : value);
    }
    
    private void processClick(MouseEvent event) {
        // This condition makes sure that it is a tap and not a drag
        if (Math.abs(initialMousePressedX - event.getX()) < DELTA && Math.abs(initialMousePressedY - event.getY()) < DELTA) {
            final Point2D mouseClick = getImageCoordinates(event.getX(), event.getY());
            double x = mouseClick.getX() - IMAGE_INSETS_X;
            double y = mouseClick.getY() - IMAGE_INSETS_Y;
            for (Entry<String, int[]> entry : ExhibitorMap.getExhibitorMap().entrySet()) {
                int[] r = entry.getValue();
                if (x >= r[0] && y >= r[1] && x < r[2] && y < r[3]) {
                    showExhibitor(entry.getKey());
                    return;
                }
            }
        }
    }

    private void showExhibitor(String boothId) {
        ReadOnlyListProperty<Exhibitor> exhibitors = service.retrieveExhibitors();
        for (Exhibitor exhibitor : exhibitors) {
            if(exhibitor.getBooth().equalsIgnoreCase(boothId)) {
                DevoxxView.EXHIBITOR.switchView().ifPresent(presenter ->
                        ((ExhibitorPresenter) presenter).setExhibitor(exhibitor));
                break;
            }
        }
    }
}