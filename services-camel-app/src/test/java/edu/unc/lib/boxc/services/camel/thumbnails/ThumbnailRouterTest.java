package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author snluong
 */
public class ThumbnailRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce("direct:start")
    protected ProducerTemplate template;
    @BeanInject(value = "thumbnailRequestProcessor")
    private ThumbnailRequestProcessor processor;

    @BeanInject(value = "importThumbnailRequestProcessor")
    private ImportThumbnailRequestProcessor importProcessor;
    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/thumbnails-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrThumbnails");
        var pid = TestHelper.makePid();

        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(pid.toString());
        var body = ThumbnailRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }

    @Test
    public void importRequestSentTest() throws Exception {
        createContext("DcrImportThumbnails");
        var pid = TestHelper.makePid();

        var request = new ImportThumbnailRequest();
        request.setAgent(agent);
        request.setPidString(pid.toString());

        var body = ImportThumbnailRequestSerializationHelper.toJson(request);
        var solrEndpoint = getMockEndpoint("mock:direct:solrIndexing");
        solrEndpoint.expectedMessageCount(1);
        var imageAccessCopyEndpoint = getMockEndpoint("mock:direct:process.enhancement.imageAccessCopy");
        imageAccessCopyEndpoint.expectedMessageCount(1);
        template.sendBody(body);

        verify(importProcessor).process(any());
        verify(importProcessor).cleanupTempThumbnailFile(any(Exchange.class));
        solrEndpoint.assertIsSatisfied();
        imageAccessCopyEndpoint.assertIsSatisfied();
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }
}
