package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSerializationHelper;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PdfEnhancementsRouterTest extends CamelSpringTestSupport {
    private final PID workPid = PIDs.get(UUID.randomUUID().toString());
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    @Produce("direct:start")
    protected ProducerTemplate template;
    private String endpointUri = "direct:aggregatePdf";

    @Mock
    private AggregatePdfProcessor aggregatePdfProcessor;

    @Override
    protected RouteBuilder createRouteBuilder() {
        var router = new PdfEnhancementsRouter();
        router.setAggregatePdfProcessor(aggregatePdfProcessor);
        router.setAggregatePdfStreamCamel(endpointUri);
        return router;
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/pdf-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("AggregatePdf");

        var request = new PdfRequest();
        request.setAgent(agent);
        request.setMimetype("image/tiff");
        request.setWorkPid(workPid.getId());
        var body = PdfRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(aggregatePdfProcessor).process(any());
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }
}
