package com.jbcnconf.json;

import java.io.IOException;
import java.net.URI;
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
		sponsor.put("image", levelSponsor.getJsonObject("image"));
		sponsor.put("level", new JsonObject().put("name", levelName).put("priority", levelPriority));
		finalJson.add(sponsor);
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		new SponsorsGenerator().generate("src/main/resources/sponsors.json");		
	}
	
}
