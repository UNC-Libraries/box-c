package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.impl.aspace.BulkRefIdJob;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdRequestProcessorTest {
    private static final String WORK_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    private BulkRefIdRequestProcessor processor;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Mock
    private RefIdService service;
    @Mock
    private BulkRefIdNotificationService bulkRefIdNotificationService;
    private Map<String, String> map;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new BulkRefIdRequestProcessor();
        processor.setRefIdService(service);
        processor.setBulkRefIdNotificationService(bulkRefIdNotificationService);
        map = new HashMap<>();
        map.put(WORK_UUID, "ref ID 1");

    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testBulkRefIdProcessor() throws Exception {
        var exchange = createRequestExchange();
        processor.process(exchange);

        verify(service).updateRefId(any());
        verify(bulkRefIdNotificationService).sendResults(any(), any(), any());
    }

    private Exchange createRequestExchange() throws IOException {
        var request = new BulkRefIdRequest();
        request.setRefIdMap(map);
        request.setAgent(agent);

        return TestHelper.mockExchange(BulkRefIdRequestSerializationHelper.toJson(request));
    }
}
