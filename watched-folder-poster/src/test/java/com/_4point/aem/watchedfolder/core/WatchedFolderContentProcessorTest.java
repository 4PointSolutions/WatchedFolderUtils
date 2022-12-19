package com._4point.aem.watchedfolder.core;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class WatchedFolderContentProcessorTest {

	private final WatchedFolderContentProcessor underTest = new WatchedFolderContentProcessor();
	
	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testProcessInputs() throws Exception {
		List<Entry<String, InputStream>> inputs = Collections.emptyList();

		Entry<String, byte[]> result = underTest.processInputs(inputs.stream());
		
		assertEquals("dummy", result.getKey());
		assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), result.getValue());
	}

}
