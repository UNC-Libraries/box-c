package edu.unc.lib.boxc.operations.jms.thumbnails;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.nio.file.Path;

/**
 * Request object for importing an image as thumbnail for a collection, folder, or admin unit
 *
 * @author snluong
 */
public class ImportThumbnailRequest {
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String mimetype;
    private Path storagePath;
    private String pidString;

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

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }
}
