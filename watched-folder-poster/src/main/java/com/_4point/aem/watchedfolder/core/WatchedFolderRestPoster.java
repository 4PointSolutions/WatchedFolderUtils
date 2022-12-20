package com._4point.aem.watchedfolder.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.aemfd.watchfolder.service.api.ContentProcessor;
import com.adobe.aemfd.watchfolder.service.api.ProcessorContext;

@Component(service = ContentProcessor.class,
		   property = {
				   Constants.SERVICE_DESCRIPTION + "=4Point Watched Folder REST Poster",
				   Constants.SERVICE_PID + "=com._4point.aem.watchedfolder.core.WatchedFolderRestPoster"
				   },
		   immediate = true
		   )
public class WatchedFolderRestPoster implements ContentProcessor {
	private static final Logger log = LoggerFactory.getLogger(WatchedFolderRestPoster.class);
	
	public WatchedFolderRestPoster() {
	}

	/**
	 *	Called by AEM's Watched Folder mechanism.
	 */
	@Override
	public void processInputs(ProcessorContext context) throws Exception {	
		Entry<String, byte[]> result = processInputs(context.getInputMap()
															.entrySet().stream()
																	   .map(WatchedFolderRestPoster::removeDocumentWrapper),
													 new ConfigurationParameters(context.getConfigParameters())
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
	public Entry<String, byte[]> processInputs(Stream<Entry<String, InputStream>> inputs, ConfigurationParameters configParams) {

		HttpUriRequest multipartRequest = buildRequest(inputs, configParams.endpoint());

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
			throw new IllegalStateException("Error occurred during POST to '" + configParams.endpoint() + "'.", e);
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
	
	
	public static class ConfigurationParameters {
		private static final String ENDPOINT_PARAM_NAME = "endpoint";
		
		private final Map<String, Object> configParameters;
		private final String endpoint;

		public ConfigurationParameters(Map<String, Object> configParameters) {
			this.configParameters = configParameters;
			this.endpoint = getString(ENDPOINT_PARAM_NAME);
		}

		public ConfigurationParameters logValues() {
			for (Entry<String, Object> entry : configParameters.entrySet()) {
				Object obj_value = entry.getValue();
				if (obj_value instanceof String) {
					String str_value = (String) obj_value;
					log.debug("Found Parameter - '" + entry.getKey() + "'/'" + str_value + "'.");
				} else {
					log.debug("Found Parameter - '" + entry.getKey() + "'/<object>.");
				}
			}
			return this;
		}
		
		public String endpoint() {
			return endpoint;
		}
		
		private String getString(String key) {
			Object value_obj = configParameters.get(key);
			if (value_obj instanceof String) {
				return (String) value_obj;
			} else {
				throw new IllegalStateException("'" + key + "' config parameter is not a String. (" + value_obj.getClass().getName() + ")");
			}
		}
	}
}


