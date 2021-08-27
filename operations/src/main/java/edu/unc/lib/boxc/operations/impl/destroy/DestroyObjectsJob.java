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
package edu.unc.lib.boxc.operations.impl.destroy;

import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.METADATA_CONTAINER;
import static edu.unc.lib.boxc.operations.impl.destroy.ServerManagedProperties.isServerManagedProperty;
import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.assertCanDestroy;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import io.dropwizard.metrics5.Timer;

/**
 * A job for removing objects from the repository and replacing them with tombstones
 *
 * @author harring
 *
 */
public class DestroyObjectsJob extends AbstractDestroyObjectsJob {
    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsJob.class);

    private static final Timer timer = TimerFactory.createTimerForClass(DestroyObjectsJob.class);

    private List<String> deletedObjIds = new ArrayList<>();

    private ObjectPathFactory pathFactory;
    private InheritedAclFactory inheritedAclFactory;

    public DestroyObjectsJob(DestroyObjectsRequest request) {
        super(request);
    }

    @Override
    public void run() {
        FedoraTransaction tx = txManager.startTransaction();
        try (Timer.Context context = timer.time()) {
            // convert each destroyed obj to a tombstone
            for (PID pid : objsToDestroy) {
                RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);

                assertCanDestroy(agent, repoObj, aclService);
                if (!inheritedAclFactory.isMarkedForDeletion(pid)) {
                    log.warn("Skipping destruction of {}, it is not marked for deletion", pid);
                    continue;
                }

                if (!repoObj.getResource(true).hasProperty(RDF.type, Cdr.Tombstone)) {
                    RepositoryObject parentObj = repoObj.getParent();

                    // purge tree with repoObj as root from repository
                    // Add the root of the tree to delete
                    deletedObjIds.add(repoObj.getPid().getUUID());

                    destroyTree(repoObj);

                    // Add premis event to parent
                    String lineSeparator = System.getProperty("line.separator");
                    premisLoggerFactory.createPremisLogger(parentObj)
                            .buildEvent(Premis.Deletion)
                            .addAuthorizingAgent(AgentPids.forPerson(agent))
                            .addOutcome(true)
                            .addEventDetail("{0} object(s) were destroyed", deletedObjIds.size())
                            .addEventDetail("Objects destroyed:" + lineSeparator
                                            + "{0}", String.join(lineSeparator, deletedObjIds))
                            .writeAndClose();
                }
                indexingMessageSender.sendIndexingOperation(agent.getUsername(), pid, DELETE_SOLR_TREE);
            }
        } catch (Exception e) {
             tx.cancel(e);
        } finally {
             tx.close();
        }

        // Defer binary cleanup until after fedora destroy transaction completes
        destroyBinaries();
    }



    private void destroyTree(RepositoryObject rootOfTree) throws FedoraException, IOException,
            FcrepoOperationFailedException {
        log.debug("Performing destroy on object {} of type {}",
                rootOfTree.getPid().getQualifiedId(), rootOfTree.getClass().getName());

        if (rootOfTree instanceof ContentContainerObject) {
            ContentContainerObject container = (ContentContainerObject) rootOfTree;
            List<ContentObject> members = container.getMembers();

            for (ContentObject member : members) {
                deletedObjIds.add(member.getPid().getUUID());
                destroyTree(member);

            }
        }

        Resource rootResc = rootOfTree.getResource(true);
        Model rootModel = rootResc.getModel();
        List<URI> binaryUris = null;
        if (rootOfTree instanceof FileObject) {
            FileObject fileObj = (FileObject) rootOfTree;
            binaryUris = destroyFile(fileObj, rootResc);
        }

        sendBinariesDestroyedMsg(rootOfTree, binaryUris);

        boolean hasLdpContains = rootModel.contains(rootResc, Ldp.contains);
        if (hasLdpContains) {
            deleteNonContentObjects(rootModel);
        }
        // destroy root of sub-tree
        Model stoneModel = convertModelToTombstone(rootOfTree, rootResc);
        repoObjFactory.createOrTransformObject(rootOfTree.getUri(), stoneModel);

        //add premis event to tombstone
        premisLoggerFactory.createPremisLogger(rootOfTree)
            .buildEvent(Premis.Deletion)
            .addAuthorizingAgent(AgentPids.forPerson(agent))
            .addEventDetail("Item deleted from repository and replaced by tombstone")
            .writeAndClose();
    }

    private Model convertModelToTombstone(RepositoryObject destroyedObj, Resource destroyedResc)
            throws IOException, FcrepoOperationFailedException {

        Model stoneModel = ModelFactory.createDefaultModel();
        stoneModel.add(destroyedResc.getModel().listStatements(new TombstonePropertySelector(destroyedResc)));

        // determine paths and store in tombstone model
        ObjectPath objPath = pathFactory.getPath(destroyedObj.getPid());
        String namePath = objPath.toNamePath();
        stoneModel.add(destroyedResc, Cdr.historicalPath, namePath);
        String pidPath = objPath.toIdPath();
        stoneModel.add(destroyedResc, Cdr.historicalIdPath, pidPath);
        stoneModel.add(destroyedResc, RDF.type, Cdr.Tombstone);
        return stoneModel;
    }

    private List<URI> destroyFile(FileObject fileObj, Resource resc) {
        BinaryObject origFile = fileObj.getOriginalFile();
        if (origFile != null) {
            addBinaryMetadataToParent(resc, origFile);
            URI uri = origFile.getContentUri();
            cleanupBinaryUris.add(uri);
            return Collections.singletonList(uri);
        }
        return null;
    }

    private void addBinaryMetadataToParent(Resource parentResc, BinaryObject child) {
        log.debug("Adding binary metadata from {} to parent {}", child.getPid().getQualifiedId(), parentResc);
        Resource childResc = child.getResource(true);
        TombstonePropertySelector selector = new TombstonePropertySelector(childResc);
        Model childModel = childResc.getModel();
        StmtIterator iter = childModel.listStatements(selector);

        while (iter.hasNext()) {
            Statement s = iter.next();

            Property p = s.getPredicate();
            if (p.equals(RDF.type)) {
                continue;
            }
            Property replacement = null;
            if (isServerManagedProperty(p)) {
                replacement = replaceServerManagedProperty(p);
            }
            if (replacement != null) {
                parentResc.addProperty(replacement, s.getObject());
            } else {
                parentResc.addProperty(p, s.getObject());
            }
        }
    }

    private Property replaceServerManagedProperty(Property p) {
        return ServerManagedProperties.mapToLocalNamespace(p);

    }

    private void deleteNonContentObjects(Model model) {
        NodeIterator iter = model.listObjectsOfProperty(Ldp.contains);
        while (iter.hasNext()) {
            RDFNode obj = iter.next();
            String objUri = obj.asResource().getURI();
            // do not delete Premis events and metadata container
            if (!(objUri.endsWith("/" + MD_EVENTS.getId()) || objUri.endsWith("/" + METADATA_CONTAINER))) {
                purgeObject(objUri);
            } else {
                log.debug("Skipping destroy on {}", objUri);
            }
        }
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public void setInheritedAclFactory(InheritedAclFactory inheritedAclFactory) {
        this.inheritedAclFactory = inheritedAclFactory;
    }

}