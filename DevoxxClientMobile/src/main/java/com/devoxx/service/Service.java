/*
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
package com.devoxx.service;

import com.devoxx.model.Badge;
import com.devoxx.model.Conference;
import com.devoxx.model.Exhibitor;
import com.devoxx.model.Favorite;
import com.devoxx.model.Feedback;
import com.devoxx.model.Floor;
import com.devoxx.model.Note;
import com.devoxx.model.ProposalType;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.model.Sponsor;
import com.devoxx.model.SponsorBadge;
import com.devoxx.model.Track;
import com.devoxx.model.Vote;
import com.devoxx.model.*;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.gluonhq.cloudlink.client.user.User;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * A service class that provides the data from a back end system. Some methods require an authenticated
 * user to be available.
 */
@Singleton
public interface Service {

    /**
     * Returns a list of past conferences.
     *
     * @return List of Devoxx and Voxxed conferences
     */
    GluonObservableList<Conference> retrievePastConferences();

    /**
     * Returns a list of conferences.
     * 
     * @param type whether Devoxx or Voxxed
     * @return List of Devoxx or Voxxed conferences
     */
    GluonObservableList<Conference> retrieveConferences(Conference.Type type);

    /**
     * Returns details about a conference.
     *
     * @return A conference object
     */
    GluonObservableObject<Conference> retrieveConference(String conferenceId);

    /**
     * Sets the selected conference.
     * @param selectedItem the selected conference
     */
    void setConference(Conference selectedItem);

    /**
     * Gets the selected conference.
     * @return the selected conference
     */
    Conference getConference();

    ReadOnlyObjectProperty<Conference> conferenceProperty();

    /**
     * Execute a reload of sessions and speakers when a reload is requested.
     */
    void checkIfReloadRequested();

    /**
     * Returns true if a rating dialog has been requested from the GCL.
     */
    boolean showRatingDialog();

    /**
     * Returns a list of sessions at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<Session> retrieveSessions();

    /**
     * Returns a list of speakers at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<Speaker> retrieveSpeakers();

    /**
     * Returns a speaker detail for the specified UUID
     *
     * @param uuid UUID of the Speaker
     * @return
     */
    ReadOnlyObjectProperty<Speaker> retrieveSpeaker(String uuid);

    /**
     * Returns a list of tracks at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<Track> retrieveTracks();

    /**
     * Returns a list of tracks at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<ProposalType> retrieveProposalTypes();

    /**
     * Returns a list of exhibitors at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<Exhibitor> retrieveExhibitors();

    /**
     * Returns a list of exhibition maps at the conference.
     *
     * @return
     */
    ReadOnlyListProperty<Floor> retrieveExhibitionMaps();

    /**
     * Returns a list of sponsors of the conference.
     *
     * @return
     */
    GluonObservableList<Sponsor> retrieveSponsors();

    /**
     * Starts the authentication process. This will show a View in which the user can authenticate
     * himself. When the authentication process completed successfully, the run method of the provided
     * <code>successRunnable</code> will be called.
     *
     */
    void authenticate(Runnable successRunnable);

    /**
     * Starts the authentication process. This will show a View in which the user can authenticate
     * himself. When the authentication process completed successfully, the run method of the provided
     * <code>successRunnable</code> will be called. If the authentication process fails, or was
     * aborted by the user, the run method of the provided <code>failureRunnable</code> will be called
     * instead.
     *
     * @return <code>true</code> if the user is authenticated at the end of this method
     */
    void authenticate(Runnable successRunnable, Runnable failureRunnable);

    /**
     * Returns a boolean indicating whether there is an authenticated user or not.
     *
     * @return true if there is an authenticated user, false otherwise.
     */
    boolean isAuthenticated();
    
    /**
     * The ready property can be set to true when the service has finished loading some resources.
     * This can be used by the wearable service to add listeners and perform some actions
     * @return a boolean property
     */
    BooleanProperty readyProperty();

    /**
     * Starts the log out process. This will show a Dialog in which the user can confirm
     * he wants to log out. When the log out process completed successfully, 
     * <code>true</code> will be returned.
     *
     * @return <code>true</code> if the user is logged out at the end of this method
     */
    boolean logOut();

    /**
     * Returns a list of sessions that the authenticated user marked as favorite.
     *
     * @return
     * @throws IllegalStateException when no user is currently authenticated
     */
    ObservableList<Session> retrieveFavoredSessions();

    /**
     * Returns a list of notes that the authenticated user has written. Each note belongs to a specific session.
     *
     * @return
     * @throws IllegalStateException when no user is currently authenticated
     */
    ObservableList<Note> retrieveNotes();

    /**
     * Returns a list of badges that the authenticated user has scanned. 
     *
     * @return
     * @throws IllegalStateException when no user is currently authenticated
     */
    ObservableList<Badge> retrieveBadges();

    /**
     * Returns a list of badges that the authenticated sponsor has scanned. 
     *
     * @return An observable list of badges scanned and stored by the sponsor.
     * @throws IllegalStateException when no user is currently authenticated
     * @param sponsor The sponsor for which badges are to be retrieved
     */
    ObservableList<SponsorBadge> retrieveSponsorBadges(Sponsor sponsor);

    /**
     * Returns a list of favored or scheduled sessions from the cloud.
     * @param sessionListType Type of session
     * @return A list of favored or scheduled session.
     */
    ObservableList<Session> reloadSessionsFromCFP(SessionListType sessionListType);

    /**
     * Retrieves the Session for a specific session uuid.
     * @param uuid The uuid assigned to the session
     * @return Session with the specific uuid
     */
    Optional<Session> findSession(String uuid);

    void voteTalk(Vote vote);

    /**
     * Retrieves the list of favorites for the selected conference.
     * @return An ObservableList of favorites
     */
    ObservableList<Favorite> retrieveFavorites();

    /**
     * Updates the list of favorites from the data source.
     */
    void refreshFavorites();

    /**
     * Fetches the password for the sponsor
     * @return The GluonObservableObject with response containing password
     */
    GluonObservableObject<String> authenticateSponsor();

    /**
     * Sends the sponsor badge to a GCL remote function
     * @param sponsorBadge The sponsor badge to be send to the GCL remote function.
     */
    void saveSponsorBadge(SponsorBadge sponsorBadge);

    /**
     * Returns the authenticated user, if present. Otherwise, returns null.
     * @return The authenticated user, if present.
     */
    User getAuthenticatedUser();

    /**
     * Submits the user feedback
     */
    void sendFeedback(Feedback feedback);

    /**
     * Fetches the details about the venue of the conference
     * @return Location of the selected conference
     */
    GluonObservableObject<Location> retrieveLocation();
}
