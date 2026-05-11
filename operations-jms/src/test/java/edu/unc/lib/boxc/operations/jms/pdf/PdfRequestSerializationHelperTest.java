package edu.unc.lib.boxc.operations.jms.pdf;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PdfRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private final PID pid = PIDs.get(UUID.randomUUID().toString());

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new PdfRequest();
        request.setAgent(agent);
        request.setWorkPid(pid.getId());
        request.setMimetype("image/tiff");

        var json = PdfRequestSerializationHelper.toJson(request);
        var helperRequest = PdfRequestSerializationHelper.toRequest(json);

        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getWorkPid(), helperRequest.getWorkPid());
        assertEquals(request.getMimetype(), helperRequest.getMimetype());
    }
}
