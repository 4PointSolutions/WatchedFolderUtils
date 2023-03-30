package com._4point.aem.watchedfolder.noop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._4point.aem.watchedfolder.support.Jdk8Utils;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.aemfd.watchfolder.service.api.ContentProcessor;
import com.adobe.aemfd.watchfolder.service.api.ProcessorContext;

@Component(service = ContentProcessor.class,
		   property = {
					   Constants.SERVICE_DESCRIPTION + "=4Point Watched Folder No-Op Processor",
					   Constants.SERVICE_PID + "=com._4point.aem.watchedfolder.noop.WatchedFolderNoOpProcessor"
					   },
		   immediate = true
		   )
public class WatchedFolderNoOpProcessor implements ContentProcessor {
	private static final Logger log = LoggerFactory.getLogger(WatchedFolderNoOpProcessor.class);

	private static final String RESULT_CONTENT = "Result was a success!";

	public WatchedFolderNoOpProcessor() {
		GitCommit.logSummary();
	}

	@Override
	public void processInputs(ProcessorContext context) throws Exception {
		log.info("-- Begin Inputs --");
		for (Map.Entry<String,Document> entry : context.getInputMap().entrySet()) {
			logInput(entry);
		}
		log.info("-- End Inputs --");
		
		Result result = new Result(RESULT_CONTENT.getBytes(StandardCharsets.UTF_8), "text/plain", "result.txt");
		log.info("Returning {} bytes with content-type='{}' and filename='{}' to AEM.", result.bytes.length, result.contentType, result.filename);

		context.setResult(result.filename(), result.toDocument());
	}

	private static void logInput(Map.Entry<String,Document> entry) throws IOException {
		Document doc = entry.getValue();
		Object filename = doc.getAttribute("filename");
		String contentType = doc.getContentType();
		log.info("Found input '{}' of type {}.", entry.getKey(), contentType);
		if (filename != null) {
			if (filename instanceof String) {
				log.info("   Filename={}", (String)filename);
			} else {
				log.info("   Filename is instanceof {}", filename.getClass().getName());
			}
		}
		if (log.isTraceEnabled() && (contentType.startsWith("text") || contentType.endsWith("xml"))) {
			byte[] bytes = Jdk8Utils.readAllBytes(doc.getInputStream());
			String string = new String(bytes, StandardCharsets.UTF_8);
			log.trace("input docBytes='{}'.", string);
			// The following lines were added in order to track down a non-visible Unicode character issue.
			// Feel free to uncomment them if you are chasing up something similar.
			// Files.write(Paths.get("/temp/NoOpBytes.txt"), bytes, StandardOpenOption.CREATE);
			// Files.write(Paths.get("/temp/NoOpString.txt"), string.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		}
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
}
