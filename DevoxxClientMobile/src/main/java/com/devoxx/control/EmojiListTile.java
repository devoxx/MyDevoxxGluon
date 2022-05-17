package com.devoxx.control;

import com.devoxx.util.emoji.TextUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.Optional;

public class EmojiListTile extends BorderPane {

    private final ObjectProperty<Node> primaryGraphicProperty = new SimpleObjectProperty<>();
    private final ObservableList<String> textProperty = FXCollections.observableArrayList();
    private final ObjectProperty<Node> secondaryGraphicProperty = new SimpleObjectProperty<>();
    private final BooleanProperty wrapText = new SimpleBooleanProperty();

    private final TextFlow primaryTextLabel = new TextFlow();
    private final VBox textBox = new VBox();

    private String currentTileStyleClass = null;

    public EmojiListTile() {

        getStyleClass().setAll("emoji-list-tile");

        textBox.getStyleClass().setAll("text-box");
        primaryTextLabel.getStyleClass().setAll("primary-text");
        primaryTextLabel.setMaxHeight(Double.MAX_VALUE);

        primaryGraphicProperty.addListener( (o, old, node) -> replaceGraphic( node, true ));
        secondaryGraphicProperty.addListener( (o, old, node) -> replaceGraphic( node, false ));

        textProperty.addListener((ListChangeListener.Change<? extends String> c) -> updateText());
        wrapText.addListener((obs, ov, nv) -> updateWrap());

        // pre-initialize layout
        setCenter(textBox);
        BorderPane.setAlignment(textBox, Pos.CENTER_LEFT);
    }

    // --- primary graphic
    public final ObjectProperty<Node> primaryGraphicProperty() {
        return primaryGraphicProperty;
    }
    public final Node getPrimaryGraphic() {
        return primaryGraphicProperty.get();
    }
    public final void setPrimaryGraphic(Node graphic) {
        primaryGraphicProperty.set(graphic);
    }

    // text
    public final ObservableList<String> textProperty() {
        return textProperty;
    }

    public void setTextLine(int i, String v) {
        while (textProperty.size() < i) {
            textProperty.add("");
        }
        if (textProperty.size() == i) {
            textProperty.add(v);
        } else {
            textProperty.set(i, v);
        }
    }


    // secondary graphic
    public final ObjectProperty<Node> secondaryGraphicProperty() {
        return secondaryGraphicProperty;
    }
    public final Node getSecondaryGraphic() {
        return secondaryGraphicProperty.get();
    }
    public final void setSecondaryGraphic(Node graphic) {
        secondaryGraphicProperty.set(graphic);
    }

    // wrap text
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }
    public final boolean isWrapText() {
        return wrapText.get();
    }
    public final void setWrapText(boolean value) {
        wrapText.set(value);
    }

    private void replaceGraphic( Node newNode, boolean primary ) {

        Optional.ofNullable(primary? getLeft(): getRight()).ifPresent( n -> getChildren().remove(n));
        Optional.ofNullable(newNode).ifPresent( n-> {
            n.getStyleClass().add( primary? "primary-graphic": "secondary-graphic");
            BorderPane.setAlignment(n, Pos.CENTER);
            if (primary) {setLeft(n);} else {setRight(n);}
        });
    }

    private void updateText() {
        if (textProperty.size() == textBox.getChildren().size()) {
            primaryTextLabel.getChildren().setAll(TextUtils.textEmojiHelper(textProperty.isEmpty() ? "" : textProperty.get(0)));
            for (int i = 1; i < textProperty.size(); i++) {
                Label label = (Label) textBox.getChildren().get(i);
                label.setText(textProperty.get(i));
            }
        } else {
            textBox.getChildren().clear();
            primaryTextLabel.getChildren().setAll(TextUtils.textEmojiHelper(textProperty.isEmpty() ? "" : textProperty.get(0)));
            textBox.getChildren().add(primaryTextLabel);
            VBox.setVgrow(primaryTextLabel, Priority.ALWAYS);
            for (int i = 1; i <= 2; i++) {
                if (textProperty.size() > i) {
                    Label secondaryLine = new Label(textProperty.get(i));
                    secondaryLine.setMaxHeight(Double.MAX_VALUE);
                    secondaryLine.setAlignment(Pos.TOP_LEFT);
                    secondaryLine.getStyleClass().add(i == 1 ? "secondary-text" : "tertiary-text");
                    textBox.getChildren().add(secondaryLine);
                    VBox.setVgrow(secondaryLine, Priority.ALWAYS);
                }
            }
            String newTileStyleClass = getTileStyle();
            if (currentTileStyleClass != null && !newTileStyleClass.equals(currentTileStyleClass) ) {
                getStyleClass().remove(currentTileStyleClass);
                getStyleClass().add(newTileStyleClass);
                currentTileStyleClass = newTileStyleClass;
            }
        }
        updateWrap();
    }

    private void updateWrap() {
        for (int i = 1; i < textProperty.size(); i++) {
            Label label = (Label) textBox.getChildren().get(i);
            label.setWrapText(isWrapText());
        }
    }

    private String getTileStyle() {
        switch (textProperty.size()) {
            case 0:
            case 1:  return "single-line-tile";
            case 2:  return "dual-line-tile";
            default: return "triple-line-tile";
        }
    }
}