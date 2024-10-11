package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DestroyDerivativesRouterTest extends CamelTestSupport {
    private static final String DESTROY_DERIVATIVES_ROUTE = "CdrDestroyDerivatives";
    private static final String DESTROY_FULLTEXT_ROUTE = "CdrDestroyFullText";
    private static final String DESTROY_IMAGE_ROUTE = "CdrDestroyImage";
    private static final String TEST_ID = "dee2614c-8a4b-4ac2-baf2-4b4afc11af87";

    @Produce("direct:start")
    private ProducerTemplate template;

    private DestroyedMsgProcessor destroyedMsgProcessor;

    @Mock
    private DestroyDerivativesProcessor destroyCollectionSrcImgProcessor;

    @Mock
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @Mock
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    @TempDir
    public Path tmpFolder;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        destroyedMsgProcessor = new DestroyedMsgProcessor();
        var router = new DestroyDerivativesRouter();
        router.setDestroyedMsgProcessor(destroyedMsgProcessor);
        router.setDestroyAccessCopyProcessor(destroyAccessCopyProcessor);
        router.setDestroyFulltextProcessor(destroyFulltextProcessor);
        router.setDestroyDerivativesStreamCamel("direct:destroy.derivatives.stream");
        return router;
    }

    @Test
    public void routeRequestText() throws Exception {
        var imgDestroyEndpoint = getMockEndpoint("mock:direct:image.derivatives.destroy");
        imgDestroyEndpoint.expectedMessageCount(0);
        var fullTextDestroyEndpoint = getMockEndpoint("mock:direct:fulltext.derivatives.destroy");
        fullTextDestroyEndpoint.expectedMessageCount(1);

        createContext(DESTROY_DERIVATIVES_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        Path contentPath = tmpFolder.resolve("content.txt");
        var body = createMessage(Cdr.FileObject, "text/plain", TEST_ID, contentPath);
        template.sendBody(body);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        imgDestroyEndpoint.assertIsSatisfied();
        fullTextDestroyEndpoint.assertIsSatisfied();
    }

    @Test
    public void routeRequestImage() throws Exception {
        var imgDestroyEndpoint = getMockEndpoint("mock:direct:image.derivatives.destroy");
        imgDestroyEndpoint.expectedMessageCount(1);
        var fullTextDestroyEndpoint = getMockEndpoint("mock:direct:fulltext.derivatives.destroy");
        fullTextDestroyEndpoint.expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);

        Path contentPath = tmpFolder.resolve("content.png");
        var body = createMessage(Cdr.FileObject, "image/png", TEST_ID, contentPath);
        template.sendBody(body);

        imgDestroyEndpoint.assertIsSatisfied();
        fullTextDestroyEndpoint.assertIsSatisfied();
    }

    @Test
    public void routeNonBinary() throws Exception {
        var imgDestroyEndpoint = getMockEndpoint("mock:direct:image.derivatives.destroy");
        imgDestroyEndpoint.expectedMessageCount(0);
        var fullTextDestroyEndpoint = getMockEndpoint("mock:direct:fulltext.derivatives.destroy");
        fullTextDestroyEndpoint.expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);

        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.FileObject, "application", TEST_ID, contentPath);
        template.sendBody(body);

        imgDestroyEndpoint.assertIsSatisfied();
        fullTextDestroyEndpoint.assertIsSatisfied();
    }

    @Test
    public void destroyTextDerivative() throws Exception {
        createContext(DESTROY_FULLTEXT_ROUTE);

        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.FileObject, "text/plain", TEST_ID, contentPath);
        template.sendBody(body);

        verify(destroyFulltextProcessor).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyImageThumbnailDerivative() throws Exception {
        createContext(DESTROY_IMAGE_ROUTE);

        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.FileObject, "image/png", TEST_ID, contentPath);
        template.sendBody(body);

        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyImageThumbnailDerivativeCollection() throws Exception {
        AdviceWith.adviceWith(context, DESTROY_DERIVATIVES_ROUTE, a -> {
            a.replaceFromWith("direct:start");
        });

        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.Collection, "image/*", TEST_ID, contentPath);
        template.sendBody(body);

        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    // See if any messages are routed for object with no mimetype
    @Test
    public void destroyImageThumbnailNoDerivativeCollection() throws Exception {
        var imgDestroyEndpoint = getMockEndpoint("mock:direct:image.derivatives.destroy");
        imgDestroyEndpoint.expectedMessageCount(0);
        var fullTextDestroyEndpoint = getMockEndpoint("mock:direct:fulltext.derivatives.destroy");
        fullTextDestroyEndpoint.expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);
        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.Collection, "", TEST_ID, contentPath);
        template.sendBody(body);

        imgDestroyEndpoint.assertIsSatisfied();
        fullTextDestroyEndpoint.assertIsSatisfied();
    }

    @Test
    public void destroyImageAccessDerivative() throws Exception {
        createContext(DESTROY_IMAGE_ROUTE);

        Path contentPath = tmpFolder.resolve("content");
        var body = createMessage(Cdr.FileObject, "image/png", TEST_ID, contentPath);
        template.sendBody(body);

        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static String createMessage(Resource objType, String mimeType, String pidId, Path contentPath) {
        return "<message xmlns='" + JDOMNamespaceUtil.CDR_MESSAGE_NS.getURI() + "' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>"
                + "<objToDestroy>"
                    + "<objType>" + objType.getURI() + "</objType>"
                    + "<mimeType>" + mimeType + "</mimeType>"
                    + "<pidId>" + pidId + "</pidId>"
                    + "<contentUri>" + contentPath.toUri().toString() + "</contentUri>"
                + "</objToDestroy>"
                + "</message>";
    }
}
