package edu.unc.lib.boxc.operations.jms.pdf;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Request to create pdf derivatives for a work
 *
 * @author krwong
 */
public class PdfRequest {
    private PID workPid;
    private String mimetype;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;

    /**
     * @return work object
     */
    public PID getWorkPid() {
        return workPid;
    }

    public void setWorkPid(PID workPid) {
        this.workPid = workPid;
    }

    /**
     * @return work object mimetype
     */
    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }
}
