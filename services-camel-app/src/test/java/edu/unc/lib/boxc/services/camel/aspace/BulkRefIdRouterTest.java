package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class BulkRefIdRouterTest extends CamelSpringTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeanInject("bulkRefIdRequestProcessor")
    private BulkRefIdRequestProcessor processor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/bulk-ref-id-context.xml");
    }

    @Test
    public void requestSentTest() throws Exception {
        var pid = TestHelper.makePid();
        Map<String, String> map = new HashMap<>();
        map.put(pid.getId(), "2817ec3c77e5ea9846d5c070d58d402b");

        TestHelper.createContext(context, "DcrBulkRefId");
        var request = new BulkRefIdRequest();
        request.setAgent(agent);
        request.setRefIdMap(map);

        var body = BulkRefIdRequestSerializationHelper.toJson(request);
        template.sendBody(body);
        verify(processor).process(any());
    }
}
