package edu.unc.lib.boxc.operations.impl.machineGenerated;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for updating machine generated descriptions
 *
 * @author snluong
 */
public class MachineGenRequest {
    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String text;

    public String getPidString() {
        return pidString;
    }

    public void setPidString(String pidString) {
        this.pidString = pidString;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
