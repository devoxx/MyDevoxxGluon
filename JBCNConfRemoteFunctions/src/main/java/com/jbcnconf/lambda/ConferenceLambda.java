package com.jbcnconf.lambda;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class ConferenceLambda implements RequestHandler<Map<String, Object>, String>{

	@Override
	public String handleRequest(Map<String, Object> input, Context context) {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resource = classLoader.getResource("conference.json");
			String jsonContent = new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
			return jsonContent;
		} catch (IOException | URISyntaxException e) {
			context.getLogger().log(e.getMessage());
			throw new RuntimeException(e);
		}
	}

}
