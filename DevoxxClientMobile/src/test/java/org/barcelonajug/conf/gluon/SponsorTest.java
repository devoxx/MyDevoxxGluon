package org.barcelonajug.conf.gluon;

import org.junit.Assert;
import org.junit.Test;

import com.devoxx.model.Image;
import com.devoxx.model.Level;
import com.devoxx.model.Sponsor;



public class SponsorTest {

	@Test
	public void sponsor_csv() {
		Sponsor originalSponsor = new Sponsor("1", "Red Hat", "red-hat", "http://redhat.com", new Image("redhatlogo", "Red Hat Logo"), new Level("Cosmos", 1));
		String csv = originalSponsor.toCSV();
		
		Sponsor parsedSponsor = Sponsor.fromCSV(csv);
		
		Assert.assertEquals(originalSponsor, parsedSponsor);
		
	}
}
