package edu.unc.lib.boxc.operations.jms.streaming;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class StreamingPropertiesRequest {
    public static final String DURACLOUD = "duracloud";
    public static final String OPEN = "open-hls";
    public static final String CLOSED = "closed-hls";
    public static final String CAMPUS = "campus-hls";
    public static Set<String> VALID_FOLDERS = new HashSet<>(asList(OPEN, CLOSED, CAMPUS));
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String filePidString;
    private String filename;
    private String folder;
    private String host = DURACLOUD;



    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getFilePidString() {
        return filePidString;
    }

    public void setFilePidString(String filePidString) {
        this.filePidString = filePidString;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
