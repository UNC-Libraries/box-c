package edu.unc.lib.boxc.services.camel.accessSurrogates;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import edu.unc.lib.boxc.services.camel.images.ImageCacheInvalidationProcessor;
import edu.unc.lib.boxc.services.camel.images.ImageCacheInvalidationProcessorTest;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class AccessSurrogateRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;
    @BeanInject(value = "accessSurrogateRequestProcessor")
    private AccessSurrogateRequestProcessor processor;
    @BeanInject(value = "imageCacheInvalidationProcessor")
    private ImageCacheInvalidationProcessor imageCacheInvalidationProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/access-surrogates-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrAccessSurrogates");
        var pid = ProcessorTestHelper.makePid();

        var request = new AccessSurrogateRequest();
        request.setAgent(agent);
        request.setPidString(pid.toString());
        var body = AccessSurrogateRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
        verify(imageCacheInvalidationProcessor).process(any());
    }

    private void createContext(String routeName) throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });

        context.start();
    }
}
