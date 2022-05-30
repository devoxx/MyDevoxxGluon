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
package com.gluonhq.devoxx.serverless.util;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonCollectors;

public class ConferenceUtil {
    
    public static JsonObject createCleanResponseForClientFromNewEndpoint(JsonObject conf, JsonArray floorPlans) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("id",               conf.getInt("id", 0));
        builder.add("name",             conf.getString("name", ""));
        builder.add("website",          conf.getString("website", ""));
        builder.add("description",      conf.getString("description", ""));
        builder.add("imageURL",         conf.getString("imageURL", ""));
        builder.add("scheduleURL",      conf.getString("scheduleURL", ""));
        builder.add("eventImagesURL",   conf.getString("eventImageURL", ""));
        builder.add("youTubeURL",       conf.getString("youTubeURL", ""));
        builder.add("fromDate",         conf.getString("fromDate").substring(0, 10));
        builder.add("endDate",          conf.getString("toDate").substring(0, 10));
        builder.add("fromDateTime",     conf.getString("fromDate"));
        builder.add("endDateTime",      conf.getString("toDate"));
        if (conf.containsKey("days")) {
            builder.add("days", conf.getJsonArray("days"));
        }
        builder.add("cfpFromDate",      conf.getString("cfpOpening", ""));
        builder.add("cfpEndDate",       conf.getString("cfpClosing", ""));
        builder.add("eventType",        conf.getString("theme", ""));
        builder.add("cfpURL",           conf.getString("apiURL", ""));
        builder.add("cfpVersion",       conf.getString("cfpVersion", ""));
        builder.add("archived",         conf.getBoolean("archived", true));
        builder.add("cfpActive",        conf.getBoolean("live", false));
        builder.add("locationId",       conf.getInt("locationId", 0));
        builder.add("locationName",     conf.getString("locationName", ""));
        builder.add("locationAddress",  conf.getString("locationAddress", ""));
        builder.add("locationCity",     conf.getString("locationCity", ""));
        builder.add("locationCountry",  conf.getString("locationCountry", ""));
        builder.add("timezone",         conf.getString("timezone", ""));
        if (conf.get("venueLongitude") instanceof JsonNumber) {
            builder.add("venueLongitude", conf.getJsonNumber("venueLongitude"));
        }
        if (conf.get("venueLatitude") instanceof JsonNumber) {
            builder.add("venueLatitude", conf.getJsonNumber("venueLatitude"));
        }
        builder.add("cfpAdminEmail",    conf.getString("cfpAdminEmail", ""));
        builder.add("maxProposals",     conf.getString("maxProposals", ""));
        builder.add("myBadgeActive",    conf.getBoolean("myBadgeActive", false));
        builder.add("owners",
                conf.containsKey("owners") ? conf.getJsonArray("owners") : emptyJsonArray());
        builder.add("tracks",
                conf.containsKey("tracks") ? conf.getJsonArray("tracks") : emptyJsonArray());
        builder.add("sessionTypes",
                conf.containsKey("sessionTypes") ? getSessionTypes(conf) : emptyJsonArray());
        builder.add("languages",
                conf.containsKey("languages") ? conf.getJsonArray("languages") : emptyJsonArray());
        builder.add("floorPlans", floorPlans);
        return builder.build();
    }

    private static JsonArray getSessionTypes(JsonObject conf) {
        final JsonArray sessionTypes = conf.getJsonArray("sessionTypes");
        return sessionTypes.getValuesAs(JsonObject.class)
                .stream()
                .map(st -> enrich(st, "isPause", "pause", st.get("isPause")))
                .collect(JsonCollectors.toJsonArray());
    }

    private static JsonArray emptyJsonArray() {
        return Json.createArrayBuilder().build();
    }

    public static JsonObject createCleanResponseForClientFromOldEndpoint(JsonObject conf) {
        if (conf.containsKey("fromDate")) {
            String fromDate = conf.getString("fromDate");
            conf = enrich(conf, "fromDate", "fromDate", Json.createValue(fromDate.substring(0, 10)));
        }
        if (conf.containsKey("toDate")) {
            String toDate = conf.getString("toDate");
            conf = enrich(conf, "toDate", "endDate", Json.createValue(toDate.substring(0, 10)));
        }
        if (conf.containsKey("apiURL")) {
            conf = enrich(conf, "apiURL", "cfpURL", conf.get("apiURL"));
        }
        if (conf.containsKey("eventCategory")) {
            conf = enrich(conf, "eventCategory", "eventType", conf.get("eventCategory"));
        }
        if (conf.containsKey("id")) {
            conf = enrich(conf, null, "cfpVersion", conf.get("id"));
        }
        return conf;
    }

    /**
     * Replaces an entry of key and value in the json source
     * with the user specified key and value.
     * @param source json object in which replacement is to be done
     * @param sourceKey key corresponding to the key and value to be replaced
     * @param key new key for replacement
     * @param value new value for replacement
     * @return JsonObject with updated entries
     */
    private static JsonObject enrich(JsonObject source, String sourceKey, String key, JsonValue value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        source.entrySet().stream().filter(entry -> !entry.getKey().equals(sourceKey)).forEach(entry -> builder.add(entry.getKey(), entry.getValue()));
        builder.add(key, value);
        return builder.build();
    }
    
    public static boolean isNewCfpURL(String cfp) {
        return cfp.matches(".+?(?=.cfp.dev/api)(.*)");
    }
}
