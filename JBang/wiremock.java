///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS com.github.tomakehurst:wiremock-jre8:2.35.0
//DEPS org.slf4j:jul-to-slf4j:1.7.36
//DEPS org.slf4j:slf4j-simple:1.7.36

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

@Command(name = "wiremock", mixinStandardHelpOptions = true, version = "wiremock 0.1",
        description = "Run WireMock server")
class wiremock implements Callable<Integer> {
	private static Logger log = Logger.getLogger(wiremock.class.getName());
	
    @Parameters(index = "0", description = "Turn recording on", defaultValue = "false")
    private Boolean recordingOn;

    @Parameters(index = "1", description = "Recording service Url", defaultValue = "http://locahost:8080")
    private String recordingUrl;

    private static WireMockServer wireMockServer = new WireMockServer(8088); //No-args constructor will start on port 8080, no HTTPS
    
    private static Thread thread;
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new wiremock()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
    	setLevel(Level.FINEST);
    	wireMockServer.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("mappings")));
		if (recordingOn) {
			wireMockServer.startRecording(recordingUrl);
		}
    	thread = Thread.currentThread();
    	
    	wireMockServer.start();
    	createShutDownHook();
    	
        System.out.println();
        System.out.println("Started wiremock server... isRunning=" + wireMockServer.isRunning() + " on port " + wireMockServer.port());
        
    	for( StubMapping mapping : wireMockServer.listAllStubMappings().getMappings()) {
    		System.out.println("Found " + mapping.getName());
    	}
    	
   		try {
			Thread.sleep(Duration.ofHours(8).toMillis());	// Sleep for 8 hours (or until CTRL-C is hit)
		} catch (InterruptedException e) {
			// Carry on...
		}
   		
		if (recordingOn) {
			SnapshotRecordResult recordings = wireMockServer.stopRecording();
			List<StubMapping> mappings = recordings.getStubMappings();
			System.out.println("Found " + mappings.size() + " recordings.");
			for (StubMapping mapping : mappings) {
				ResponseDefinition response = mapping.getResponse();
				var jsonBody = response.getJsonBody();
				System.out.println(jsonBody == null ? "JsonBody is null" : jsonBody.toPrettyString());
			}
		}
        System.out.println();
        System.out.println("Shutting down wiremock server...");
        wireMockServer.stop();
        return 0;
    }
    
    private static void createShutDownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                thread.interrupt();
            }
        }));
    }
    
    public static void setLevel(Level targetLevel) {
        Logger root = Logger.getLogger("");
        root.setLevel(targetLevel);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(targetLevel);
        }
        System.out.println("level set: " + targetLevel.getName());
    }
}
