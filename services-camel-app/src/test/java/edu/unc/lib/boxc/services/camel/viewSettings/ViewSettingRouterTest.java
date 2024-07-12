package edu.unc.lib.boxc.services.camel.viewSettings;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author snluong
 */
@ExtendWith(MockitoExtension.class)
public class ViewSettingRouterTest extends CamelTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Mock
    private ViewSettingRequestProcessor processor;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var router = new ViewSettingRouter();
        router.setViewSettingRequestProcessor(processor);
        router.setViewSettingStreamCamel("direct:start");
        return router;
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
