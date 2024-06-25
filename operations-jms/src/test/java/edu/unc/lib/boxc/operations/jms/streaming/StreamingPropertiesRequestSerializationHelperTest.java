package edu.unc.lib.boxc.operations.jms.streaming;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.ADD;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.STREAMREAPER_PREFIX_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamingPropertiesRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private final PID pid = PIDs.get(UUID.randomUUID().toString());

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new StreamingPropertiesRequest();
        request.setAgent(agent);
        request.setId(pid.getId());
        request.setUrl(STREAMREAPER_PREFIX_URL + "?params=more");
        request.setAction(ADD);
        request.setType("video");

        var json = StreamingPropertiesRequestSerializationHelper.toJson(request);
        var helperRequest = StreamingPropertiesRequestSerializationHelper.toRequest(json);

        assertEquals(request.getAction(), helperRequest.getAction());
        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getUrl(), helperRequest.getUrl());
        assertEquals(request.getId(), helperRequest.getId());
    }
}
