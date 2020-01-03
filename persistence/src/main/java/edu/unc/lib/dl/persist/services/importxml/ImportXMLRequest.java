/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.persist.services.importxml;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;

/**
 * Request to perform a bulk import of descriptive metadata
 *
 * @author bbpennel
 *
 */
public class ImportXMLRequest {
    private String userEmail;
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
        return new AgentPrincipals(username, principals);
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
