package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author bbpennel
 */
public class DeregisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String FILTER_DEREGISTER_ENDPOINT = "direct:filter.longleaf.deregister";

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        Path tmpPath = tmpFolder.resolve("output_file");
        Files.createFile(tmpPath);
        outputPath = tmpPath.toAbsolutePath().toString();
        output = null;
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);

        registerToLongleafProcessor = mock(RegisterToLongleafProcessor.class);
        deregisterLongleafProcessor = new DeregisterLongleafProcessor();
        deregisterLongleafProcessor.setLongleafBaseCommand(longleafScript);
        var router = getLongleafRouter();
        return router;
    }

    @Test
    public void deregisterSingleBinary() throws Exception {
        // Expecting 1 batch message and 1 individual file message, on different routes
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1 + 1)
                .create();

        String contentUri = generateContentUri();
        sendMessages(contentUri);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(2000, contentUri);
    }

    @Test
    public void deregisterSingleBatch() throws Exception {
        // Expecting 1 batch message and 3 individual file messages, on different routes
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1 + 3)
                .create();

        String[] contentUris = generateContentUris(3);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(2000, contentUris);
    }

    @Test
    public void deregisterMultipleBatches() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2 + 10)
                .create();

        String[] contentUris = generateContentUris(10);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(5000, contentUris);
    }

    // Should process file uris, and absolute paths without file://, but not http uris or relative
    @Test
    public void deregisterMultipleMixOfSchemes() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2 + 9)
                .create();

        String[] contentUris = new String[3*4];
        String[] successUris = new String[3*2];
        for (int i = 0; i < 3; i++) {
            contentUris[i*3] = generateContentUri();
            successUris[i*2] = contentUris[i*3];
            contentUris[i*3+1] = "/path/to/file/" + UUID.randomUUID().toString();
            successUris[i*2+1] = contentUris[i*3+1];
            contentUris[i*3+2] = PIDs.get(UUID.randomUUID().toString()).getRepositoryPath();
            contentUris[i*3+3] = "file/" + UUID.randomUUID().toString();
        }
        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(10000, successUris);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deregisterPartialSuccess() throws Exception {
        mockDlq.expectedMessageCount(1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(3)
                .create();

        String[] contentUris = generateContentUris(3);

        // Append to existing script
        FileUtils.writeStringToFile(new File(longleafScript),
                "\necho \"SUCCESS register " + Paths.get(URI.create(contentUris[0])).toString() + "\"" +
                "\necho \"FAILURE register " + Paths.get(URI.create(contentUris[1])).toString() + "\"" +
                "\necho \"SUCCESS register " + Paths.get(URI.create(contentUris[2])).toString() + "\"" +
                "\nexit 2",
                UTF_8, true);

        sendMessages(contentUris);

        boolean result1 = notify.matches(20l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(5000, contentUris);

        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals("Only two uris should be in the failed message body", 1, failedList.size());
        assertTrue("Exchange in DLQ must contain the fcrepo uri of the failed binary",
                failedList.contains(contentUris[1]));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deregisterCommandErrorSuccessExit() throws Exception {
        mockDlq.expectedMessageCount(1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(1)
                .create();

        String[] contentUris = generateContentUris(1);

        // Append to existing script
        FileUtils.writeStringToFile(new File(longleafScript),
                "\necho 'ERROR: \"longleaf deregister\" was called with arguments [\"--ohno\"]'",
                UTF_8, true);

        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertSubmittedPaths(5000, contentUris);

        mockDlq.assertIsSatisfied(1000);

        List<Exchange> dlqExchanges = mockDlq.getExchanges();
        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals("Only one uri should be in the failed message body", 1, failedList.size());

        assertTrue("Exchange in DLQ must contain the fcrepo uri of the unprocessed binary",
                failedList.contains(contentUris[0]));
    }

    private String generateContentUri() {
        return "file:///path/to/file/" + UUID.randomUUID().toString() + "." + System.nanoTime();
    }

    private String[] generateContentUris(int num) {
        String[] uris = new String[num];
        for (int i = 0; i < num; i++) {
            uris[i] = generateContentUri();
        }
        return uris;
    }

    private void sendMessages(String... contentUris) {
        for (String contentUri : contentUris) {
            template.sendBody(FILTER_DEREGISTER_ENDPOINT, makeDocument(contentUri));
        }
    }

    private String makeDocument(String uri) {
        XMLOutputter out = new XMLOutputter();
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);

        Element obj = new Element("objToDestroy", CDR_MESSAGE_NS);
        Element uriValue = new Element("contentUri", CDR_MESSAGE_NS).setText(uri);
        obj.addContent(uriValue);

        entry.addContent(obj);
        msg.addContent(entry);

        return out.outputString(msg);
    }
}
