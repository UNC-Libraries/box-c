package edu.unc.lib.boxc.services.camel.viewSettings;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
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
public class ViewSettingRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @BeanInject(value = "viewSettingRequestProcessor")
    private ViewSettingRequestProcessor processor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/view-setting-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        TestHelper.createContext(context, "DcrViewSetting");
        var pid = TestHelper.makePid();

        var request = new ViewSettingRequest();
        request.setAgent(agent);
        request.setObjectPidString(pid.toString());
        var body = ViewSettingRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }
}
