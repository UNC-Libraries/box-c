package edu.unc.lib.boxc.services.camel.streaming;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.StreamingConstants;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class StreamingPropertiesRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeanInject(value = "streamingPropertiesRequestProcessor")
    private StreamingPropertiesRequestProcessor processor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/streaming-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        TestHelper.createContext(context, "DcrStreaming");
        var pid = TestHelper.makePid();

        var request = new StreamingPropertiesRequest();
        request.setAgent(agent);
        request.setId(pid.getId());
        request.setUrl(StreamingConstants.STREAMREAPER_PREFIX_URL);
        request.setType("video");
        var body = StreamingPropertiesRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }
}
