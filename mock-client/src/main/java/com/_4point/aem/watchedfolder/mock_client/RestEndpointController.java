package com._4point.aem.watchedfolder.mock_client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestEndpointController {

	
	@PostMapping("/test/endpoint")
	@ResponseBody String endpoint() {
		return "Test Response";
	}
}
