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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages the retrieval of an object's MODS description, if available
 *
 * @author harring
 *
 */
public class MODSRetrievalService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;

    private static final Timer timer = TimerFactory.createTimerForClass(MODSRetrievalService.class);

    public MODSRetrievalService() {
    }

    /**
     * Retrieves the MODS description of the object whose pid is given
     *
     * @param agent security principals of the agent making the request
     * @param pid the pid of the object whose MODS is requested
     * @return the MODS as a String, or null if the object has no MODS
     * @throws FedoraException
     * @throws IOException
     */
    public String retrieveMODS(AgentPrincipals agent, PID pid)
            throws FedoraException, IOException {

        aclService.assertHasAccess("User does not have permissions to view MODS", pid, agent.getPrincipals(),
                Permission.viewMetadata);

        try (Timer.Context context = timer.time()) {
            ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
            BinaryObject mods = obj.getDescription();

            if (mods != null) {
                try (InputStream modsStream = mods.getBinaryStream()) {
                    String modsString = IOUtils.toString(modsStream, StandardCharsets.UTF_8);
                    return modsString;
                }
            } else {
                return null;
            }
        }
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
}
