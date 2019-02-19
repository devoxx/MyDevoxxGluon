package com.jbcnconf.lambda;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

import io.vertx.core.json.JsonObject;

public class ConferenceLambdaTest {

	
	@Test
	public void invocation() {
		ConferenceLambda lambda = new ConferenceLambda();
		String result = lambda.handleRequest(null, mock(Context.class));
		Assert.assertNotNull(result);
		try {
			new JsonObject(result);
		} catch (Exception e) {
			Assert.fail("Not valid json returned");
		}
		
	}
	
}
