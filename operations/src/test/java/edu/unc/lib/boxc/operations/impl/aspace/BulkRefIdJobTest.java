package edu.unc.lib.boxc.operations.impl.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdJobTest {
    private static final String WORK_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private AutoCloseable closeable;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private RefIdService service;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void updateRefIds() {
        Map<String, String> map = new HashMap<>();
        map.put(WORK_UUID, "ref ID 1");
        map.put(WORK2_UUID, "ref ID 2");
        var bulkRequest = new BulkRefIdRequest();
        bulkRequest.setAgent(agent);
        bulkRequest.setRefIdMap(map);

        var job = new BulkRefIdJob();
        job.setRequest(bulkRequest);
        job.setService(service);
        job.run();

        verify(service, times(2)).updateRefId(any());
    }
}
