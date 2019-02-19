package com.jbcnconf.lambda;

import static com.jbcnconf.lambda.MockResponseRetriever.getMockResponseFor;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class SpeakersLambda  implements RequestHandler<Map<String, Object>, String>{

	@Override
	public String handleRequest(Map<String, Object> input, Context context) {
		return getMockResponseFor("speakers");
	}

}