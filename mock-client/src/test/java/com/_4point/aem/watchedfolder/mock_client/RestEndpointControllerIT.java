package com._4point.aem.watchedfolder.mock_client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestEndpointControllerIT {

	@LocalServerPort
	private int port;
	
	@Test
	void testEndpoint() {
		RestClient restClient = RestClient.create();
		
		String result = restClient.post()
								  .uri("http://localhost:%d/test/endpoint".formatted(port))
								  .body("Input String")
								  .retrieve()
								  .body(String.class);
		
		assertEquals("Test Response", result);
	}

}
