package com._4point.aem.watchedfolder.mock_client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@Disabled("Not yet modified for multipart/form-data")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestEndpointControllerIT {

	@LocalServerPort
	private int port;
	
	@ParameterizedTest
	@ValueSource(ints = {1, 5})
	void testEndpoint(int numSecs) {
		RestClient restClient = RestClient.create();
		
		String result = restClient.post()
								  .uri("http://localhost:%d/test/endpoint".formatted(port))
								  .body("Input String")
								  .retrieve()
								  .body(String.class);
		
		assertEquals(RequestResponseUtils.expectedResponse(numSecs), result);
	}

}
