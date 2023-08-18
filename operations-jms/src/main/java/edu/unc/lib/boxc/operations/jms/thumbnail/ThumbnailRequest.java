package edu.unc.lib.boxc.operations.jms.thumbnail;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for setting a file as thumbnail for a work
 *
 * @author snluong
 */
public class ThumbnailRequest {
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String filePid;

    public AgentPrincipals getAgent() { return agent; }

    public void setAgent(AgentPrincipals agent) { this.agent = agent; }

    public String getFilePid() { return filePid; }

    public void setFilePid(String fileId) { this.filePid = filePid; }
}
