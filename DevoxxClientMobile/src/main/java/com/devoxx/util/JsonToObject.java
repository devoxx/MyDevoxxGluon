/*
 * Copyright (c) 2019, Gluon Software
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

import com.devoxx.model.Speaker;
import com.devoxx.model.Talk;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

public class JsonToObject {
    
    public static List<Speaker> toSpeakers(List<JsonObject> source) {
        List<Speaker> list = new ArrayList<>();
        for (JsonObject s : source) {
            Speaker speaker = toSpeaker(s);
            list.add(speaker);
        }
        return list;
    }
    
    public static Speaker toSpeaker(JsonObject source) {
        Speaker speaker = new Speaker();
        speaker.setUuid(String.valueOf(source.getInt("id")));
        speaker.setBio(getNullable(source,"bio"));
        speaker.setFirstName(getNullable(source,"firstName"));
        speaker.setLastName(getNullable(source,"lastName"));
        speaker.setAvatarURL(getNullable(source,"imageUrl"));
        speaker.setCompany(getNullable(source,"company"));
        speaker.setTwitter(getNullable(source, "twitterHandle"));
        speaker.setAcceptedTalks(toTalks(source.get("proposals")));
        return speaker;
    }

    private static List<Talk> toTalks(JsonValue proposals) {
        if (proposals == null || proposals.getValueType() == JsonValue.ValueType.NULL) return null;

        List<Talk> talks = new ArrayList<>();
        final List<JsonObject> values = ((JsonArray) proposals).getValuesAs(JsonObject.class);
        for (JsonObject jsonObject : values) {
            final Talk talk = toTalk(jsonObject);
            talks.add(talk);
        }
        return talks;
    }

    private static Talk toTalk(JsonObject src) {
        final Talk talk = new Talk();
        talk.setId(getNullableInt(src,"id"));
        talk.setTitle(getNullable(src, "title"));
        talk.setSummary(getNullable(src, "description"));
        talk.setTrackId(getNullableInt(src,"trackId"));
        talk.setTrack(getNullable(src, "trackName"));
        talk.setTalkType(getNullable(src, "sessionTypeName"));
        return talk;
    }

    private static String getNullable(JsonObject source, String key) {
        return source.get(key) == null || source.isNull(key) ? "" : source.getString(key);
    }

    private static String getNullableInt(JsonObject source, String key) {
        return source.getJsonNumber(key) == null ? null : String.valueOf(source.getInt(key));
    }
}
