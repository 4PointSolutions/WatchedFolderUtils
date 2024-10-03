package com._4point.aem.watchedfolder.mock_client;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
class RestEndpointControllerTest {

	@Autowired private MockMvc mockMvc;
	
	@Test
	void testEndpoint() throws Exception {
		this.mockMvc.perform(post("/test/endpoint"))
					.andExpect(content().string(equalTo("Test Response")));
	}

}
