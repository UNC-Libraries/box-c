package edu.unc.lib.boxc.operations.jms.accessSurrogates;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessSurrogateRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private final PID pid = PIDs.get(UUID.randomUUID().toString());
    @Mock
    private Path path;

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new AccessSurrogateRequest();
        request.setPidString(pid.getId());
        request.setFilePath(path);
        request.setAgent(agent);
        request.setAction(DELETE);
        request.setMimetype("image/jpeg");

        var json = AccessSurrogateRequestSerializationHelper.toJson(request);
        var helperRequest = AccessSurrogateRequestSerializationHelper.toRequest(json);

        assertEquals(request.getAction(), helperRequest.getAction());
        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getFilePath(), helperRequest.getFilePath());
        assertEquals(request.getMimetype(), helperRequest.getMimetype());
        assertEquals(request.getPidString(), helperRequest.getPidString());
    }
}
