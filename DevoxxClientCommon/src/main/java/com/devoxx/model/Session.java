/**
 * Copyright (c) 2016, 2017, Gluon Software
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

import javax.xml.bind.annotation.XmlTransient;
import java.time.ZonedDateTime;
import java.util.Locale;

public class Session extends Searchable {

    private String slotId;
    private String roomId;
    private String roomName;
    private String day;
    private String fromTime;
    private long fromTimeMillis;
    private ZonedDateTime startDate;
    private String toTime;
    private long toTimeMillis;
    private ZonedDateTime endDate;
    private Break aBreak;
    private Talk talk;
    private boolean decorated;
    private boolean showSessionType;

    public Session() {}

    public Session(String slotId, String roomId, String roomName, String day, String fromTime, long fromTimeMillis, String toTime, long toTimeMillis, Break aBreak, Talk talk) {
        this.slotId = slotId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.day = day;
        this.fromTime = fromTime;
        this.fromTimeMillis = fromTimeMillis;
        this.toTime = toTime;
        this.toTimeMillis = toTimeMillis;
        this.aBreak = aBreak;
        this.talk = talk;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getFromTime() {
        return fromTime;
    }

    public void setFromTime(String fromTime) {
        this.fromTime = fromTime;
    }

    public long getFromTimeMillis() {
        return fromTimeMillis;
    }

    public void setFromTimeMillis(long fromTimeMillis) {
        this.fromTimeMillis = fromTimeMillis;
    }

    @XmlTransient
    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public long getToTimeMillis() {
        return toTimeMillis;
    }

    public void setToTimeMillis(long toTimeMillis) {
        this.toTimeMillis = toTimeMillis;
    }

    @XmlTransient
    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public Break getBreak() {
        return aBreak;
    }

    public void setBreak(Break aBreak) {
        this.aBreak = aBreak;
    }

    public Talk getTalk() {
        return talk;
    }

    public void setTalk(Talk talk) {
        this.talk = talk;
    }

    public boolean isDecorated() {
        return decorated;
    }

    public void setDecorated(boolean decorated) {
        this.decorated = decorated;
    }

    public boolean isShowSessionType() {
        return showSessionType;
    }

    public void setShowSessionType(boolean showSessionType) {
        this.showSessionType = showSessionType;
    }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        } 
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsKeyword(getTitle(), lowerKeyword) ||
               containsKeyword(getRoomName(), lowerKeyword);
    }

    public String getTitle() {
        if (talk != null) return talk.getTitle();
        return null;
    }

    public String getSummary() {
        if (talk != null) return talk.getSummary();
        return null;
    }

    public String getRequirements()
    {
        if (talk !=null) return talk.getRequirements();
        return null;
    }

    public String getLocation() {
        return getRoomName();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (talk != null ? talk.hashCode() : 0);
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
        final Talk otherTalk = ((Session) obj).talk;
        return  (talk != null && talk.equals(otherTalk)) ||
                (aBreak != null && aBreak.equals(((Session) obj).aBreak));
    }

}
