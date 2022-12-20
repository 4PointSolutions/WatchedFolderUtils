package com._4point.aem.watchedfolder.core;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com._4point.aem.watchedfolder.core.WatchedFolderRestPoster.ConfigurationParameters;

@Tag("requiresWireMockRunning")
class WatchedFolderRestPosterTest {

	private WatchedFolderRestPoster underTest = new WatchedFolderRestPoster();

	@Test
	void testProcessInputs_HappyPath() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("test1", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8))),
				new AbstractMap.SimpleEntry<>("test2", new ByteArrayInputStream("SomeText2".getBytes(StandardCharsets.UTF_8)))
				);
		Entry<String, byte[]> result = underTest.processInputs(inputs.stream(), createMockConfig());
		
		assertEquals("result", result.getKey());
		assertTrue(result.getValue().length > 70000);	// TODO: Need to develop a better test, this is just a placeholder.
	}

	@Test
	void testProcessInputs_BadRequestException() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("BadRequestException", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8)))
				);

		Entry<String, byte[]> result = underTest.processInputs(inputs.stream(), createMockConfig());
		
		assertEquals("error", result.getKey());
		assertThat(new String(result.getValue(), StandardCharsets.UTF_8), containsString("BadRequest"));
	}

	@Test
	void testProcessInputs_InternalErrorException() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("InternalErrorException", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8)))
				);

		Entry<String, byte[]> result = underTest.processInputs(inputs.stream(), createMockConfig());
		
		assertEquals("error", result.getKey());
		assertThat(new String(result.getValue(), StandardCharsets.UTF_8), containsString("Internal Server Error"));
	}

	private static ConfigurationParameters createMockConfig() {
		Map<String, Object> configValues = Collections.singletonMap("endpoint", "http://localhost:8088/getDocument");
		ConfigurationParameters mockConfigParams = new ConfigurationParameters(configValues );
		return mockConfigParams;
	}

}
