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
package edu.unc.lib.dl.persist.services.destroy;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.FCR_TOMBSTONE;

import java.io.IOException;
import java.net.URI;
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
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.util.TombstonePropertySelector;
import io.dropwizard.metrics5.Timer;

/**
 * A job for removing objects from the repository and replacing them with tombstones
 *
 * @author harring
 *
 */
public class DestroyObjectsJob implements Runnable {
    private static final Timer timer = TimerFactory.createTimerForClass(DestroyObjectsJob.class);

    private List<PID> objsToDestroy;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;
    private DestroyProxyService proxyService;
    private ObjectPathFactory pathFactory;
    private FcrepoClient fcrepoClient;

    public DestroyObjectsJob(List<PID> objsToDestroy) {
        this.objsToDestroy = objsToDestroy;
    }

    @Override
    public void run() {
        FedoraTransaction tx = txManager.startTransaction();
        try (Timer.Context context = timer.time()) {
            // convert each destroyed obj to a tombstone
            for (PID pid : objsToDestroy) {
                RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
                if (!repoObj.getResource().hasProperty(RDF.type, Cdr.Tombstone)) {
                    // remove containment relation from obj's parent
                    proxyService.destroyProxy(pid);
                    // purge tree with repoObj as root from repository
                    destroyTree(repoObj);
                }
           }
        } catch (Exception e) {
             tx.cancel(e);
        } finally {
             tx.close();
        }
    }

    private void destroyTree(RepositoryObject rootOfTree) throws FedoraException, IOException,
            FcrepoOperationFailedException {
        if (rootOfTree instanceof ContentContainerObject) {
            ContentContainerObject container = (ContentContainerObject) rootOfTree;
            List<ContentObject> members = container.getMembers();
            for (ContentObject member : members) {
                destroyTree(member);
            }
        }
        Resource rootResc = rootOfTree.getResource();
        Model rootModel = rootResc.getModel();
        if (rootOfTree instanceof FileObject) {
            FileObject file = (FileObject) rootOfTree;
            BinaryObject origFile = file.getOriginalFile();
            if (origFile != null) {
                addBinaryMetadataToParent(rootResc, origFile);
            }
        }
        boolean hasLdpContains = rootModel.contains(rootResc, Ldp.contains);
        if (hasLdpContains) {
            deleteNonContentObjects(rootModel);
        }
        // destroy root of sub-tree
        Model stoneModel = convertModelToTombstone(rootOfTree, rootResc);
        repoObjFactory.createOrTransformObject(rootOfTree.getUri(), stoneModel);

        //add premis event to tombstone
        rootOfTree.getPremisLog().buildEvent(Premis.Deletion)
            .addEventDetail("Item deleted from repository and replaced by tombstone")
            .write();
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

    private void addBinaryMetadataToParent(Resource parentResc, BinaryObject child) {
        Resource childResc = child.getResource();
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
            if (ServerManagedProperties.isServerManagedProperty(p)) {
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
        Property localProperty = ServerManagedProperties.mapToLocalNamespace(p);
        return localProperty;

    }

    private void deleteNonContentObjects(Model model) {
        NodeIterator iter = model.listObjectsOfProperty(Ldp.contains);
        while (iter.hasNext()) {
            RDFNode obj = iter.next();
            String objUri = obj.asResource().getURI();
            // do not delete Premis events
            if (!objUri.endsWith("event")) {
                try (FcrepoResponse resp = fcrepoClient.delete(URI.create(objUri)).perform()) {
                } catch (FcrepoOperationFailedException | IOException e) {
                    throw new ServiceException("Unable to clean up proxy for " + objUri, e);
                }

                URI tombstoneUri = URI.create(objUri.toString() + "/" + FCR_TOMBSTONE);
                try (FcrepoResponse resp = fcrepoClient.delete(tombstoneUri).perform()) {
                } catch (FcrepoOperationFailedException | IOException e) {
                    throw new ServiceException("Unable to clean up proxy tombstone for " + objUri, e);
                }
            }
        }
    }

    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setProxyService(DestroyProxyService proxyService) {
        this.proxyService = proxyService;
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}