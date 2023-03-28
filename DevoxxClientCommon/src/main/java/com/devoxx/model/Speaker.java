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
package com.devoxx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.Locale;

public class Speaker extends Searchable {
    private String uuid;
    private String bio;
    private String bioAsHtml;
    private String firstName;
    private String lastName;
    private String avatarURL;
    private String company;
    private String blog;
    private String twitter;
    private String lang;
    private List<Talk> acceptedTalks;
    private BooleanProperty detailsRetrieved = new SimpleBooleanProperty();

    public Speaker() {
    }

    public Speaker(String uuid, String bio, String bioAsHtml, String firstName, String lastName, String avatarURL, String company, String blog, String twitter, String lang, List<Talk> acceptedTalks) {
        this.uuid = uuid;
        this.bio = bio;
        this.bioAsHtml = bioAsHtml;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarURL = avatarURL;
        this.company = company;
        this.blog = blog;
        this.twitter = twitter;
        this.lang = lang;
        this.acceptedTalks = acceptedTalks;
    }

    public String getUuid() {
        return uuid;
    }

    public String getBio() {
        return bio;
    }

    public String getBioAsHtml() {
        return bioAsHtml;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAvatarURL() {
        return avatarURL;
    }
    
    public String getCompany() {
        return company;
    }

    public String getBlog() {
        return blog;
    }

    public String getTwitter() {
        return twitter;
    }

    public String getLang() {
        return lang;
    }

    public List<Talk> getAcceptedTalks() {
        return acceptedTalks;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setBioAsHtml(String bioAsHtml) {
        this.bioAsHtml = bioAsHtml;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setBlog(String blog) {
        this.blog = blog;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setAcceptedTalks(List<Talk> acceptedTalks) {
        this.acceptedTalks = acceptedTalks;
    }

    public boolean isDetailsRetrieved() {
        return detailsRetrieved.get();
    }

    public BooleanProperty detailsRetrievedProperty() {
        return detailsRetrieved;
    }

    public void setDetailsRetrieved(boolean detailsRetrieved) {
        this.detailsRetrieved.set(detailsRetrieved);
    }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsKeyword(getFirstName(), lowerKeyword) ||
               containsKeyword(getLastName(), lowerKeyword)  ||
               containsKeyword(getCompany(), lowerKeyword)   ||
               containsKeyword(getSummary(), lowerKeyword);

    }

    @Override
    public String toString() {
        return "Speaker{" +
                "uuid='" + uuid + '\'' +
                ", bio='" + bio + '\'' +
                ", bioAsHtml='" + bioAsHtml + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", avatarURL='" + avatarURL + '\'' +
                ", company='" + company + '\'' +
                ", blog='" + blog + '\'' +
                ", twitter='" + twitter + '\'' +
                ", lang='" + lang + '\'' +
                ", acceptedTalks=" + acceptedTalks +
                '}';
    }

    public String getSummary() {
        if (bio != null) {
            return bio
                    .replaceAll("\\[(.*?)]\\(.*?\\)", "$1")
                    .replaceAll("\\<(/?[^\\>]+)\\>", "\\ ")
                    .replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

}