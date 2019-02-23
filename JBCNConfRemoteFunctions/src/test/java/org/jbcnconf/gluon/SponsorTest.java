package org.jbcnconf.gluon;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.devoxx.model.Image;
import com.devoxx.model.Level;
import com.devoxx.model.Sponsor;

import io.vertx.core.json.JsonArray;


public class SponsorTest {

	@Test
	public void sponsor_csv() {
		Sponsor originalSponsor = new Sponsor("1", "Red Hat", "red-hat", "http://redhat.com", new Image("redhatlogo", "Red Hat Logo"), new Level("Cosmos", 1));
		String csv = originalSponsor.toCSV();
		
		Sponsor parsedSponsor = Sponsor.fromCSV(csv);
		
		Assert.assertEquals(originalSponsor, parsedSponsor);
		
	}
	
	@Test
	public void sponsor_cloud_link() throws IOException {
		String json = new String(Files.readAllBytes(Paths.get("src/test/resources/cloudLinkSponsors.json")), StandardCharsets.UTF_8);
		JsonArray jsonArray = new JsonArray(json);
		for (int i=0; i<jsonArray.size(); i++) {
			Sponsor sponsor = jsonArray.getJsonObject(i).mapTo(Sponsor.class);
			assertNotNull(sponsor);
			assertNotNull(sponsor.getImage());
			assertNotNull(sponsor.getLevel());
			assertTrue(sponsor.getHref().startsWith("http"));
			assertTrue(sponsor.getImage().getSrc().startsWith("http"));
		}
		 
	}
}
