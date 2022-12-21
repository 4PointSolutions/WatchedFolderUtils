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
import com._4point.aem.watchedfolder.core.WatchedFolderRestPoster.Result;
import com._4point.aem.watchedfolder.core.WatchedFolderRestPoster.WatchedFolderRestPosterException;

/**
 * These tests require that wiremock be running in order for them to pass.  To start wiremock, run the wiremock JBang script
 * in the JBang directory of this project's repository.
 * 
 * The reason wiremock has to be run separately is because of a ridiculously old version of Google quava use within the AEM
 * dependencies.  It conflicts with the more recent version used by wiremock, so wiremock has to be run within it's own
 * class path.
 * 
 * These tests are normally disabled during the maven build since wiremock isn't guaranteed to be running. 
 *
 */
@Tag("requiresWireMockRunning")
class WatchedFolderRestPosterTest {
	private static final String MOCK_WATCHED_FOLDER_ID = "WatchedFolderId1";

	private WatchedFolderRestPoster underTest = new WatchedFolderRestPoster();

	@Test
	void testProcessInputs_HappyPath() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("test1", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8))),
				new AbstractMap.SimpleEntry<>("test2", new ByteArrayInputStream("SomeText2".getBytes(StandardCharsets.UTF_8)))
				);
		Result result = underTest.processInputs(inputs.stream(), createMockConfig(), MOCK_WATCHED_FOLDER_ID);
		
		assertAll(
				()->assertEquals("result", result.filename()),
				()->assertEquals("multipart/form-data; boundary=Boundary_33_2015684127_1590785034918", result.contentType()),
				()->assertTrue(result.bytes().length > 70000)	// TODO: Need to develop a better test, this is just a placeholder.
				);
	}

	@Test
	void testProcessInputs_HappyPath_wFilename() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("filename_test", new ByteArrayInputStream("Filename Text".getBytes(StandardCharsets.UTF_8)))
				);
		Result result = underTest.processInputs(inputs.stream(), createMockConfig(), MOCK_WATCHED_FOLDER_ID);
		
		assertAll(
				()->assertEquals("result_filename.txt", result.filename()),
				()->assertEquals("text/plain; charset=utf-8", result.contentType()),
				()->assertEquals("This is the result of the call.", new String(result.bytes(), StandardCharsets.UTF_8))
				);
	}

	@Test
	void testProcessInputs_BadRequestException() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("BadRequestException", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8)))
				);

		WatchedFolderRestPosterException ex = assertThrows(WatchedFolderRestPosterException.class, ()->underTest.processInputs(inputs.stream(), createMockConfig(), MOCK_WATCHED_FOLDER_ID));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertThat(msg, containsString("BadRequest"));
	}

	@Test
	void testProcessInputs_InternalErrorException() throws Exception {
		List<Entry<String, InputStream>> inputs = Arrays.asList(
				new AbstractMap.SimpleEntry<>("InternalErrorException", new ByteArrayInputStream("SomeText1".getBytes(StandardCharsets.UTF_8)))
				);

		WatchedFolderRestPosterException ex = assertThrows(WatchedFolderRestPosterException.class, ()->underTest.processInputs(inputs.stream(), createMockConfig(), MOCK_WATCHED_FOLDER_ID));
		String msg = ex.getMessage();
		assertNotNull(msg);
		assertThat(msg, containsString("Internal Server Error"));
	}

	private static ConfigurationParameters createMockConfig() {
		Map<String, Object> configValues = Collections.singletonMap("endpoint", "http://localhost:8088/getDocument");
		ConfigurationParameters mockConfigParams = new ConfigurationParameters(configValues );
		return mockConfigParams;
	}

}
