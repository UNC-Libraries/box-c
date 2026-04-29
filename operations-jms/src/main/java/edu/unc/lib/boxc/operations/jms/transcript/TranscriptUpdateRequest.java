package edu.unc.lib.boxc.operations.jms.transcript;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Request to update the user-provided or reviewed transcript of a file object
 *
 * @author snluong
 */
public class TranscriptUpdateRequest {
    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String transcriptText;
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

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public BinaryTransferSession getTransferSession() {
        return transferSession;
    }

    public void setTransferSession(BinaryTransferSession transferSession) {
        this.transferSession = transferSession;
    }
}
