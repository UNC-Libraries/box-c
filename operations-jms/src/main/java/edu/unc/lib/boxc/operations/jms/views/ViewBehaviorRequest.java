package edu.unc.lib.boxc.operations.jms.views;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for updating the view behavior of the UV
 * @author sharonluong
 */
public class ViewBehaviorRequest {
    private String objectPidString;
    private ViewBehavior viewBehavior;

    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    public enum ViewBehavior {
        INDIVIDUALS,
        PAGED,
        CONTINUOUS
    }

    public String getObjectPidString() {
        return objectPidString;
    }

    public void setObjectPidString(String objectPidString) {
        this.objectPidString = objectPidString;
    }

    public ViewBehavior getViewBehavior() {
        return viewBehavior;
    }

    public void setViewBehavior(ViewBehavior viewBehavior) {
        this.viewBehavior = viewBehavior;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }
}
