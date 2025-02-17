package edu.unc.lib.boxc.operations.jms.altText;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Request to update the alt text of a file object
 *
 * @author bbpennel
 */
public class AltTextUpdateRequest {
    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String altText;
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

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public BinaryTransferSession getTransferSession() {
        return transferSession;
    }

    public void setTransferSession(BinaryTransferSession transferSession) {
        this.transferSession = transferSession;
    }
}
