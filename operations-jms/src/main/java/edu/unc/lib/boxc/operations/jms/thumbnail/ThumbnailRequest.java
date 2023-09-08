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
    public static String ASSIGN = "assign";
    public static String DELETE = "delete";
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String pidString;
    private String action;

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getPidString() {
        return pidString;
    }

    public void setPidString(String filePidString) {
        this.pidString = filePidString;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
