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
package com.devoxx.util;

import com.devoxx.service.Service;
import com.devoxx.model.*;
import com.devoxx.views.cell.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class DevoxxSearch {
    
    public static final Map<String, Class<?>> CELL_MAP = new HashMap<>();
    static {
        CELL_MAP.put(Exhibitor.class.getSimpleName(),   ExhibitorCell.class);
        CELL_MAP.put(Note.class.getSimpleName(),        NoteCell.class);
        CELL_MAP.put(Badge.class.getSimpleName(),       BadgeCell.class);
        CELL_MAP.put(Session.class.getSimpleName(),     ScheduleCell.class);
        CELL_MAP.put(Speaker.class.getSimpleName(),     SpeakerCell.class);
//        CELL_MAP.put(Sponsor.class.getSimpleName(),     SponsorCell.class);
    }

    @Inject
    private Service service;

    private final Collection<Supplier<Collection<? extends Searchable>>> searchables = Arrays.asList(
            () -> service.retrieveExhibitors(),
            () -> service.retrieveSessions(),
            () -> service.retrieveSpeakers()
//            () -> service.retrieveSponsors()
    );

    private final Collection<Supplier<Collection<? extends Searchable>>> authSearchables = Arrays.asList(
            () -> service.retrieveNotes()
//            () -> service.retrieveBadges()
    );


    /**
     * Returns a list of items based on a contains term
     * @param term term to search for
     * @return list of item containing term
     */
    public ObservableList<Searchable> search(String term) {

        ObservableList<Searchable> results = FXCollections.observableArrayList(find(term, searchables));
        if (service.isAuthenticated()) {
            results.addAll(find(term, authSearchables));
        }
        return results;
    }
    
    /**
     * Returns a list of items based on a contains term, from a list of items
     * @param keyword term to search for
     * @param previousSearch previous search results
     * @return 
     */
    public ObservableList<Searchable> refineSearch(String keyword, ObservableList<Searchable> previousSearch) {

        ObservableList<Searchable> results = FXCollections.observableArrayList();
        for (Searchable searchable : previousSearch) {
            if (searchable.contains(keyword)) {
                results.add(searchable);
            }
        }
        return results;
    }

    private Collection<Searchable> find( String keyword, Collection<Supplier<Collection<? extends Searchable>>> searchables ) {
        ObservableList<Searchable> results = FXCollections.observableArrayList();
        for (Supplier<Collection<? extends Searchable>> supplier: searchables) {
            for (Searchable searchable: supplier.get()) {
                if (searchable.contains(keyword)) {
                    results.add(searchable);
                }
            }
        }
        return results;
    }
}
