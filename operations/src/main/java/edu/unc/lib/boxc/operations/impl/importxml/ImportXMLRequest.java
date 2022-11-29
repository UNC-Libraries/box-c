package edu.unc.lib.boxc.operations.impl.importxml;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

/**
 * Request to perform a bulk import of descriptive metadata
 *
 * @author bbpennel
 *
 */
public class ImportXMLRequest {
    private String userEmail;
    @JsonDeserialize(as = AccessGroupSetImpl.class)
    private AccessGroupSet principals;
    private String username;
    private File importFile;

    public ImportXMLRequest() {
    }

    public ImportXMLRequest(String userEmail, AgentPrincipals agent, File importFile) {
        this.userEmail = userEmail;
        this.username = agent.getUsername();
        this.principals = agent.getPrincipals();
        this.importFile = importFile;
    }

    /**
     * @return the userEmail
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * @param userEmail the userEmail to set
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     * @return the agent
     */
    @JsonIgnore
    public AgentPrincipals getAgent() {
        return new AgentPrincipalsImpl(username, principals);
    }

    /**
     * @return the principals
     */
    public AccessGroupSet getPrincipals() {
        return principals;
    }

    /**
     * @param principals the principals to set
     */
    public void setPrincipals(AccessGroupSet principals) {
        this.principals = principals;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the importFile
     */
    public File getImportFile() {
        return importFile;
    }

    /**
     * @param importFile the importFile to set
     */
    public void setImportFile(File importFile) {
        this.importFile = importFile;
    }
}
