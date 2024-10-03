package com._4point.aem.watchedfolder.mock_client;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestEndpointControllerIT {

	@LocalServerPort
	private int port;
	
	// For a simple file system with Unix-style paths and behavior:
	FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

	@ParameterizedTest
	@ValueSource(ints = {1, 5})
	void testEndpoint(int numSecs) throws Exception {
		RestClient restClient = RestClient.create();
		
		// REST Client will apparently only generate the correct request with a FileSystemResource,
		// so we have to read it from a file system.  We'll use JimFs for this.
		Path inputJsonPath = fs.getPath("input.json");
		Files.writeString(inputJsonPath, RequestResponseUtils.jsonRequestBody(numSecs));
		
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("datafiles", new FileSystemResource(inputJsonPath));
		
		String result = restClient.post()
								  .uri("http://localhost:%d/test/endpoint".formatted(port))
								  .body(body)
								  .retrieve()
								  .body(String.class);
		
		assertEquals(RequestResponseUtils.expectedResponse(numSecs), result);
	}

}
