package edu.unc.lib.boxc.services.camel.views;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.operations.jms.views.ViewBehaviorRequest;
import edu.unc.lib.boxc.operations.jms.views.ViewBehaviorRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
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

/**
 * @author snluong
 */
public class ViewBehaviorRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @BeanInject(value = "viewBehaviorRequestProcessor")
    private ViewBehaviorRequestProcessor processor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/view-behavior-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrViewBehavior");
        var pid = ProcessorTestHelper.makePid();

        var request = new ViewBehaviorRequest();
        request.setAgent(agent);
        request.setObjectPidString(pid.toString());
        var body = ViewBehaviorRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
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
