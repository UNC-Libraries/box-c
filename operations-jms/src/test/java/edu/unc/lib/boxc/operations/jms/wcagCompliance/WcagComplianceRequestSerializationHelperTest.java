package edu.unc.lib.boxc.operations.jms.wcagCompliance;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WcagComplianceRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private final PID pid = PIDs.get(UUID.randomUUID().toString());
    private static final String LEVEL = "WCAG 1.0 Level A";

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new WcagComplianceRequest();
        request.setAgent(agent);
        request.setPidString(pid.getId());
        request.setLevel(LEVEL);

        var json = WcagComplianceRequestSerializationHelper.toJson(request);
        var helperRequest = WcagComplianceRequestSerializationHelper.toRequest(json);

        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getPidString(), helperRequest.getPidString());
        assertEquals(request.getLevel(), helperRequest.getLevel());
    }
}
