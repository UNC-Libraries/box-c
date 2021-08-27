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
package edu.unc.lib.boxc.operations.impl.edit;

import java.util.Arrays;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages editing of the ebucore:filename property on an object
 *
 * @author harring
 *
 */
public class EditFilenameService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;
    private TransactionManager txManager;
    private OperationsMessageSender operationsMessageSender;
    private PremisLoggerFactory premisLoggerFactory;

    private static final Timer timer = TimerFactory.createTimerForClass(EditFilenameService.class);

    public EditFilenameService() {
    }

    /**
     * Changes an object's dc:title and creates a premis event marking the change
     *
     * @param agent security principals of the agent making request
     * @param pid the pid of the object whose label is to be changed
     * @param label the new label (dc:title) of the given object
     */
    public void editLabel(AgentPrincipals agent, PID pid, String label) {
        FedoraTransaction tx = txManager.startTransaction();

        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess(
                    "User does not have permissions to edit filenames",
                    pid, agent.getPrincipals(), Permission.editDescription);

            RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);

            if (!(obj instanceof FileObject)) {
                throw new IllegalArgumentException("Failed to edit filename for " + obj.getPid());
            }

            BinaryObject binaryObj = ((FileObject) obj).getOriginalFile();
            String oldLabel = getOldLabel(binaryObj.getFilename());

            repoObjFactory.createExclusiveRelationship(binaryObj, Ebucore.filename, label);
            repoObjFactory.createExclusiveRelationship(obj, DcElements.title, label);

            premisLoggerFactory.createPremisLogger(obj)
                .buildEvent(Premis.FilenameChange)
                .addImplementorAgent(AgentPids.forPerson(agent))
                .addEventDetail("Object renamed from " + oldLabel + " to " + label)
                .writeAndClose();
        } catch (Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

        // Send message that the action completed
        operationsMessageSender.sendUpdateDescriptionOperation(
                agent.getUsername(), Arrays.asList(pid));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repoObjFactory the factory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param txManager the transaction manager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    /**
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    private String getOldLabel(String filename) {
        String oldLabel = "no ebucore:filename";

        if (filename != null) {
            oldLabel = filename;
        }

        return  oldLabel;
    }
}
