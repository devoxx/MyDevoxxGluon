package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.News;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CardCell;
import com.gluonhq.charm.glisten.control.CardPane;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewsPresenter extends GluonPresenter<DevoxxApplication> {

    @FXML
    private View newsView;

    @FXML
    private CardPane<News> newsCardPane;

    @Inject
    private Service service;

    public void initialize() {
        newsView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.NEWS.getTitle());
            appBar.getActionItems().setAll(getApp().getSearchButton());
        });
        
        newsView.setOnShown(event -> {
            Services.get(SettingsService.class).ifPresent(settingsService -> {
                System.out.println(">>>> NewsPresenter SettingsService");
                settingsService.store(DevoxxSettings.SWITCH_TO_NEWS, Boolean.FALSE.toString());
            });
            newsCardPane.requestLayout();
        });

        Bindings.bindContent(newsCardPane.getItems(), service.retrieveNews());

        newsCardPane.setOnPullToRefresh(event -> {
            // Do nothing we only want pull to refresh to be enabled.
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
        });

        newsCardPane.setPlaceholder(new Label(DevoxxBundle.getString("OTN.NEWS.PLACEHOLDER")));
        newsCardPane.setCellFactory(param -> new NewsCardCell());
    }

    private String getTimeText(long creationDate) {
        long now = new Date().getTime();
        long differenceInMinutes = (long) Math.floor((now - creationDate) / (1000 * 60));

        if (differenceInMinutes == 0) {
            return "Just Now";
        } else if (differenceInMinutes < 60) {
            return differenceInMinutes + "m";
        } else if (differenceInMinutes < (24* 60)) {
            return differenceInMinutes / 60 + "h";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM");
            return dateFormat.format(new Date(creationDate));
        }
    }
    
    class NewsCardCell extends CardCell<News> {

        final Label headerLabel = new Label();
        final Label contentText = new Label();
        final Label headerGraphic = (Label) MaterialDesignIcon.SMS.graphic();
        final VBox content = new VBox();

        NewsCardCell() {
            final HBox header = new HBox(headerGraphic, headerLabel);

            headerLabel.getStyleClass().add("title");
            contentText.setWrapText(true);

            header.getStyleClass().add("header");

            content.getStyleClass().add("content");
            content.getChildren().addAll(header, contentText);

            final Insets cardPanePadding = newsCardPane.getPadding();
            final Insets padding = getPadding();
            final Insets contentPadding = content.getPadding();
            final Insets headerPadding = header.getPadding();
            
            double cPPadding = cardPanePadding.getLeft() + cardPanePadding.getRight();
            double cPCellPadding = padding.getLeft() + padding.getRight();
            double cPadding = contentPadding.getLeft() + contentPadding.getRight();
            double cHeaderPadding = headerPadding.getLeft() + headerPadding.getRight();
            
            headerLabel.maxWidthProperty().bind(
                    newsCardPane.widthProperty().subtract(
                            cPPadding + cPCellPadding + cPadding + cHeaderPadding + 10
                    ).subtract(headerGraphic.widthProperty())
                    .subtract(header.spacingProperty())
            );
            
            contentText.maxWidthProperty().bind(
                    newsCardPane.widthProperty().subtract(
                            cPPadding + cPCellPadding + cPadding + 10
                    )
            );
        }

        @Override 
        public void updateItem(News item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                String timeText = getTimeText(item.getCreationDate());
                headerLabel.setText(item.getTitle() + " - " + timeText);
                contentText.setText(item.getContent());
                setText("");
                setGraphic(content);
            }
        }
    }
}
