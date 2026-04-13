package edu.unc.lib.boxc.operations.jms.fullDescription;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Request to update the user-provided full description of a file object
 *
 * @author snluong
 */
public class FullDescriptionUpdateRequest {

    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String fullDescriptionText;
    private BinaryTransferSession transferSession;

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

    public String getFullDescriptionText() {
        return fullDescriptionText;
    }

    public void setFullDescriptionText(String fullDescriptionText) {
        this.fullDescriptionText = fullDescriptionText;
    }

    public BinaryTransferSession getTransferSession() {
        return transferSession;
    }

    public void setTransferSession(BinaryTransferSession transferSession) {
        this.transferSession = transferSession;
    }
}
