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

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;

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
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
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

    public static final String REPOSITORY_NAMESPACE = "http://fedora.info/definitions/v4/repository#";
    public static final Resource INBOUND_REFERENCES = createResource(REPOSITORY_NAMESPACE + "InboundReferences");
    public static final Resource PAIRTREE = createResource(REPOSITORY_NAMESPACE + "Pairtree");
    public static final Resource REPOSITORY_ROOT = createResource(REPOSITORY_NAMESPACE + "RepositoryRoot");
    public static final String IANA_NAMESPACE = "http://www.iana.org/assignments/relation/";
    public static final Property DESCRIBEDBY = createProperty(IANA_NAMESPACE + "describedby");
    public static final String LDP_NAMESPACE = "http://www.w3.org/ns/ldp#";
    public static final Property CONTAINS = createProperty(LDP_NAMESPACE + "contains");

    public static final String PREMIS_NAMESPACE = "http://www.loc.gov/premis/rdf/v1#";
    public static final Property HAS_SIZE = createProperty(PREMIS_NAMESPACE + "hasSize");
    public static final Property HAS_MESSAGE_DIGEST = createProperty(PREMIS_NAMESPACE + "hasMessageDigest");

    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final Property RDF_TYPE = createProperty(RDF_NAMESPACE + "type");

    public static final Resource CONTAINER = createResource(LDP_NAMESPACE + "Container");
    public static final Property MEMBERSHIP_RESOURCE = createProperty(LDP_NAMESPACE + "membershipResource");
    public static final Property NON_RDF_SOURCE = createProperty(LDP_NAMESPACE + "NonRDFSource");
    public static final Property RDF_SOURCE = createProperty(LDP_NAMESPACE + "RDFSource");
    public static final Property CREATED_DATE = createProperty(REPOSITORY_NAMESPACE + "created");
    public static final Property CREATED_BY = createProperty(REPOSITORY_NAMESPACE + "createdBy");
    public static final Property LAST_MODIFIED_DATE = createProperty(REPOSITORY_NAMESPACE + "lastModified");
    public static final Property LAST_MODIFIED_BY = createProperty(REPOSITORY_NAMESPACE + "lastModifiedBy");

    private List<PID> objsToDestroy;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private DestroyProxyService proxyService;
    @Autowired
    private ObjectPathFactory pathFactory;

    public DestroyObjectsJob(List<PID> objsToDestroy) {
        this.objsToDestroy = objsToDestroy;
    }

    @Override
    public void run() {
        FedoraTransaction tx = txManager.startTransaction();
        try (Timer.Context context = timer.time()) {
            // convert each destroyed obj to a tombstone
            for (PID pid : objsToDestroy) {
                // remove containment relation from obj's parent
                proxyService.destroyProxy(pid);
                RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
                // purge tree with repoObj as root from repository
                destroyTree(repoObj);
           }
        } catch (Exception e) {
             tx.cancel(e);
        } finally {
             tx.close();
        }
    }

    private void destroyTree(RepositoryObject rootOfTree) throws FedoraException, IOException, FcrepoOperationFailedException {
        if (rootOfTree instanceof ContentContainerObject) {
            ContentContainerObject container = (ContentContainerObject) rootOfTree;
            List<ContentObject> members = container.getMembers();
            for (ContentObject member : members) {
                destroyTree(member);
            }
        } else if (rootOfTree instanceof FileObject) {
            FileObject file = (FileObject) rootOfTree;
            BinaryObject binary = file.getOriginalFile();
            if (binary != null) {
                addBinaryMetadataToParent(rootOfTree, binary);
            }
        }
        // destroy root of sub-tree
        Model stoneModel = rootOfTree.getModel();
        stoneModel = convertModelToTombstone(rootOfTree);
        repoObjFactory.createOrTransformObject(rootOfTree.getUri(), stoneModel);

        //add premis event to tombstone
        rootOfTree.getPremisLog().buildEvent(Premis.Deletion)
        .addEventDetail("Item deleted from repository and replaced by tombstone")
        .write();
    }

    private Model convertModelToTombstone(RepositoryObject destroyedObj) throws IOException, FcrepoOperationFailedException {
        Model oldModel = destroyedObj.getModel();
        Model stoneModel = ModelFactory.createDefaultModel();
        Resource resc = destroyedObj.getResource();

        TombstonePropertySelector selector = new TombstonePropertySelector(resc);
        StmtIterator iter = oldModel.listStatements(selector);
        while (iter.hasNext()) {
            Statement s = iter.nextStatement();
            if (selector.selects(s)) {
                stoneModel.add(s);
            }
        }
        sanitize(stoneModel);
        // determine path and store in tombstone model
        String path = pathFactory.getPath(destroyedObj.getPid()).toNamePath();
        stoneModel.add(resc, Cdr.historicalPath, path);
        stoneModel.add(resc, RDF.type, Cdr.Tombstone);
        return stoneModel;
    }

    private void addBinaryMetadataToParent(RepositoryObject parent, BinaryObject child) {
        Model childModel = child.getModel();
        Model parentModel = parent.getModel();
        Resource resc = child.getResource();

        TombstonePropertySelector selector = new TombstonePropertySelector(resc);
        StmtIterator iter = childModel.listStatements(selector);
        while (iter.hasNext()) {
            Statement s = iter.nextStatement();
            if (selector.selects(s)) {
                parentModel.add(s);
            }
        }
    }

    private Model sanitize(final Model model) throws IOException, FcrepoOperationFailedException {
        final List<Statement> remove = new ArrayList<>();
        for (final StmtIterator it = model.listStatements(); it.hasNext(); ) {
            final Statement s = it.nextStatement();

            if ((s.getPredicate().getNameSpace().equals(REPOSITORY_NAMESPACE) && !relaxedPredicate(s.getPredicate()))
                    || s.getSubject().getURI().endsWith("fcr:export?format=jcr/xml")
                    || s.getSubject().getURI().equals(REPOSITORY_NAMESPACE + "jcr/xml")
                    || s.getPredicate().equals(DESCRIBEDBY)
                    || s.getPredicate().equals(CONTAINS)
                    || s.getPredicate().equals(HAS_MESSAGE_DIGEST)
                    || s.getPredicate().equals(HAS_SIZE)
                    || (s.getPredicate().equals(RDF_TYPE) && forbiddenType(s.getResource()))) {
                remove.add(s);
            }
        }
        return model.remove(remove);
    }

    private boolean forbiddenType(final Resource resource) {
        return resource.getNameSpace().equals(REPOSITORY_NAMESPACE)
            || resource.getURI().equals(CONTAINER.getURI())
            || resource.getURI().equals(NON_RDF_SOURCE.getURI())
            || resource.getURI().equals(RDF_SOURCE.getURI());
   }

   /**
    * Tests whether the provided property is one of the small subset of the predicates within the
    * repository namespace that may be modified.  This method always returns false if the
    * import/export configuration is set to "legacy" mode.
    * @param p the property (predicate) to test
    * @return true if the predicate is of the type that can be modified
    */
   private boolean relaxedPredicate(final Property p) {
       return (p.equals(CREATED_BY) || p.equals(CREATED_DATE)
               || p.equals(LAST_MODIFIED_BY) || p.equals(LAST_MODIFIED_DATE));
   }

    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setTxManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setProxyService(DestroyProxyService proxyService) {
        this.proxyService = proxyService;
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}