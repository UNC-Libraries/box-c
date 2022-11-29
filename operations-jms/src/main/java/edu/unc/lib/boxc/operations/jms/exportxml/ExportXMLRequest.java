package edu.unc.lib.boxc.operations.jms.exportxml;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;

/**
 * A request object for a bulk XML export
 *
 * @author bbpennel
 */
public class ExportXMLRequest {
    private List<String> pids;
    private boolean exportChildren;
    private String email;
    private Instant requestedTimestamp;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private boolean onlyIncludeValidDatastreams;
    private Set<DatastreamType> datastreams;

    public ExportXMLRequest() {
    }

    public List<String> getPids() {
        return pids;
    }

    public void setPids(List<String> pids) {
        this.pids = pids;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean getExportChildren() {
        return exportChildren;
    }

    public void setExportChildren(boolean exportChildren) {
        this.exportChildren = exportChildren;
    }

    /**
     * @return Time at which this request was generated
     */
    public Instant getRequestedTimestamp() {
        return requestedTimestamp;
    }

    public void setRequestedTimestamp(Instant requestedTimestamp) {
        this.requestedTimestamp = requestedTimestamp;
    }

    /**
     * @return The agent making this request
     */
    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    /**
     * @return True if objects which return no datastreams should be excluded from the export
     */
    public boolean getOnlyIncludeValidDatastreams() {
        return onlyIncludeValidDatastreams;
    }

    public void setOnlyIncludeValidDatastreams(boolean onlyIncludeValidDatastreams) {
        this.onlyIncludeValidDatastreams = onlyIncludeValidDatastreams;
    }

    /**
     * @return List of datastreams to be exported
     */
    public Set<DatastreamType> getDatastreams() {
        return datastreams;
    }

    public void setDatastreams(Set<DatastreamType> datastreams) {
        if (datastreams == null || datastreams instanceof EnumSet) {
            this.datastreams = datastreams;
        } else {
            if (datastreams.isEmpty()) {
                this.datastreams = EnumSet.noneOf(DatastreamType.class);
            } else {
                this.datastreams = EnumSet.copyOf(datastreams);
            }
        }
    }
}
