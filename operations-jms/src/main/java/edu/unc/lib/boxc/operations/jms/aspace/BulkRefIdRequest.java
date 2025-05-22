package edu.unc.lib.boxc.operations.jms.aspace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.util.Map;

public class BulkRefIdRequest {
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private Map<String, String> refIdMap;

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public Map<String, String> getRefIdMap() {
        return refIdMap;
    }

    public void setRefIdMap(Map<String, String> refIdMap) {
        this.refIdMap = refIdMap;
    }
}
