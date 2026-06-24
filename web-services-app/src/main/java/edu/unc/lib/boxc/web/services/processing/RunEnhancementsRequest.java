package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;

import java.util.List;

/**
 * @author bbpennel
 */
public class RunEnhancementsRequest {
    private List<String> pids;
    private boolean force;
    private AgentPrincipals agent;
    private boolean recursive;
    private List<String> enhancements;

    public List<String> getPids() {
        return pids;
    }

    public boolean isForce() {
        return force;
    }

    public void setPids(List<String> pids) {
        this.pids = pids;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public List<String> getEnhancements() {
        return enhancements;
    }

    public void setEnhancements(List<String> enhancements) {
        this.enhancements = enhancements;
    }
}
