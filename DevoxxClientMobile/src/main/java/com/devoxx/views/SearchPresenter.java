/*
 * Copyright (c) 2016, 2023, Gluon Software
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
import com.devoxx.model.Exhibitor;
import com.devoxx.model.Note;
import com.devoxx.model.Searchable;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSearch;
import com.devoxx.views.cell.SearchCell;
import com.devoxx.views.cell.SearchHeaderCell;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import javax.inject.Inject;

public class SearchPresenter extends GluonPresenter<DevoxxApplication> {

    private static final int MIN_CHARACTERS = 3;
    private final Placeholder emptySearchPlaceholder = new Placeholder(DevoxxBundle.getString("OTN.SEARCH.PLACEHOLDER"), MaterialDesignIcon.SEARCH);
    private final Placeholder noResultsPlaceholder = new Placeholder(DevoxxBundle.getString("OTN.SEARCH.NO_RESULTS"), MaterialDesignIcon.SEARCH);

    @FXML
    private View search;
    
    @FXML
    private CharmListView<Searchable, String> searchListView;

    private TextField searchTextField;
    private Button clearButton;
    
    @Inject
    private Service service;

    @Inject
    private SessionVisuals sessionVisuals;

    @Inject
    private DevoxxSearch devoxxSearch;
    
    private ObservableList<Searchable> results;
    private ObservableList<Searchable> prefixResults;
    private String prefix;
    
    public void initialize() {
        results = FXCollections.observableArrayList();
        prefixResults = FXCollections.observableArrayList();
        searchTextField = new TextField();
        searchTextField.getStyleClass().add("search-text-field");
        searchTextField.setPromptText(DevoxxBundle.getString("OTN.SEARCH.PROMPT"));
        searchTextField.setOnAction(e -> searchListView.requestFocus());
        clearButton = MaterialDesignIcon.CLEAR.button(e -> {
            searchTextField.clear();
            searchListView.itemsProperty().clear();
            // there is a jump in keyboard due to focus
            searchTextField.requestFocus();
        });
        clearButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                final String text = searchTextField.getText();
                if (text.isEmpty()) {
                    searchListView.itemsProperty().clear();
                } else if (text.length() == MIN_CHARACTERS) {
                    // initial search
                    prefix = text;
                    backgroundSearch().start();
                } else if (text.length() > MIN_CHARACTERS) {
                    // while typing more characters, use the initial results to refine
                    // the search over those results.
                    if (prefix.equals(text.substring(0, MIN_CHARACTERS))) {
                        refineBackgroundSearch().start();
                    } else {
                        // Whenever the prefix changes, do a full search again
                        prefix = text.substring(0, MIN_CHARACTERS);
                        backgroundSearch().start();
                    }
                }
                return text.isEmpty();
            }, searchTextField.textProperty()));
        
        search.setOnShowing(e -> {
            AppBar appBar = AppManager.getInstance().getAppBar();
            appBar.setPrefHeight(appBar.getHeight());
            if (!appBar.getStyleClass().contains("search-app-bar")) {
                appBar.getStyleClass().add("search-app-bar");
            }
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitle(searchTextField);
            appBar.getActionItems().add(clearButton);
            setTextFieldWidth(appBar);
            appBar.widthProperty().addListener(o -> setTextFieldWidth(appBar));
            searchTextField.requestFocus();
            if (searchListView != null) {
                searchListView.setSelectedItem(null);
            }
            
        });
        search.setOnHiding(e -> {
            AppBar appBar = AppManager.getInstance().getAppBar();
            appBar.getStyleClass().remove("search-app-bar");
            appBar.setPrefHeight(-1);
            searchTextField.clear();
        });

        searchListView.setPlaceholder(emptySearchPlaceholder);
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                searchListView.setPlaceholder(noResultsPlaceholder);
            } else {
                searchListView.setPlaceholder(emptySearchPlaceholder);
            }
        });
        searchListView.setHeadersFunction(item -> item.getClass().toString());
        searchListView.setCellFactory(p -> new SearchCell<>(service, sessionVisuals));
        searchListView.getStyleClass().add("search-list-view");
        searchListView.itemsProperty().addListener((ListChangeListener<Searchable>) change -> searchListView.refresh());
        searchListView.setItems(results);
        searchListView.setHeaderCellFactory(p -> new SearchHeaderCell());
        addViewChangeListener(searchListView);
    }

    private void setTextFieldWidth(AppBar appBar) {
        searchTextField.setPrefWidth(appBar.getWidth() - appBar.getNavIcon().prefWidth(-1) - 
                        clearButton.prefWidth(-1));
    }
    
    private Thread backgroundSearch() {
        Runnable task = () -> {
            ObservableList<Searchable> search1 = devoxxSearch.search(searchTextField.getText());
            Platform.runLater(() -> {
                results.setAll(search1);
                // Keep the initial results on a list to refine the search based on them
                prefixResults.setAll(results);
            });
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
    }
    
    private Thread refineBackgroundSearch() {
        Runnable task = () -> {
            // Uses the initial results to refine the search based on them
            // We don't update the prefixResults list, as this will imply losing elements from it,
            // and in case the user hits the back key, there won't be the same items to perform the search
            // as there were before
            ObservableList<Searchable> refineSearch = devoxxSearch.refineSearch(searchTextField.getText(), prefixResults);
            Platform.runLater(() -> results.setAll(refineSearch));
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
    }

    private void addViewChangeListener(CharmListView<Searchable, String> searchListView) {
        searchListView.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue instanceof Exhibitor) {
                    DevoxxView.EXHIBITOR.switchView().ifPresent(presenter ->
                            ((ExhibitorPresenter) presenter).setExhibitor((Exhibitor) newValue));
                } else if (newValue instanceof Note) {
                    service.findSession(((Note) newValue).getSessionUuid()).ifPresent(session ->
                        DevoxxView.SESSION.switchView().ifPresent(presenter ->
                            ((SessionPresenter) presenter).showSession(session, SessionPresenter.Pane.NOTE)));
                } else if (newValue instanceof Session) {
                    DevoxxView.SESSION.switchView().ifPresent(presenter ->
                            ((SessionPresenter) presenter).showSession((Session) newValue));
                } else if (newValue instanceof Speaker) {
                    DevoxxView.SPEAKER.switchView().ifPresent(presenter ->
                            ((SpeakerPresenter)presenter).setSpeaker((Speaker) newValue));
//                } if (newValue instanceof Sponsor) {
//                    DevoxxView.SPONSOR.switchView().ifPresent( presenter ->
//                            ((SponsorPresenter)presenter).setSponsor((Sponsor) newValue));
                }
            }
        });
    }

}
