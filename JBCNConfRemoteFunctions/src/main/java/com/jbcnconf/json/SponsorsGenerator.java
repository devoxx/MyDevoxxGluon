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
package com.jbcnconf.json;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SponsorsGenerator {

	public void generate(String jsonPath) throws IOException, URISyntaxException {
		String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
		JsonObject json = new JsonObject(jsonContent);
		JsonArray finalJson = new JsonArray();
		
		importSponsors(json, finalJson, "diamond", 1, "cosmos");
		importSponsors(json, finalJson, "cosmos", 1);
		importSponsors(json, finalJson, "galaxy-premium", 2);
		importSponsors(json, finalJson, "galaxy-regular", 3);
		importSponsors(json, finalJson, "galaxy-basic", 4);
		importSponsors(json, finalJson, "stars", 5);
		
		// write finalJson to file
		System.out.println(finalJson.encodePrettily());
		
	}

	private void importSponsors(JsonObject json, JsonArray finalJson, String levelName, int levelPriority) {
		importSponsors(json, finalJson, levelName, levelPriority, levelName);
	}

	private void importSponsors(JsonObject json, JsonArray finalJson, String levelName, int levelPriority, String finalLevelName) {
		if (json.containsKey(levelName)) {
			Object levelSponsors = json.getValue(levelName);
			if (levelSponsors instanceof JsonObject) {
				importSponsor((JsonObject) levelSponsors, finalJson, finalLevelName, levelPriority);
			}
			if (levelSponsors instanceof JsonArray) {
				importSponsorArray((JsonArray) levelSponsors, finalJson, finalLevelName, levelPriority);
			}
		}
	}

	private void importSponsorArray(JsonArray levelSponsors, JsonArray finalJson, String levelName, int levelPriority) {
		for (int i=0; i<levelSponsors.size(); i++) {
			importSponsor(levelSponsors.getJsonObject(i), finalJson, levelName, levelPriority);
		}
	}

	private void importSponsor(JsonObject levelSponsor, JsonArray finalJson, String levelName, int levelPriority) {
		JsonObject sponsor = new JsonObject();
		sponsor.put("id", (finalJson.size()+1) + "");
		sponsor.put("name", levelSponsor.getString("name"));
		sponsor.put("href", levelSponsor.getString("href"));
		JsonObject image = levelSponsor.getJsonObject("image");
		image.put("src", "https://www.jbcnconf.com/2019" + image.getString("src"));
		sponsor.put("image", image);
		sponsor.put("level", new JsonObject().put("name", levelName).put("priority", levelPriority));
		finalJson.add(sponsor);
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		new SponsorsGenerator().generate("src/main/resources/sponsors.json");		
	}
	
}
