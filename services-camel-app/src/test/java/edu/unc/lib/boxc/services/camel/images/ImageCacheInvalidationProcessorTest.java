package edu.unc.lib.boxc.services.camel.images;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@WireMockTest(httpPort = 46887)
public class ImageCacheInvalidationProcessorTest {
    private static final String TEST_UUID = "40031d0b-9be6-439e-bad7-33c3e455d9a5";
    private static final String TEST_IMG_IDENTIFIER = "40/03/1d/0b/40031d0b-9be6-439e-bad7-33c3e455d9a5.jp2";
    private ImageCacheInvalidationProcessor processor;
    private HttpClientConnectionManager connectionManager;
    private String imageServerBasePath = "http://localhost:46887";
    private Exchange exchange;
    private Message messageIn;
    private ListAppender<ILoggingEvent> actionAppender;

    @BeforeEach
    public void init() {
        connectionManager = new PoolingHttpClientConnectionManager();
        processor = new ImageCacheInvalidationProcessor();
        processor.setConnectionManager(connectionManager);
        processor.setImageServerBasePath(imageServerBasePath);
        processor.setImageServerUsername("user");
        processor.setImageServerPassword("password");
        exchange = ProcessorTestHelper.mockExchange("");
        messageIn = exchange.getIn();

        Logger actionLogger = (Logger) LoggerFactory.getLogger(ImageCacheInvalidationProcessor.class);
        actionAppender = new ListAppender<>();
        actionLogger.setLevel(Level.DEBUG);
        actionLogger.addAppender(actionAppender);
        actionAppender.start();
    }

    @AfterEach
    public void cleanup() throws Exception {
        connectionManager.shutdown();
        actionAppender.stop();
    }

    @Test
    public void processSuccessTest() throws Exception {
        stubFor(WireMock.post(urlMatching( "/tasks"))
                .withRequestBody(containing(TEST_IMG_IDENTIFIER))
                .withRequestBody(containing("PurgeItemFromCache"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.ACCEPTED.value())));

        PID filePid = PIDs.get(TEST_UUID);
        when(messageIn.getHeader(FCREPO_URI)).thenReturn(filePid.getRepositoryPath());
        processor.process(exchange);

        var loggedMessages = getLoggedMessages();
        assertTrue(loggedMessages.contains("Successfully invalidated image cache for " + TEST_UUID),
                "Did not contain expected log message, log was: " + loggedMessages);
    }

    @Test
    public void processFailureTest() throws Exception {
        stubFor(WireMock.post(urlMatching( "/tasks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        PID filePid = ProcessorTestHelper.makePid();
        when(messageIn.getHeader(FCREPO_URI)).thenReturn(filePid.getRepositoryPath());
        processor.process(exchange);

        var loggedMessages = getLoggedMessages();
        assertTrue(loggedMessages.contains("Failed to invalidate image cache for " + filePid.getId()),
                "Did not contain expected log message, log was: " + loggedMessages);
    }

    private String getLoggedMessages() {
        StringBuilder sb = new StringBuilder();
        for (ILoggingEvent event : actionAppender.list) {
            sb.append(event.getFormattedMessage()).append("\n");
        }
        return sb.toString();
    }
}
