package edu.unc.lib.boxc.operations.jms.destroy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * A request to destroy one or more objects.
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRequest {

    private String jobId;
    private String[] ids;
    private String username;
    @JsonDeserialize(as = AccessGroupSetImpl.class)
    private AccessGroupSet principals;
    private boolean completely;

    public DestroyObjectsRequest() {
    }

    public DestroyObjectsRequest(String jobId, AgentPrincipals agent, String... ids) {
        this.username = agent.getUsername();
        this.principals = agent.getPrincipals();
        this.ids = ids;
        this.jobId = jobId;
    }

    @JsonIgnore
    public AgentPrincipals getAgent() {
        return new AgentPrincipalsImpl(username, principals);
    }

    public AccessGroupSet getPrincipals() {
        return principals;
    }

    public void setPrincipals(AccessGroupSet principals) {
        this.principals = principals;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public boolean isDestroyCompletely() {
        return completely;
    }

    public void setDestroyCompletely(boolean completely) {
        this.completely = completely;
    }
}
