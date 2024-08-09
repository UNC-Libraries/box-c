package edu.unc.lib.boxc.operations.impl.download;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

public class DownloadBulkRequest {
    private String workPidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;

    public DownloadBulkRequest(String workPidString, AgentPrincipals agent) {
        this.workPidString = workPidString;
        this.agent = agent;
    }

    public String getWorkPidString() {
        return workPidString;
    }

    public void setWorkPidString(String workPidString) {
        this.workPidString = workPidString;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }
}
