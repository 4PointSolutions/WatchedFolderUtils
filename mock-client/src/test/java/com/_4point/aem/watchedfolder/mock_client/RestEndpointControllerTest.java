package com._4point.aem.watchedfolder.mock_client;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com._4point.aem.watchedfolder.mock_client.RequestResponseUtils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
class RestEndpointControllerTest {

	@Autowired private MockMvc mockMvc;
	
	@ParameterizedTest
	@ValueSource(ints = {1, 5})
	void testEndpoint(int numSecs) throws Exception {
		MockMultipartFile file = new MockMultipartFile("datafiles", "input.json", MediaType.MULTIPART_FORM_DATA.toString(), 
				jsonRequestBody(numSecs).getBytes());
		
		this.mockMvc.perform(multipart("/test/endpoint").file(file))
					.andExpect(status().isOk())
					.andExpect(content().string(equalTo(expectedResponse(numSecs))));
	}

	@Test
	void testEndpoint_NoDataFiles() throws Exception {
		this.mockMvc.perform(multipart("/test/endpoint"))
					.andExpect(status().is4xxClientError());
		
	}
}
