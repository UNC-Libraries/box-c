package edu.unc.lib.boxc.operations.jms.datastreams;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Request to update the alt text, full description, or transcript datastream of a file object
 *
 * @author snluong
 */
public class DatastreamUpdateRequest {
    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String text;
    private BinaryTransferSession transferSession;
    private DatastreamType datastreamType;

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

    public BinaryTransferSession getTransferSession() {
        return transferSession;
    }

    public void setTransferSession(BinaryTransferSession transferSession) {
        this.transferSession = transferSession;
    }

    public DatastreamType getDatastreamType() {
        return datastreamType;
    }

    public void setDatastreamType(DatastreamType datastreamType) {
        this.datastreamType = datastreamType;
    }
}
