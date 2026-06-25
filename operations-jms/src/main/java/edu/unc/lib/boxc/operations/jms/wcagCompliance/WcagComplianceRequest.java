package edu.unc.lib.boxc.operations.jms.wcagCompliance;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;

/**
 * Request for updating WCAG Compliance level of a FileObject
 */
public class WcagComplianceRequest {
    private AgentPrincipals agent;
    private String pidString;
    private String level;

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getPidString() {
        return pidString;
    }

    public void setPidString(String pidString) {
        this.pidString = pidString;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
