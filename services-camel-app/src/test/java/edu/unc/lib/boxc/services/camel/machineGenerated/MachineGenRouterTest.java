package edu.unc.lib.boxc.services.camel.machineGenerated;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MachineGenRouterTest extends CamelTestSupport {
    private static final String FILE_ID = "343b3da4-8876-42f5-8821-7aabb65e0f19";

    @Produce("direct:start")
    protected ProducerTemplate template;
    @Mock
    private MachineGenDescriptionProcessor processor;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var router = new MachineGenRouter();
        router.setMachineGenDescriptionProcessor(processor);
        router.setMachineGenDescriptionStreamCamel("direct:start");
        return router;
    }

    @Test
    public void requestSentTest() throws Exception {
        when(processor.needsRun(any())).thenReturn(true);
        TestHelper.createContext(context,"MachineGenDescription");
        template.sendBodyAndHeaders("", createHeaders());

        verify(processor).process(any());
    }

    @Test
    public void requestSentRunNotNeededTest() throws Exception {
        when(processor.needsRun(any())).thenReturn(false);
        TestHelper.createContext(context,"MachineGenDescription");
        template.sendBodyAndHeaders("", createHeaders());

        verify(processor, never()).process(any());
    }

    private Map<String, Object> createHeaders() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, FILE_ID);
        headers.put(FcrepoJmsConstants.EVENT_TYPE, "ResourceCreation");
        headers.put(FcrepoJmsConstants.IDENTIFIER, "original_file");
        headers.put(FcrepoJmsConstants.RESOURCE_TYPE, Binary.getURI());
        headers.put(CdrBinaryMimeType, "audio/wav");

        return headers;
    }
}
