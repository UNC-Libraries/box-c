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
package edu.unc.lib.dl.persist.services.edit;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import io.dropwizard.metrics5.Timer;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Service that manages editing of the mods:title property on an object
 *
 * @author smithjp
 *
 */
public class EditTitleService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;

    private static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public EditTitleService() {
    }

    /**
     * Changes an object's mods:title
     *
     * @param agent security principals of the agent making request
     * @param pid the pid of the object whose label is to be changed
     * @param title the new label (dc:title) of the given object
     */
    public void editTitle(AgentPrincipals agent, PID pid, String title) throws Exception {
        log.info(pid.toString());
        log.info(agent.getUsername());
        log.info(agent.getPrincipals().toString());

        aclService.assertHasAccess(
                "User does not have permissions to edit titles",
                pid, agent.getPrincipals(), Permission.editDescription);

        log.info("access checked");

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        log.info("object loaded");
        BinaryObject mods = obj.getDescription();
        log.info("mods fetched");

        String username = agent.getUsername();

        if (mods != null) {
            log.info(mods.toString());
            String oldTitle = getOldTitle(mods);
            // if old title is null, add titleInfo/title to mods
            if (oldTitle != null) {
                log.info("mods has title element: "+oldTitle);
                // update title
            } else {
                log.info("mods needs title element");
                // add titleInfo/title to mods
            }
        } else {
            log.info("null mods");
            // else create new mods stream
            createMODS(title);
        }

        // Send message that the action completed
//        operationsMessageSender.sendUpdateDescriptionOperation(agent.getUsername(), Arrays.asList(pid));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public RepositoryObjectLoader getRepoObjLoader() {
        return repoObjLoader;
    }

    /**
     *
     * @param repoObjLoader
     */
    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     *
     * @param mods the mods record to be edited
     * @return current title property
     */
    private String getOldTitle(BinaryObject mods) throws Exception {
        String oldTitle = null;
        InputStream modsStream = mods.getBinaryStream();
        String modsString = IOUtils.toString(modsStream, StandardCharsets.UTF_8);
        log.info(modsString);
        // find title

        return oldTitle;
    }

    private String createMODS(String title) {

        // create mods

        return "mods with new title";
    }
}
