package edu.unc.lib.boxc.services.camel.fulltext;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FulltextRouterTest extends CamelTestSupport {
    private static final String ENHANCEMENT_ROUTE = "CdrServiceFulltextExtraction";
    private static final String EXTRACTION_ROUTE = "ExtractingText";
    private static final String FILE_ID = "343b3da4-8876-42f5-8821-7aabb65e0f19";

    @Mock
    private FulltextProcessor ftProcessor;
    private AddDerivativeProcessor adProcessor;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @TempDir
    public Path tmpFolder;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        adProcessor = new AddDerivativeProcessor(DatastreamType.FULLTEXT_EXTRACTION.getExtension(),
                tmpFolder.toAbsolutePath().toString());
        var router = new FulltextRouter();
        router.setFulltextProcessor(ftProcessor);
        router.setAddDerivativeProcessor(adProcessor);
        return router;
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeNoForceNoExistingFile() throws Exception {
        var mockEndpoint = getMockEndpoint("mock:direct:fulltext.extraction");
        mockEndpoint.expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        template.sendBodyAndHeaders("", createEvent());
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeForceNoExistingFile() throws Exception {
        var mockEndpoint = getMockEndpoint("mock:direct:fulltext.extraction");
        mockEndpoint.expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put("force", "true");
        template.sendBodyAndHeaders("", headers);
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeNoForceExistingFile() throws Exception {
        String derivativePath = idToPath(FILE_ID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path existingPath = tmpFolder.resolve(derivativePath).resolve(FILE_ID + ".txt");
        FileUtils.writeStringToFile(existingPath.toFile(), "extracted text", "utf-8");

        var mockEndpoint = getMockEndpoint("mock:direct:fulltext.extraction");
        mockEndpoint.expectedMessageCount(0);
        createContext(ENHANCEMENT_ROUTE);

        template.sendBodyAndHeaders("", createEvent());
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeForceExistingFile() throws Exception {
        String derivativePath = idToPath(FILE_ID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path existingPath = tmpFolder.resolve(derivativePath).resolve(FILE_ID + ".txt");
        FileUtils.writeStringToFile(existingPath.toFile(), "extracted text", "utf-8");

        var mockEndpoint = getMockEndpoint("mock:direct:fulltext.extraction");
        mockEndpoint.expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put("force", "true");
        template.sendBodyAndHeaders("", headers);
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterInvalidMimeType() throws Exception {
        var mockEndpoint = getMockEndpoint("mock:direct:fulltext.extraction");
        mockEndpoint.expectedMessageCount(0);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put(CdrBinaryMimeType, "image/png");

        template.sendBodyAndHeaders("", headers);

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testTextExtraction() throws Exception {
        createContext(EXTRACTION_ROUTE);

        Map<String, Object> headers = createEvent();
        template.sendBodyAndHeaders("", headers);

        verify(ftProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static Map<String, Object> createEvent() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Binary.getURI());
        headers.put(FCREPO_URI, FILE_ID);
        headers.put(CdrBinaryMimeType, "text/plain");
        headers.put("force", "false");

        return headers;
    }
}
