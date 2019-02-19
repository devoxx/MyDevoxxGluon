package com.jbcnconf.lambda;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MockResponseRetriever {

	/**
	 * Retrieves mock response for the passed key
	 * @param key
	 * @return
	 */
	public static String getMockResponseFor(String key) {
		try {
			ClassLoader classLoader = MockResponseRetriever.class.getClassLoader();
			URL resource = classLoader.getResource(key + ".json");
			String jsonContent = new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
			return jsonContent;
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		
	}
	
}
