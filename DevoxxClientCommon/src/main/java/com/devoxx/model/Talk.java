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
package com.devoxx.model;

import java.util.List;

public class Talk {

    private String id;
    private String title;
    private String talkType;
    private String track;
    private String trackId;
    private String lang;
    private String audienceLevel;
    private String summary;
    private String summaryAsHtml;
    private String requirements;
    private List<Tag> tags;
    private List<TalkSpeaker> speakers;

    public Talk() {}

    public Talk(String id, String title, String talkType, String track, String lang, String audienceLevel, String summary, String summaryAsHtml, List<Tag> tags, List<TalkSpeaker> speakers) {
        this.id = id;
        this.title = title;
        this.talkType = talkType;
        this.track = track;
        this.lang = lang;
        this.audienceLevel = audienceLevel;
        this.summary = summary;
        this.summaryAsHtml = summaryAsHtml;
        this.tags = tags;
        this.speakers = speakers;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTalkType() {
        return talkType;
    }

    public String getTrack() {
        return track;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getLang() {
        return lang;
    }

    public String getAudienceLevel() {
        return audienceLevel;
    }

    public String getSummary() {
        return summary;
    }

    public String getSummaryAsHtml() {
        return summaryAsHtml;
    }

    public String getRequirements() { return requirements; }

    public List<Tag> getTags() {
        return tags;
    }

    public List<TalkSpeaker> getSpeakers() {
        return speakers;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param talkType the talkType to set
     */
    public void setTalkType(String talkType) {
        this.talkType = talkType;
    }

    /**
     * @param track the track to set
     */
    public void setTrack(String track) {
        this.track = track != null ? track.replaceAll("&amp;", "&") : null;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    /**
     * @param lang the lang to set
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * @param audienceLevel the audience level to set
     */
    public void setAudienceLevel(String audienceLevel) {
        this.audienceLevel = audienceLevel;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * @param summaryAsHtml the summaryAsHtml to set
     */
    public void setSummaryAsHtml(String summaryAsHtml) {
        this.summaryAsHtml = summaryAsHtml;
    }

    /**
     *
     * @param requirements url link
     */
    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    /**
     * @param speakers the speakers to set
     */
    public void setSpeakers(List<TalkSpeaker> speakers) {
        this.speakers = speakers;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final String otherId = ((Talk) obj).id;
        return id == otherId || (id != null && id.equals(otherId));
    }
    
}
