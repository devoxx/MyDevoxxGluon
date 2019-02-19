package com.jbcnconf.lambda;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

import io.vertx.core.json.JsonArray;

public class SpeakersLambdaTest {

	
	@Test
	public void invocation() {
		SpeakersLambda lambda = new SpeakersLambda();
		String result = lambda.handleRequest(null, mock(Context.class));
		Assert.assertNotNull(result);
		try {
			new JsonArray(result);
		} catch (Exception e) {
			Assert.fail("Not valid json returned");
		}
		
	}
	
}
