package edu.unc.lib.boxc.operations.jms.collectionDisplay;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesSerializationHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionDisplayPropertiesRequestSerializationHelperTest {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private final PID pid = PIDs.get(UUID.randomUUID().toString());

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new CollectionDisplayPropertiesRequest();
        request.setAgent(agent);
        request.setId(pid.getId());
        request.setSortType("default,normal");
        request.setWorksOnly(true);
        request.setDisplayType("gallery");;

        var json = CollectionDisplayPropertiesSerializationHelper.toJson(request);
        var helperRequest = CollectionDisplayPropertiesSerializationHelper.toRequest(json);

        assertEquals(request.getAgent().getPrincipals(), helperRequest.getAgent().getPrincipals());
        assertEquals(request.getId(), helperRequest.getId());
        assertEquals(request.getSortType(), helperRequest.getSortType());
        assertEquals(request.getWorksOnly(), helperRequest.getWorksOnly());
        assertEquals(request.getDisplayType(), helperRequest.getDisplayType());
    }
}
