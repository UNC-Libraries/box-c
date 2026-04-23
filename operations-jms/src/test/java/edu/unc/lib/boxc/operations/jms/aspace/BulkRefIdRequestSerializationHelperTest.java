package edu.unc.lib.boxc.operations.jms.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkRefIdRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    @Test
    public void testHelperSuccess() throws IOException {
        Map<String, String> map = new HashMap<>();
        var id = UUID.randomUUID().toString();
        map.put(id, "ref ID 1");
        var request = new BulkRefIdRequest();
        request.setAgent(agent);
        request.setRefIdMap(map);
        request.setEmail("user@email.com");

        var json = BulkRefIdRequestSerializationHelper.toJson(request);
        var helperRequest = BulkRefIdRequestSerializationHelper.toRequest(json);

        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getRefIdMap(), helperRequest.getRefIdMap());
        assertEquals(request.getEmail(), helperRequest.getEmail());
    }
}
