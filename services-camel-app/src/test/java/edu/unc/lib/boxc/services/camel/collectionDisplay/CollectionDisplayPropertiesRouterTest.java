package edu.unc.lib.boxc.services.camel.collectionDisplay;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.StreamingConstants;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesSerializationHelper;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import edu.unc.lib.boxc.services.camel.streaming.StreamingPropertiesRequestProcessor;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class CollectionDisplayPropertiesRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeanInject("collectionDisplayPropertiesRequestProcessor")
    private CollectionDisplayPropertiesRequestProcessor processor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/collection-display-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        TestHelper.createContext(context, "DcrCollectionDisplayProperties");
        var pid = TestHelper.makePid();

        var request = new CollectionDisplayPropertiesRequest();
        request.setAgent(agent);
        request.setId(pid.getId());
        request.setSortType("default,normal");
        request.setDisplayType("gallery-view");
        request.setWorksOnly(true);
        var body = CollectionDisplayPropertiesSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }
}
