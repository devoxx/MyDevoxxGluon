package com.gluonhq.otn.control;

import com.gluonhq.otn.control.skin.CircularSelectorSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.util.Duration;

public class CircularSelector<T> extends Control {

    private ObservableList<T> items = FXCollections.observableArrayList();

    public CircularSelector() {
        getStyleClass().add("circular-selector");
    }

    @Override
    public String getUserAgentStylesheet() {
        return CircularSelector.class.getResource("circular-selector.css").toExternalForm();
    }

    @Override
    public Skin<CircularSelector> createDefaultSkin() {
        return new CircularSelectorSkin(this);
    }

    // selectedItemProperty
    private final ObjectProperty<T> selectedItemProperty = new SimpleObjectProperty<>(this, "selectedItem");
    public final ObjectProperty<T> selectedItemProperty() {
       return selectedItemProperty;
    }
    public final T getSelectedItem() {
       return selectedItemProperty.get();
    }
    public final void setSelectedItem(T value) {
        selectedItemProperty.set(value);
    }

    // mainCircleRadiusProperty
    private final DoubleProperty mainCircleRadiusProperty =
            new SimpleDoubleProperty(this, "mainCircleRadius", 150);
    public final DoubleProperty mainCircleRadiusProperty() {
        return mainCircleRadiusProperty;
    }
    public final double getMainCircleRadius() {
        return mainCircleRadiusProperty.get();
    }
    public final void setMainCircleRadius(double value) {
        mainCircleRadiusProperty.set(value);
    }

    // selectorCircleRadiusProperty
    private final DoubleProperty selectorCircleRadiusProperty =
            new SimpleDoubleProperty(this, "selectorCircleRadius", 40);
    public final DoubleProperty selectorCircleRadiusProperty() {
        return selectorCircleRadiusProperty;
    }
    public final double getSelectorCircleRadius() {
        return selectorCircleRadiusProperty.get();
    }
    public final void setSelectorCircleRadius(double value) {
        selectorCircleRadiusProperty.set(value);
    }

    // selector items
    public ObservableList<T> getItems() {
        return items;
    }

    // transitionDurationProperty
    private final ObjectProperty<Duration> transitionDurationProperty =
            new SimpleObjectProperty<>(this, "transitionDuration", Duration.millis(1000));
    public final ObjectProperty<Duration> transitionDurationProperty() {
        return transitionDurationProperty;
    }
    public final Duration getTransitionDuration() {
        return transitionDurationProperty.get();
    }
    public final void setTransitionDuration(Duration value) {
        transitionDurationProperty.set(value);
    }

}