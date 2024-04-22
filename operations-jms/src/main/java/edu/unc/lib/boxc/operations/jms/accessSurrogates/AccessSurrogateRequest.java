package edu.unc.lib.boxc.operations.jms.accessSurrogates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.nio.file.Path;

public class AccessSurrogateRequest {
    public static String SET = "set";
    public static String DELETE = "delete";
    private String pidString;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String action;
    private String mimetype;
    private Path filePath;

    public String getPidString() {
        return pidString;
    }

    public void setPidString(String pidString) {
        this.pidString = pidString;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }
}
