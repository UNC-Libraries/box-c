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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static edu.unc.lib.dl.util.DescriptionConstants.COLLECTION_NUMBER_EL;
import static edu.unc.lib.dl.util.DescriptionConstants.COLLECTION_NUMBER_LABEL;
import static edu.unc.lib.dl.util.DescriptionConstants.COLLECTION_NUMBER_TYPE;
import static java.util.Arrays.asList;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.jdom2.output.Format.getPrettyFormat;
import static org.springframework.util.Assert.notNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.ResourceType;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages the creation of containers
 *
 * @author harring
 *
 */
public class AddContainerService {
    private static final Logger log = LoggerFactory.getLogger(AddContainerService.class);

    private AccessControlService aclService;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;
    private OperationsMessageSender operationsMessageSender;
    private PatronAccessAssignmentService patronService;
    private StorageLocationManager storageLocationManager;
    private UpdateDescriptionService updateDescService;

    private static final Timer timer = TimerFactory.createTimerForClass(AddContainerService.class);

    /**
     * Creates a new container as a child of the given parent using the agent principals provided.
     *
     * @param addRequest request object containing the details of the container to create
     */
    public void addContainer(AddContainerRequest addRequest) {
        notNull(addRequest.getParentPid(), "A parent pid must be provided");
        notNull(addRequest.getContainerType(), "A type must be provided for the next container");
        PID parentPid = addRequest.getParentPid();
        AgentPrincipals agent = addRequest.getAgent();

        ContentContainerObject child = null;
        FedoraTransaction tx = txManager.startTransaction();

        try (Timer.Context context = timer.time()) {
            PID containerPid = PIDs.get(UUID.randomUUID().toString());
            Model containerModel = createDefaultModel();
            Resource containerResc = containerModel.createResource(containerPid.getRepositoryPath());
            containerResc.addLiteral(DcElements.title, addRequest.getLabel());

            StorageLocation storageLoc = storageLocationManager.getStorageLocation(parentPid);
            containerResc.addLiteral(Cdr.storageLocation, storageLoc.getId());
            log.debug("Adding new container to storage location {}", storageLoc.getId());

            ResourceType containerType = addRequest.getContainerType();
            // Create the appropriate container
            if (ResourceType.AdminUnit.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create admin units",
                        parentPid, agent.getPrincipals(), Permission.createAdminUnit);
                child = repoObjFactory.createAdminUnit(containerPid, containerModel);
            } else if (ResourceType.Collection.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create collections",
                        parentPid, agent.getPrincipals(), Permission.createCollection);
                child = repoObjFactory.createCollectionObject(containerPid, containerModel);
            } else if (ResourceType.Folder.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create folders",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createFolderObject(containerPid, containerModel);
            } else if (ResourceType.Work.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create works",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createWorkObject(containerPid, containerModel);
            } else {
                throw new AccessRestrictionException("User cannot add a container to object of type " + containerType);
            }

            ContentContainerObject parent = (ContentContainerObject) repoObjLoader.getRepositoryObject(parentPid);
            parent.addMember(child);

            if (addRequest.isStaffOnly() && !ResourceType.AdminUnit.equals(containerType)) {
                PatronAccessDetails accessDetails = new PatronAccessDetails();
                accessDetails.setRoles(asList(new RoleAssignment(PUBLIC_PRINC, none),
                        new RoleAssignment(AUTHENTICATED_PRINC, none)));
                patronService.updatePatronAccess(agent, containerPid, accessDetails, true);
            } else if (ResourceType.Collection.equals(containerType)) {
                PatronAccessDetails accessDetails = new PatronAccessDetails();
                accessDetails.setRoles(asList(new RoleAssignment(PUBLIC_PRINC, canViewOriginals),
                        new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals)));
                patronService.updatePatronAccess(agent, containerPid, accessDetails);
            }

            storeDescription(containerPid, addRequest);

            child.getPremisLog()
                .buildEvent(Premis.Creation)
                .addImplementorAgent(AgentPids.forPerson(agent))
                .addEventDetail("Container added at destination " + parentPid)
                .writeAndClose();
        } catch (Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

        // Send message that the action completed
        operationsMessageSender.sendAddOperation(agent.getUsername(), Arrays.asList(parentPid),
                Arrays.asList(child.getPid()), null, null);
    }

    private void storeDescription(PID containerPid, AddContainerRequest addRequest) throws IOException {
        Document doc = new Document();
        Element mods = new Element("mods", JDOMNamespaceUtil.MODS_V3_NS);
        doc.addContent(mods);

        mods.addContent(new Element("titleInfo", JDOMNamespaceUtil.MODS_V3_NS)
                .addContent(new Element("title", JDOMNamespaceUtil.MODS_V3_NS)
                        .setText(addRequest.getLabel().trim())));

        // Add in optional collection number field, only for collections
        if (ResourceType.Collection.equals(addRequest.getContainerType())
                && !StringUtils.isBlank(addRequest.getCollectionNumber())) {
            mods.addContent(new Element(COLLECTION_NUMBER_EL, JDOMNamespaceUtil.MODS_V3_NS)
                    .setAttribute("type", COLLECTION_NUMBER_TYPE)
                    .setAttribute("displayLabel", COLLECTION_NUMBER_LABEL)
                    .setText(addRequest.getCollectionNumber().trim()));
        }

        String modsString = new XMLOutputter(getPrettyFormat()).outputString(mods.getDocument());

        updateDescService.updateDescription(new UpdateDescriptionRequest(addRequest.getAgent(), containerPid,
                IOUtils.toInputStream(modsString, StandardCharsets.UTF_8)));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setPatronService(PatronAccessAssignmentService patronService) {
        this.patronService = patronService;
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
     *
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setStorageLocationManager(StorageLocationManager storageLocationManager) {
        this.storageLocationManager = storageLocationManager;
    }

    public void setUpdateDescriptionService(UpdateDescriptionService updateDescService) {
        this.updateDescService = updateDescService;
    }

    public static class AddContainerRequest {
        private PID parentPid;
        private String label;
        private Boolean staffOnly = false;
        private ResourceType containerType;
        private AgentPrincipals agent;
        private String collectionNumber;

        public void setId(String id) {
            this.setParentPid(PIDs.get(id));
        }

        public PID getParentPid() {
            return parentPid;
        }

        public void setParentPid(PID parentPid) {
            this.parentPid = parentPid;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Boolean isStaffOnly() {
            return staffOnly;
        }

        public void setStaffOnly(Boolean staffOnly) {
            this.staffOnly = staffOnly;
        }

        public String getCollectionNumber() {
            return collectionNumber;
        }

        public void setCollectionNumber(String collectionNumber) {
            this.collectionNumber = collectionNumber;
        }

        public ResourceType getContainerType() {
            return containerType;
        }

        public AddContainerRequest withContainerType(ResourceType containerType) {
            this.containerType = containerType;
            return this;
        }

        public AgentPrincipals getAgent() {
            return agent;
        }

        public AddContainerRequest withAgent(AgentPrincipals agent) {
            this.agent = agent;
            return this;
        }
    }
}
