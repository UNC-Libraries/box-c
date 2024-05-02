package edu.unc.lib.boxc.operations.jms.streaming;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Request object for updating the streaming properties of a FileObject
 */
public class StreamingPropertiesRequest {
    public static final String STREAMREAPER_PREFIX_URL = "https://durastream.lib.unc.edu/player";
    public static final String DURACLOUD = "duracloud";
    public static final String OPEN = "open-hls";
    public static final String CLOSED = "closed-hls";
    public static final String CAMPUS = "campus-hls";
    public static String ADD = "add";
    public static String DELETE = "delete";
    public static Set<String> VALID_FOLDERS = new HashSet<>(asList(OPEN, CLOSED, CAMPUS));
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
