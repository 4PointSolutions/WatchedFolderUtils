package com._4point.aem.watchedfolder.core;

import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.aemfd.watchfolder.service.api.ContentProcessor;
import com.adobe.aemfd.watchfolder.service.api.ProcessorContext;

@Component(service = ContentProcessor.class,
		   property = {Constants.SERVICE_DESCRIPTION + "=4Point Watched Folder Processor", "serviceSelector=4PointWatchedFolderProcessor"},
		   immediate = true
		   )
public class WatchedFolderContentProcessor implements ContentProcessor {
	
	/**
	 *	Called by AEM's Watched Folder mechanism.
	 */
	@Override
	public void processInputs(ProcessorContext context) throws Exception {
		
		Entry<String, byte[]> result = processInputs(context.getInputMap()
															.entrySet().stream()
																	   .map(WatchedFolderContentProcessor::removeDocumentWrapper)
					  								 );
		context.setResult(result.getKey(), new Document(result.getValue()));
	}

	/**
	 *  This method removes Adobe's Document objects because they are hard to mock and cannot therefore be used in unit
	 *  testing.  I wish they had made the Document object and interace instead, but c'est la guerre.
	 * 
	 * @param entry
	 * @return
	 */
	private static Entry<String, InputStream> removeDocumentWrapper(Entry<String, Document> entry) {
		try {
			return new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getInputStream());
		} catch (IOException ex) {
			throw new IllegalStateException("Error while reading input document input stream.", ex);
		}
	}
	
	/**
	 * This method does all the work
	 * 
	 * @param inputs
	 * @return
	 */
	public Entry<String, byte[]> processInputs(Stream<Entry<String, InputStream>> inputs) {
		
		// TODO: Convert inputs into a multipart/form-data POST
		// TODO: Perform the POST
		// TODO: Convert response to return value
		
		return new AbstractMap.SimpleEntry<>("dummy", "foo".getBytes(StandardCharsets.UTF_8));
	}
}
