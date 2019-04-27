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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.NoteCell;
import com.devoxx.views.helper.LoginPrompter;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.model.Note;
import com.devoxx.views.helper.Placeholder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class NotesPresenter extends GluonPresenter<DevoxxApplication> {
    private static final String ANONYMOUS_MESSAGE = DevoxxBundle.getString("OTN.NOTES.ANONYMOUS_MESSAGE");
    private static final String EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.NOTES.EMPTY_LIST_MESSAGE");

    private static boolean listenersAdded = false;
    
    @FXML
    private View notesView;

    private CharmListView<Note, String> lvNotes;

    private NoteListener noteListener;

    @Inject
    private Service service;

    public void initialize() {
        notesView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.NOTES.getTitle());
//            appBar.getActionItems().add(getApp().getSearchButton());
            lvNotes.setSelectedItem(null);
            
            if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
                loadAuthenticatedView();
            } else {
                loadAnonymousView();
            }
        });

        lvNotes = new CharmListView<>();
        lvNotes.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.NOTES.getMenuIcon()));
        lvNotes.setCellFactory(param -> new NoteCell(service));
//        lvNotes.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                service.findSession(newValue.getSessionUuid()).ifPresent(session ->
//                        DevoxxView.SESSION.switchView().ifPresent(presenter ->
//                                ((SessionPresenter) presenter).showSession(session, Pane.NOTE)));
//            }
//        });

        noteListener = new NoteListener();
    }

    private void loadAnonymousView() {
        notesView.setCenter(new LoginPrompter(service, ANONYMOUS_MESSAGE, DevoxxView.NOTES.getMenuIcon(), this::loadAuthenticatedView));
    }

    private void loadAuthenticatedView() {
        ObservableList<Note> finalNotes = FXCollections.observableArrayList();
        ObservableList<Note> notes = service.retrieveNotes();
        for (Note note : notes) {
            service.findSession(note.getSessionUuid()).ifPresent(session -> finalNotes.add(note));
        }
        notes.addListener((ListChangeListener<Note>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Note note : c.getAddedSubList()) {
                        service.findSession(note.getSessionUuid()).ifPresent(session -> {
                            finalNotes.add(note);
                            note.contentProperty().addListener(noteListener);
                        });
                    }
                }
                if (c.wasRemoved()) {
                    for (Note note : c.getRemoved()) {
                        finalNotes.remove(note);
                        note.contentProperty().removeListener(noteListener);
                    }
                }
            }
        });

        if (!listenersAdded){
            for (Note note : finalNotes) {
                note.contentProperty().addListener(noteListener);
            }
            listenersAdded = true;
        }

        lvNotes.setItems(finalNotes);
        notesView.setCenter(lvNotes);
    }

    private class NoteListener implements ChangeListener<String> {

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            lvNotes.refresh();
        }
    }
}
