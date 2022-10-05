/*
 * Copyright (c) 2016, 2022, Gluon Software
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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionsLambda implements RequestStreamHandler {

    private static final Logger LOGGER = Logger.getLogger(SessionsLambda.class.getName());

    private static final String CONFERENCE_ID_OLD = "\"65\"";
    private static final String CFP_ENDPOINT_OLD = "https://vxdbanff19.confinabox.com/api";
    private static final String CFP_ENDPOINT_NEW = "https://dvbe22.cfp.dev/api/";

    private static long timestamp;

    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String conferenceId, cfpEndpoint;
        timestamp = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "handleRequest START");
        try (JsonReader reader = Json.createReader(input)) {
            JsonObject jsonInput = reader.readObject();
            conferenceId = jsonInput.containsKey("conferenceId") && !jsonInput.isNull("conferenceId") ? 
                    jsonInput.getString("conferenceId") : null;
            cfpEndpoint = jsonInput.getString("cfpEndpoint");
            LOGGER.log(Level.INFO, "call SessionsRetriever, after " + (System.currentTimeMillis() - timestamp) + " ms.");
            String jsonOutput = new SessionsRetriever().retrieve(cfpEndpoint, conferenceId);
            LOGGER.log(Level.INFO, "call OutputStreamWriter, after " + (System.currentTimeMillis() - timestamp) + " ms.");
            try (Writer writer = new OutputStreamWriter(output)) {
                writer.write(jsonOutput);
            }
        }
        LOGGER.log(Level.INFO, "handleRequest END. Took: " + (System.currentTimeMillis() - timestamp) + " ms.");
    }

    public static void main(String[] args) throws IOException {
        final String json = String.format("{\"cfpEndpoint\":\"%s\", \"conferenceId\":%s}", CFP_ENDPOINT_NEW, null); // Replace with CFP_ENDPOINT_NEW and null 
        InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new SessionsLambda().handleRequest(input, output, null);
        System.out.println("output = " + new String(output.toByteArray(), StandardCharsets.UTF_8));
    }
}
