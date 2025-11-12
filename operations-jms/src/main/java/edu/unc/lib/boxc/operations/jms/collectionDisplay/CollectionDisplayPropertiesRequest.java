package edu.unc.lib.boxc.operations.jms.collectionDisplay;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for updating the public UI display properties of a CollectionObject
 */
public class CollectionDisplayPropertiesRequest {
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String id;
    private String sortType;
    private boolean worksOnly;
    private String displayType;

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSortType() { return sortType; }

    public void setSortType(String sortType) { this.sortType = sortType; }

    public boolean getWorksOnly() { return worksOnly; }

    public void setWorksOnly(boolean worksOnly) { this.worksOnly = worksOnly; }

    public String getDisplayType() { return displayType; }

    public void setDisplayType(String displayType) { this.displayType = displayType; }
}