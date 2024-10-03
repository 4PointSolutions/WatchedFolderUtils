package com._4point.aem.watchedfolder.mock_client;

public class RequestResponseUtils {
	private static final String JSON_BODY = """
			{
				"delay" : %d
			}
			""";

	/* package */ static String expectedResponse(int numSecs) {
		return "Delayed for %d second%s.".formatted(numSecs, numSecs > 1 ? "s" : "");
	}

	/* package */ static String jsonRequestBody(int numSecs) {
		return JSON_BODY.formatted(numSecs);
	}
}
