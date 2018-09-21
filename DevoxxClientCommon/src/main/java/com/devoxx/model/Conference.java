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
package com.devoxx.model;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;

public class Conference {

    private static final Logger LOG = Logger.getLogger(Conference.class.getName());

    private static final ZoneId DEFAULT_CONFERENCE_ZONE_ID = ZoneId.of("Europe/Brussels");

    private String id;
    private String name;
    private String website;
    private String description;
    private String imageURL;
    private String scheduleURL;
    private String eventImagesURL;
    private String youTubeURL;
    private String fromDate;
    private String endDate;
    private ZonedDateTime fromDateTime;
    private ZonedDateTime endDateTime;
    private ZonedDateTime[] days;
    private String cfpFromDate;
    private String cfpEndDate;
    private Type eventType;
    private String cfpURL;
    private String cfpVersion;
    private boolean archived;
    private boolean cfpActive;
    private String importDate;
    private String scheduleDate;
    private long locationId;
    private String locationName;
    private String timezone;
    private String cfpAdminEmail;
    private String fromEmail;
    private String committeeEmail;
    private String bccEmail;
    private String bugReportEmail;
    private String githubClientId;
    private String githubSecret;
    private String googleClientId;
    private String googleSecret;
    private String linkedInClientId;
    private String linkedInSecret;
    private String maxProposals;
    List<Owner> owners;
    List<Track> tracks;
    List<SessionType> sessionTypes;
    List<String> languages;
    List<Floor> floorPlans;
    
    // TODO: Needed for WearService
    private boolean selected;

    private ZoneId timezoneId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getScheduleURL() {
        return scheduleURL;
    }

    public void setScheduleURL(String scheduleURL) {
        this.scheduleURL = scheduleURL;
    }

    public String getEventImagesURL() {
        return eventImagesURL;
    }

    public void setEventImagesURL(String eventImagesURL) {
        this.eventImagesURL = eventImagesURL;
    }

    public String getYouTubeURL() {
        return youTubeURL;
    }

    public void setYouTubeURL(String youTubeURL) {
        this.youTubeURL = youTubeURL;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;

        if (this.fromDate != null && this.endDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;

        if (this.fromDate != null && this.endDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    public String getCfpFromDate() {
        return cfpFromDate;
    }

    public void setCfpFromDate(String cfpFromDate) {
        this.cfpFromDate = cfpFromDate;
    }

    public String getCfpEndDate() {
        return cfpEndDate;
    }

    public void setCfpEndDate(String cfpEndDate) {
        this.cfpEndDate = cfpEndDate;
    }

    public Type getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = Type.valueOf(eventType);
    }

    public String getCfpURL() {
        return cfpURL;
    }

    public void setCfpURL(String cfpURL) {
        this.cfpURL = cfpURL;
    }

    public String getCfpVersion() {
        return cfpVersion;
    }

    public void setCfpVersion(String cfpVersion) {
        this.cfpVersion = cfpVersion;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isCfpActive() {
        return cfpActive;
    }

    public void setCfpActive(Boolean cfpActive) {
        this.cfpActive = cfpActive == null ? false : cfpActive;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        try {
            this.timezoneId = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            LOG.log(Level.WARNING, "Failed to convert timezone: " + timezone + ", using default timezone (Europe/Brussels) instead.");
            this.timezoneId = DEFAULT_CONFERENCE_ZONE_ID;
        }

        if (this.fromDate != null && this.endDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    public String getCfpAdminEmail() {
        return cfpAdminEmail;
    }

    public void setCfpAdminEmail(String cfpAdminEmail) {
        this.cfpAdminEmail = cfpAdminEmail;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getCommitteeEmail() {
        return committeeEmail;
    }

    public void setCommitteeEmail(String committeeEmail) {
        this.committeeEmail = committeeEmail;
    }

    public String getBccEmail() {
        return bccEmail;
    }

    public void setBccEmail(String bccEmail) {
        this.bccEmail = bccEmail;
    }

    public String getBugReportEmail() {
        return bugReportEmail;
    }

    public void setBugReportEmail(String bugReportEmail) {
        this.bugReportEmail = bugReportEmail;
    }

    public String getGithubClientId() {
        return githubClientId;
    }

    public void setGithubClientId(String githubClientId) {
        this.githubClientId = githubClientId;
    }

    public String getGithubSecret() {
        return githubSecret;
    }

    public void setGithubSecret(String githubSecret) {
        this.githubSecret = githubSecret;
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public String getGoogleSecret() {
        return googleSecret;
    }

    public void setGoogleSecret(String googleSecret) {
        this.googleSecret = googleSecret;
    }

    public String getLinkedInClientId() {
        return linkedInClientId;
    }

    public void setLinkedInClientId(String linkedInClientId) {
        this.linkedInClientId = linkedInClientId;
    }

    public String getLinkedInSecret() {
        return linkedInSecret;
    }

    public void setLinkedInSecret(String linkedInSecret) {
        this.linkedInSecret = linkedInSecret;
    }

    public String getMaxProposals() {
        return maxProposals;
    }

    public void setMaxProposals(String maxProposals) {
        this.maxProposals = maxProposals;
    }

    public List<Owner> getOwners() {
        return owners;
    }

    public void setOwners(List<Owner> owners) {
        this.owners = owners;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public List<SessionType> getSessionTypes() {
        return sessionTypes;
    }

    public void setSessionTypes(List<SessionType> sessionTypes) {
        this.sessionTypes = sessionTypes;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<Floor> getFloorPlans() {
        return floorPlans;
    }

    public void setFloorPlans(List<Floor> floorPlans) {
        this.floorPlans = floorPlans;
    }

    public ZonedDateTime getFromDateTime() {
        return fromDateTime;
    }

    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    public ZoneId getConferenceZoneId() {
        return timezoneId;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private void calculateConferenceDays() {
        this.fromDateTime = LocalDate.parse(fromDate, DATE_FORMATTER).atStartOfDay(timezoneId);
        this.endDateTime = LocalDate.parse(endDate, DATE_FORMATTER).atStartOfDay(timezoneId);
        long numberOfDays = DAYS.between(fromDateTime, endDateTime) + 1;
        days = new ZonedDateTime[(int) numberOfDays];
        days[0] = dayOnly(fromDateTime, timezoneId);
        for (int day = 1; day < numberOfDays; day++) {
            days[day] = days[0].plusDays(day);
        }
    }

    public ZonedDateTime[] getDays() {
        return days;
    }

    private static ZonedDateTime dayOnly(ZonedDateTime dateTime, ZoneId zoneId) {
        return ZonedDateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), 0, 0, 0, 0, zoneId);
    }

    public int getConferenceDayIndex(ZonedDateTime date) {
        return Arrays.binarySearch(days, dayOnly(date, getConferenceZoneId())) + 1;
    }

    public long getDaysUntilStart() {
        LocalDate today = LocalDate.now(getConferenceZoneId());
        return today.until(getFromDateTime(), ChronoUnit.DAYS);
    }

    public long getDaysUntilEnd() {
        LocalDate today = LocalDate.now(getConferenceZoneId());
        return today.until(getEndDateTime(), ChronoUnit.DAYS);
    }

    public String getCountry() {
        String[] split = name.split(" ");
        switch (getEventType()) {
            case DEVOXX:
                // Devoxx Belgium 2018
                // Devoxx UK 2018
                return split.length >= 1 ? split[1] : "";
            case VOXXED:
                // Voxxed Days Ticino 2018
                // VoxxedDays Microservices 2018
                return split.length >= 2 ? split[split.length - 2] : "";
        }
        return "";
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "Conference{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
    
    public enum Type {
        DEVOXX("Devoxx", "Devoxx"),
        VOXXED("Voxxed", "VoxxedDays");

        private String name;
        private String displayName;

        Type(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
