package com._4point.aem.watchedfolder.core;

import org.osgi.service.component.annotations.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
	
	
	private final Supplier<String> endpointLocation;

	public WatchedFolderContentProcessor(Supplier<String> endpointLocation) {
		this.endpointLocation = endpointLocation;
	}

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

		HttpUriRequest multipartRequest = buildRequest(inputs, endpointLocation.get());

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpResponse httpResponse = httpclient.execute(multipartRequest);
			
			ByteArrayOutputStream bas = new ByteArrayOutputStream();
			HttpEntity entity = httpResponse.getEntity();
			Header contentType = entity.getContentType();
			HeaderElement[] contentTypeElements = contentType.getElements();
			if (contentTypeElements.length != 1) {
				// TODO: Do something here
			}
			String contentTypeValue = contentTypeElements[0].getName();

			StatusLine statusLine = httpResponse.getStatusLine();
			entity.writeTo(bas);
			return new AbstractMap.SimpleEntry<>(isStatusOk(statusLine.getStatusCode()) ? "result" : "error", bas.toByteArray());
		} catch (IOException e) {
			throw new IllegalStateException("Error occurred during POST to '" + endpointLocation + "'.", e);
		}

	}

	private static HttpUriRequest buildRequest(Stream<Entry<String, InputStream>> inputs, String endpointLocation) {
		RequestBuilder reqbuilder = RequestBuilder.post(endpointLocation);
		reqbuilder.setEntity(toMultipartEntiry(inputs.collect(Collectors.toList())));
		return reqbuilder.build();
	}

	private static HttpEntity toMultipartEntiry(List<Entry<String, InputStream>> inputsList) {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		for (Entry<String, InputStream> entry : inputsList) {
			builder = builder.addPart(entry.getKey(), new InputStreamBody(entry.getValue(), entry.getKey()));
		}
		return builder.build();
	}
	
	private static boolean isStatusOk(int statusCode) {
		return statusCode < 200 || statusCode > 299 ? false : true;
	}
}


