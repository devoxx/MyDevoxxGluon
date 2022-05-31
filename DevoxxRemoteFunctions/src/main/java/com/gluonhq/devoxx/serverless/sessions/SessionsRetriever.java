/**
 * Copyright (c) 2016, 2019 Gluon Software
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
package com.gluonhq.devoxx.serverless.sessions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import javax.json.*;
import javax.json.stream.JsonCollectors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.gluonhq.devoxx.serverless.util.ConferenceUtil.isNewCfpURL;

public class SessionsRetriever {

    private static final Logger LOGGER = Logger.getLogger(SessionsRetriever.class.getName());

    private static final Client client = ClientBuilder.newClient();
    private static JsonArray talks = null;
    private String cfpEndpoint;

    public String retrieve(String cfpEndpoint, String conferenceId) throws IOException {
        this.cfpEndpoint = cfpEndpoint;
        WebTarget target = client.target(cfpEndpoint);
        if (isNewCfpURL(cfpEndpoint)) {
            target = target.path("public").path("schedules/");
        } else {
            target = target.path("conferences").path(conferenceId).path("schedules/");
        }
        Response schedules = target.request().get();
        if (schedules.getStatus() == Response.Status.OK.getStatusCode()) {
            try (JsonReader schedulesReader = Json.createReader(new StringReader(schedules.readEntity(String.class)))) {
                List<String> dayLinks = schedulesReader.readObject().getJsonArray("links").getValuesAs(JsonObject.class).stream()
                        .filter(schedule -> schedule.containsKey("href"))
                        .map(schedule -> schedule.getString("href").replaceFirst("http://", "https://"))
                        .collect(Collectors.toList());

                JsonArrayBuilder sessions = Json.createArrayBuilder();
                for (String dayLink : dayLinks) {
                    Response slots = client.target(dayLink).request().get();
                    if (slots.getStatus() == Response.Status.OK.getStatusCode()) {
                        try (JsonReader slotsReader = Json.createReader(new StringReader(slots.readEntity(String.class)))) {
                            if (isNewCfpURL(cfpEndpoint)) {
                                LOGGER.log(Level.INFO, "New API");
                                slotsReader.readArray().getValuesAs(JsonObject.class).stream()
                                        .map(s -> updateSession(s, cfpEndpoint))
                                        .filter(s -> (s.containsKey("talk") && !s.isNull("talk")) ||
                                                s.containsKey("break") && !s.isNull("break"))
                                        .forEach(sessions::add);
                            } else {
                                LOGGER.log(Level.INFO, "Old API");
                                slotsReader.readObject().getJsonArray("slots").getValuesAs(JsonObject.class).stream()
                                        .filter(slot -> (slot.containsKey("talk") && slot.get("talk").getValueType() == JsonValue.ValueType.OBJECT) ||
                                                (slot.containsKey("break") && slot.get("break").getValueType() == JsonValue.ValueType.OBJECT))
                                        .forEach(sessions::add);
                            }
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Failed processing link " + dayLink + ": " + slots.readEntity(String.class));
                    }
                }
                return sessions.build().toString();
            }
        } else {
            throw new IOException(new WebApplicationException(schedules));
        }
    }

    private JsonObject updateSession(JsonObject source, String cfpEndpoint) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("day",             "");
        builder.add("slotId",          source.get("id"));
        builder.add("roomId",          source.getJsonObject("room").get("id"));
        builder.add("roomName",        source.getJsonObject("room").get("name"));
        builder.add("totalFavourites", source.getInt("totalFavourites", 0));
        builder.add("fromTime",        source.get("fromDate"));
        builder.add("toTime",          source.get("toDate"));
        builder.add("fromTimeMillis",  ZonedDateTime.parse(source.getString("fromDate")).toInstant().toEpochMilli());
        builder.add("toTimeMillis",    ZonedDateTime.parse(source.getString("toDate")).toInstant().toEpochMilli());

        final boolean aBreak = source.getJsonObject("sessionType").getBoolean("isPause", false);
        builder.add("break", aBreak ? fetchBreak(source) : JsonValue.NULL);

        if (source.containsKey("proposal")) {
            if (source.getValue("/proposal").getValueType().equals(JsonValue.ValueType.OBJECT)) {
                JsonObject propopal = source.getJsonObject("proposal");
                final int talkId = propopal.getInt("id", 0);
                if (talkId != 0) {
                    JsonObject talk = createTalk(propopal);
                    builder.add("talk", talk == JsonValue.EMPTY_JSON_OBJECT ? JsonValue.NULL : talk);
                }
            }
        }
        return builder.build();
    }

    private JsonObject fetchBreak(JsonObject source) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObject sessionType = source.getJsonObject("sessionType");
        builder.add("id",        String.valueOf(sessionType.get("id")));
        builder.add("nameEN",    sessionType.get("name"));
        builder.add("nameFR",    sessionType.get("name"));
        builder.add("room",      source.isNull("room") ? null : source.getJsonObject("room").getString("name"));
        builder.add("dayName",   ZonedDateTime.parse(source.getString("fromDate")).getDayOfWeek().toString());
        builder.add("startTime", ZonedDateTime.parse(source.getString("fromDate")).toInstant().toEpochMilli());
        builder.add("endTime",   ZonedDateTime.parse(source.getString("toDate")).toInstant().toEpochMilli());
        return builder.build();
    }

    private JsonObject createTalk(JsonObject proposal) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("id",            proposal.get("id"));
        builder.add("title",         proposal.get("title"));
        builder.add("talkType",      proposal.getJsonObject("sessionType").getString("name"));
        builder.add("track",         proposal.getJsonObject("track").getString("name"));
        builder.add("trackId",       proposal.getJsonObject("track").get("id"));
        builder.add("lang",          proposal.getString("langName", ""));
        builder.add("audienceLevel", proposal.get("audienceLevel"));
        builder.add("summary",       removeHTMLTags(proposal.getString("description")));
        builder.add("summaryAsHtml", proposal.get("description"));
        builder.add("tags",
                proposal.containsKey("tags") ? updateTags(proposal.getJsonArray("tags")) : emptyJsonArray());
        builder.add("speakers",
                proposal.containsKey("speakers") ? updateSpeakers(proposal.getJsonArray("speakers")) : emptyJsonArray());
        return builder.build();
    }

    private String removeHTMLTags(String talkDescription) {
        Document dirty = Jsoup.parse(talkDescription);
        Cleaner cleaner = new Cleaner(Whitelist.none());
        Document clean = cleaner.clean(dirty);
        return clean.body().text();
    }

    private JsonArray updateTags(JsonArray tags) {
        return tags.getValuesAs(JsonObject.class).stream()
                .map(s -> Json.createObjectBuilder().add("value", s.get("name")).build())
                .collect(JsonCollectors.toJsonArray());
    }

    private JsonArray updateSpeakers(JsonArray speakers) {
        return speakers.getValuesAs(JsonObject.class).stream()
                .map(s -> updateSpeaker(s))
                .collect(JsonCollectors.toJsonArray());
    }

    private JsonObject updateSpeaker(JsonObject source) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        final int id = source.getInt("id");
        final String name = source.getString("firstName") + 
                            " " + 
                            source.getString("lastName");
        Map<String, Object> link = new HashMap<>();
        link.put("href",  cfpEndpoint + "public" + "/speakers/" + id);
        link.put("rel",   cfpEndpoint + "public" + "/speakers/");
        link.put("uuid",  id);
        link.put("title", name);

        builder.add("name", name);
        builder.add("link", Json.createObjectBuilder(link));
        return builder.build();
    }

    private JsonArray emptyJsonArray() {
        return Json.createArrayBuilder().build();
    }
}
