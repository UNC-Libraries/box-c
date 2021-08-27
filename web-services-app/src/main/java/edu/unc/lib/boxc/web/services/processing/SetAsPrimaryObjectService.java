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
package edu.unc.lib.boxc.web.services.processing;

import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;


/**
 * Service that manages setting a primary object on a work
 *
 * @author harring
 *
 */
public class SetAsPrimaryObjectService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;

    private static final Timer timer = TimerFactory.createTimerForClass(SetAsPrimaryObjectService.class);

    /**
     * Sets file object with the given pid as the primary object for its work using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param fileObjPid the file object to be set as primary object
     */
    public void setAsPrimaryObject(AgentPrincipals agent, PID fileObjPid) {
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("Insufficient privileges to set " + fileObjPid.getUUID() + " as primary object",
                    fileObjPid, agent.getPrincipals(), editResourceType);

            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(fileObjPid);

            if (!(repoObj instanceof FileObject)) {
                throw new InvalidOperationForObjectType("Cannot set " + fileObjPid.getUUID()
                        + " as primary object, since objects of type " + repoObj.getClass().getName()
                        + " are not eligible.");
            }

            RepositoryObject parent = repoObj.getParent();
            if (!(parent instanceof WorkObject)) {
                throw new InvalidOperationForObjectType("Object of type " + parent.getClass().getName()
                + " cannot have a primary object.");
            }
            WorkObject work = (WorkObject) parent;
            work.setPrimaryObject(fileObjPid);
            work.shouldRefresh();

            // Send message that the action completed
            operationsMessageSender.sendSetAsPrimaryObjectOperation(agent.getUsername(),
                    Arrays.asList(parent.getPid(), fileObjPid));
        }
    }

    /**
     * Clears the primary object for a work.
     * @param agent
     * @param objPid Either the pid of the work to clear the primary object for,
     *    or the pid of the primary object for the work.
     */
    public void clearPrimaryObject(AgentPrincipals agent, PID objPid) {
        try (Timer.Context context = timer.time()) {
            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(objPid);

            RepositoryObject parent;
            if (repoObj instanceof FileObject) {
                parent = repoObj.getParent();
            } else {
                parent = repoObj;
            }

            aclService.assertHasAccess("Insufficient privileges to clear primary object for "
                    + parent.getPid().getId(), parent.getPid(), agent.getPrincipals(), editResourceType);

            if (!(parent instanceof WorkObject)) {
                throw new InvalidOperationForObjectType("Object of type " + parent.getClass().getName()
                + " cannot have a primary object.");
            }

            WorkObject work = (WorkObject) parent;

            List<PID> updated = new ArrayList<>();
            updated.add(parent.getPid());
            // if there was a primary object before, capture its pid for reporting
            FileObject previousPrimary = work.getPrimaryObject();
            if (previousPrimary != null) {
                updated.add(previousPrimary.getPid());

                work.clearPrimaryObject();
                work.shouldRefresh();
            }

            // Send message that the action completed
            operationsMessageSender.sendSetAsPrimaryObjectOperation(agent.getUsername(), updated);
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

    /**
     * @param operationsMessageSender the message sender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

}
