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
package edu.unc.lib.dl.cdr.services.processing;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.update.AtomPubMetadataUIP;
import edu.unc.lib.dl.update.MODSValidationUIPFilter;
import edu.unc.lib.dl.update.UIPException;
import edu.unc.lib.dl.update.UpdateOperation;

/**
 * Service that manages MODS description updates
 *
 * @author harring
 *
 */
public class UpdateMODSService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;
    private MODSValidationUIPFilter modsFilter;

    public void updateMODS(AgentPrincipals agent, PID pid, InputStream modsStream)
            throws FileNotFoundException, UIPException {
        aclService.assertHasAccess("User does not have permissions to update description",
                pid, agent.getPrincipals(), Permission.editDescription);

        String username = agent.getUsername();
        validateMODS(modsStream, pid, username);

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        obj.addDescription(modsStream);

        operationsMessageSender.sendUpdateMODSOperation(username, Arrays.asList(pid));

    }

    private void validateMODS(InputStream modsStream, PID pid, String username) throws UIPException {
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(modsStream);
        Entry entry = entryDoc.getRoot();
        AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, username,
                UpdateOperation.UPDATE, entry);
        modsFilter.doFilter(uip);
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     *
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
    *
    * @param MODSValidationUIPFilter modsFilter
    */
   public void setMODSValidationUIPFilter(MODSValidationUIPFilter modsFilter) {
       this.modsFilter = modsFilter;
   }

}
