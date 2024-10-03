package com._4point.aem.watchedfolder.mock_client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RestEndpointController {
	private static final Logger log = LoggerFactory.getLogger(RestEndpointController.class);
	
	@Autowired
	private ObjectMapper mapper;
	
	@PostMapping("/test/endpoint")
	@ResponseBody String endpoint(@RequestPart(required=true) List<MultipartFile> datafiles, @RequestHeader(name="x-correlation-id", required=false) String correlationIdIn) throws Exception {
		log.atInfo().log("cid={} Starting /test/endpoint request.", correlationIdIn);
		log.atInfo().log("cid={} Found {} datafiles.", correlationIdIn, datafiles.size());
		
		if (log.isDebugEnabled()) {
			logDatafilesInfo(datafiles, correlationIdIn);
		}

		if (datafiles.size() < 1) {
			throw new IllegalArgumentException("No data files found! At least one data file must be supplied.");
		}

		MultipartFile dataFile = datafiles.getFirst();

		record Payload(int delay) {};
		Payload payload = mapper.readValue(dataFile.getBytes(), Payload.class);
		
		log.atInfo().log("Delaying for {} seconds.", payload.delay());
		
		Thread.sleep(Duration.ofSeconds(payload.delay()));
		
		log.atInfo().log("cid={} Completed /test/endpoint request.", correlationIdIn);
		return "Delayed for %d second%s.".formatted(payload.delay(), payload.delay > 1 ? "s" : "");
	}

	private void logDatafilesInfo(List<MultipartFile> datafiles, String correlationIdIn) {
		datafiles.stream().forEach(f->{
			log.atDebug().log("cid={} Found '{}' filename, '{}' oldfilename.", correlationIdIn, f.getName(), f.getOriginalFilename());
			log.atTrace()
			   .addArgument(correlationIdIn)
			   .addArgument(()->new String(uncheck(f::getBytes).get()))
			   .log("cid={} Found data:\n{}");
		});
	}
	
	@FunctionalInterface
	private interface ThrowingSupplier<T> {
	    T get() throws IOException;
	}	

	private static <T> Supplier<T> uncheck(ThrowingSupplier<T> supplier) {
			return ()->{
				try {
					return supplier.get();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};
	}
}
