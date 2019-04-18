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
package org.jbcnconf.gluon;

import static org.junit.Assert.assertEquals;
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
import io.vertx.core.json.JsonObject;


public class SponsorTest {

	@Test
	public void sponsor_csv() {
		Sponsor originalSponsor = new Sponsor("1", "Red Hat", "red-hat", "http://redhat.com", new Image("redhatlogo", "Red Hat Logo"), new Level("Cosmos", 1));
		String csv = originalSponsor.toCSV();
		
		Sponsor parsedSponsor = Sponsor.fromCSV(csv);
		
		Assert.assertEquals(originalSponsor, parsedSponsor);
		
	}
	
	@Test
	public void sponsors_cloud_link() throws IOException {
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
	
	@Test
	public void single_sponsor_cloud_link() throws IOException {
		String json = new String(Files.readAllBytes(Paths.get("src/test/resources/cloudLinkMockSponsor.json")), StandardCharsets.UTF_8);
		JsonObject jsonObj = new JsonObject(json);
		Sponsor sponsor = jsonObj.mapTo(Sponsor.class);
		assertEquals("1", sponsor.getId());
		assertEquals("Red Hat", sponsor.getName());
		assertEquals("https://developers.redhat.com", sponsor.getHref());
		assertEquals("red-hat", sponsor.getSlug());
		assertEquals("https://www.jbcnconf.com/2019/assets/img/sponsors/redhat_rgb.png", sponsor.getImage().getSrc());
	}
}
