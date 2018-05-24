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

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
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
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import io.dropwizard.metrics5.Timer;

/**
 *
 * @author harring
 *
 */
public class DestroyObjectsJob implements Runnable {
    private static final Timer timer = TimerFactory.createTimerForClass(DestroyObjectsJob.class);

    private List<PID> objsToDestroy;
    private AgentPrincipals agent;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;
    private DestroyProxyService proxyService;
    private ObjectPathFactory pathFactory;

    public DestroyObjectsJob(AgentPrincipals agent, List<PID> objsToDestroy) {
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

    private void destroyTree(RepositoryObject rootOfTree) throws FedoraException {
        if (rootOfTree instanceof ContentContainerObject) {
            ContentContainerObject container = (ContentContainerObject) rootOfTree;
            List<ContentObject> members = container.getMembers();
            for (ContentObject member : members) {
                destroyTree(member);
            }
        } else if (rootOfTree instanceof FileObject) {
            FileObject file = (FileObject) rootOfTree;
            List<BinaryObject> binaries = file.getBinaryObjects();
            for (BinaryObject binary : binaries) {
                destroyTree(binary);
            }
        }
        // destroy root of tree
        Model stoneModel = rootOfTree.getModel();
        stoneModel = convertModelToTombstone(rootOfTree);
        repoObjFactory.createObject(rootOfTree.getUri(), stoneModel);

        //add premis event to tombstone
        rootOfTree.getPremisLog().buildEvent(Premis.Deletion)
        .addImplementorAgent(agent.getUsernameUri())
        .addEventDetail("Item deleted from repository and replaced by tombstone")
        .write();

        // persist model
        repoObjFactory.createObject(rootOfTree.getUri(), stoneModel);
    }

    private Model convertModelToTombstone(RepositoryObject destroyedObj) {
        Model model = destroyedObj.getModel();
        Resource resc = destroyedObj.getResource();

        if (destroyedObj instanceof BinaryObject) {
            // whitelisted properties for tombstone model of binaries
            Selector selectEbucoreFilename = new SimpleSelector(resc, Ebucore.filename, (Object) null);
            Selector selectDcFilename = new SimpleSelector(resc, DcElements.title, (Object) null);
            Selector selectPremisOriginalName = new SimpleSelector(resc, Premis.hasOriginalName, (Object) null);
            Selector selectMimetype = new SimpleSelector(resc, Ebucore.hasMimeType, (Object) null);
            Selector selectChecksum= new SimpleSelector(resc, Premis.hasMessageDigest, (Object) null);
            Selector selectFilesize = new SimpleSelector(resc, Premis.hasSize, (Long) null);

            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                Statement s = iter.nextStatement();
                if (!(selectEbucoreFilename.test(s) || selectDcFilename.test(s) || selectPremisOriginalName.test(s)
                        || selectMimetype.test(s) || selectChecksum.test(s) || selectFilesize.test(s))) {
                    model.remove(s);
                }
            }
        } else {
            // retain only the object-type of a non-binary object
            Selector selectRdfType = new SimpleSelector(resc, RDF.type, (Object) null);
            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                Statement s = iter.nextStatement();
                if (!selectRdfType.test(s)) {
                    model.remove(s);
                }
            }
        }
        // determine path and store in tombstone model
        String path = pathFactory.getPath(destroyedObj.getPid()).toNamePath();
        model.add(resc, Cdr.historicalPath, path);
        model.add(resc, RDF.type, Cdr.Tombstone);
        return model;
    }
}