package edu.unc.lib.boxc.operations.jms.viewSettings;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for updating the view settings of Clover
 * @author sharonluong
 */
public class ViewSettingRequest {
    private String objectPidString;
    private ViewBehavior viewBehavior;

    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    public enum ViewBehavior {
        INDIVIDUALS,
        PAGED,
        CONTINUOUS;

        public String getString() {
            return this.name().toLowerCase();
        }

        public static ViewBehavior caseInsensitiveValueOf(String behavior) {
            return valueOf(behavior.toUpperCase());
        }
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
