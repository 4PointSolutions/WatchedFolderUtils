package com._4point.aem.watchedfolder.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
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

import com._4point.aem.watchedfolder.core.ProcessingMetadata.ProcessingMetadataBuilder;
import com._4point.aem.watchedfolder.support.Jdk8Utils;
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
		Result result = processInputs(context.getInputMap()
											 .entrySet().stream()
											 			.map(WatchedFolderRestPoster::removeDocumentWrapper),
									  new ConfigurationParameters(context.getConfigParameters()),
									  context.getWatchFolderId()
					  				  );
		context.setResult(result.filename, result.toDocument());
	}

	/**
	 *  This method removes Adobe's Document objects because they are hard to mock and cannot therefore be used in unit
	 *  testing.  I wish they had made the Document object an interface instead, but c'est la guerre.
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
	 * This method does all the work.  It is called directly by the unit tests.
	 * 
	 * @param inputs
	 * @return
	 */
	/* package */ Result processInputs(Stream<Entry<String, InputStream>> inputs, ConfigurationParameters configParams, String watchedFolderId) {
		String correlationId = CorrelationId.generate();
		log.info("Processing watched folder transaction '" + correlationId + "' from watched folder id '" + watchedFolderId + "'.");
		ProcessingMetadataBuilder metadataBuilder = ProcessingMetadata.start(correlationId);
		configParams.logValues();

		HttpUriRequest multipartRequest = buildRequest(inputs, configParams.endpoint(), correlationId);

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpResponse httpResponse = httpclient.execute(multipartRequest);
			
			byte[] responseBytes = Jdk8Utils.readAllBytes(httpResponse.getEntity().getContent());

			StatusLine statusLine = httpResponse.getStatusLine();
			if (isErrorStatus(statusLine.getStatusCode())) {
				throw new WatchedFolderRestPosterException(new String(responseBytes, StandardCharsets.UTF_8), new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase()));
			}

			// Get the filename from the Content-Disposition header or use "result" if it's not there.
			String filename = ContentDispositionHeader.from(httpResponse)
					.map(ContentDispositionHeader::filename)
					.orElse("result");

			// Get content type from the Content-Type header
			String contentType = ContentTypeHeader.from(httpResponse)
					.map(ContentTypeHeader::getFullValue)
					.orElse("application/octet-stream");
			
			ProcessingMetadata processingMetadata = metadataBuilder.finish();
			log.info("Completed transaction '" + processingMetadata.getCorrelationId() + "' in " + processingMetadata.getFormattedElapsedTime() + ".");
			return new Result(responseBytes, contentType, filename);
		} catch (IOException e) {
			throw new IllegalStateException("Error occurred during POST to '" + configParams.endpoint() + "'.", e);
		}

	}

	private static HttpUriRequest buildRequest(Stream<Entry<String, InputStream>> inputs, String endpointLocation, String correlationId) {
		RequestBuilder reqbuilder = RequestBuilder.post(endpointLocation);
		reqbuilder.setEntity(toMultipartEntiry(inputs.collect(Collectors.toList())));
		reqbuilder.addHeader(CorrelationId.CORRELATION_ID_HDR, correlationId);
		return reqbuilder.build();
	}

	private static HttpEntity toMultipartEntiry(List<Entry<String, InputStream>> inputsList) {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		for (Entry<String, InputStream> entry : inputsList) {
			builder = builder.addPart(entry.getKey(), new InputStreamBody(entry.getValue(), entry.getKey()));
		}
		return builder.build();
	}
	
	private static boolean isErrorStatus(int statusCode) {
		return statusCode < 200 || statusCode > 299;
	}
	
	public static class Result {
		private final byte[] bytes;
		private final String contentType;
		private final String filename;

		public Result(byte[] bytes, String contentType, String filename) {
			this.bytes = bytes;
			this.contentType = contentType;
			this.filename = filename;
		}

		public byte[] bytes() {
			return bytes;
		}

		public String contentType() {
			return contentType;
		}

		public String filename() {
			return filename;
		}
		
		public Document toDocument() {
			Document doc = new Document(bytes());
			doc.setContentType(contentType);
			doc.setAttribute("filename", filename);
			return doc;
		}
	}
	
	public static class ConfigurationParameters {
		private static final String ENDPOINT_PARAM_NAME = "endpoint";
		
		private final Map<String, Object> configParameters;
		private final String endpoint;

		public ConfigurationParameters(Map<String, Object> configParameters) {
			this.configParameters = configParameters;
			this.endpoint = getString(ENDPOINT_PARAM_NAME).orElseThrow(()->new IllegalArgumentException(ENDPOINT_PARAM_NAME + " configuration parameter was not configured."));
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
		
		private Optional<String> getString(String key) {
			Object value_obj = configParameters.get(key);
			if (value_obj == null) {
				return Optional.empty();
			} else if (value_obj instanceof String) {
				return Optional.of((String)value_obj);
			} else {
				throw new IllegalArgumentException("'" + key + "' config parameter is not a String. (" + value_obj.getClass().getName() + ")");
			}
		}
	}
	
	private static abstract class HttpHeader {
		private final HeaderElement headerElement;
		private final String headerValue;

		public HttpHeader(Header header) {
			this.headerElement = header.getElements()[0];
			this.headerValue = header.getValue();
			
		}
		
		public String getValue() { return headerElement.getName(); }
		protected String getParameter(String name) { return headerElement.getParameterByName(name).getValue(); }
		public String getFullValue() { return headerValue; }
 	}
	
	private static class ContentDispositionHeader extends HttpHeader {
		private static final String CONTENT_DISPOSITION_HEADER_NAME = "Content-Disposition";
		private static final String FILENAME_PARAMETER_NAME = "filename";

		private ContentDispositionHeader(Header header) {
			super(header);
		}

		public String filename() { return getParameter(FILENAME_PARAMETER_NAME); }

		public static Optional<ContentDispositionHeader> from(HttpResponse response) {
			return Optional.ofNullable(response.getFirstHeader(CONTENT_DISPOSITION_HEADER_NAME))
						   .map(ContentDispositionHeader::new);
		}
	}
	
	private static class ContentTypeHeader extends HttpHeader {
		private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";

		public ContentTypeHeader(Header header) {
			super(header);
		}
		
		public static Optional<ContentTypeHeader> from(HttpResponse response) {
			return Optional.ofNullable(response.getFirstHeader(CONTENT_TYPE_HEADER_NAME))
					   .map(ContentTypeHeader::new);
		}
	}
	
	@SuppressWarnings("serial")
	public static class WatchedFolderRestPosterException extends RuntimeException {

		public WatchedFolderRestPosterException() {
			super();
		}

		public WatchedFolderRestPosterException(String message, Throwable cause) {
			super(message, cause);
		}

		public WatchedFolderRestPosterException(String message) {
			super(message);
		}

		public WatchedFolderRestPosterException(Throwable cause) {
			super(cause);
		}
	}
}


