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

import java.util.Arrays;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.services.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages editing of the dc:title property on an object
 *
 * @author harring
 *
 */
public class EditLabelService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;
    private TransactionManager txManager;
    private OperationsMessageSender operationsMessageSender;

    private static final Timer timer = TimerFactory.createTimerForClass(EditLabelService.class);

    public EditLabelService() {
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
                    "User does not have permissions to edit labels",
                    pid, agent.getPrincipals(), Permission.editDescription);

            RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);

            String oldLabel = getOldLabel(obj);

            repoObjFactory.createExclusiveRelationship(obj, DcElements.title, label);

            obj.getPremisLog()
                .buildEvent(Premis.Migration)
                .addImplementorAgent(agent.getUsernameUri())
                .addEventDetail("Object renamed from " + oldLabel + " to " + label)
                .write();
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

    private String getOldLabel(RepositoryObject obj) {
        String oldLabel = "no dc:title";
        Model objModel = obj.getModel();
        Resource resc = obj.getResource();
        if (objModel.contains(resc, DcElements.title)) {
            Statement s = objModel.getRequiredProperty(resc, DcElements.title);
            oldLabel = s.getLiteral().getString();
        }
        return oldLabel;
     }
}
