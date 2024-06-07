package edu.unc.lib.boxc.operations.jms.streaming;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request object for updating the streaming properties of a FileObject
 */
public class StreamingPropertiesRequest {
    public static final String STREAMREAPER_PREFIX_URL = "https://durastream.lib.unc.edu/player";
    public static String ADD = "add";
    public static String DELETE = "delete";
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String id;
    private String action;
    private String url;



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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
