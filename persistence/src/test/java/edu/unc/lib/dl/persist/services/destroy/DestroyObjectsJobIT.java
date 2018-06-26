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

import static edu.unc.lib.dl.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.dl.sparql.SparqlUpdateHelper.createSparqlReplace;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.sparql.SparqlUpdateService;
import edu.unc.lib.dl.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.dl.test.TestHelper;
/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
})
public class DestroyObjectsJobIT {

    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    @Spy
    private DestroyProxyService spyProxyService;
    @Autowired
    private ObjectPathFactory pathFactory;
    @Mock
    private ObjectPath path;
    @Autowired
    private SparqlUpdateService sparqlUpdateService;
    @Autowired
    private Model queryModel;
    @Autowired
    private FcrepoClient fcrepoClient;

    private RepositoryObjectTreeIndexer treeIndexer;

    private List<PID> objsToDestroy = new ArrayList<>();

    private DestroyObjectsJob job;

    @Before
    public void init() throws Exception {
        initMocks(this);
        TestHelper.setContentBase("http://localhost:48085/rest");
        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));

        treeIndexer = new RepositoryObjectTreeIndexer(queryModel, fcrepoClient);

        objsToDestroy = createContentTree();

        when(pathFactory.getPath(any(PID.class))).thenReturn(path);
        when(path.toNamePath()).thenReturn("path/to/object");
    }

    @Test
    public void destroySingleObjectTest() {
        PID fileObjPid = objsToDestroy.get(2);
        initializeJob(Arrays.asList(fileObjPid));

        job.run();

        verify(spyProxyService).destroyProxy(fileObjPid);
        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        assertTrue(fileObj.getModel().contains(fileObj.getResource(), RDF.type, Cdr.Tombstone));
    }

    @Test
    public void destroyObjectsInSameTreeTest() {
        initializeJob(objsToDestroy);
        //remove unrelated folder obj before running job
        objsToDestroy.remove(3);

        job.run();

        PID fileObjPid = objsToDestroy.get(2);
        PID workObjPid = objsToDestroy.get(1);
        PID folderObjPid = objsToDestroy.get(0);
        verify(spyProxyService).destroyProxy(folderObjPid);

        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        WorkObject workObj = repoObjLoader.getWorkObject(workObjPid);
        FolderObject folderObj = repoObjLoader.getFolderObject(folderObjPid);
        assertTrue(fileObj.getModel().contains(fileObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(workObj.getModel().contains(workObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(folderObj.getModel().contains(folderObj.getResource(), RDF.type, Cdr.Tombstone));

        PremisEventObject event = repoObjLoader.getPremisEventObject(folderObj.getPremisLog().listEvents().get(0));
        assertTrue(event.getResource().hasProperty(Premis.hasEventType, Premis.Deletion));
        assertTrue(event.getResource().hasProperty(Premis.hasEventDetail,
                "Item deleted from repository and replaced by tombstone"));
    }

    @Test
    public void destroyObjectsInDifferentTreesTest() {
        initializeJob(objsToDestroy);

        job.run();

        PID folderObj2Pid = objsToDestroy.get(3);
        PID fileObjPid = objsToDestroy.get(2);
        PID workObjPid = objsToDestroy.get(1);
        PID folderObjPid = objsToDestroy.get(0);
        verify(spyProxyService).destroyProxy(folderObjPid);
        verify(spyProxyService).destroyProxy(folderObj2Pid);

        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        WorkObject workObj = repoObjLoader.getWorkObject(workObjPid);
        FolderObject folderObj = repoObjLoader.getFolderObject(folderObjPid);
        FolderObject folderObj2 = repoObjLoader.getFolderObject(folderObj2Pid);
        assertTrue(fileObj.getModel().contains(fileObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(workObj.getModel().contains(workObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(folderObj.getModel().contains(folderObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(folderObj2.getModel().contains(folderObj2.getResource(), RDF.type, Cdr.Tombstone));

        PremisEventObject event = repoObjLoader.getPremisEventObject(folderObj.getPremisLog().listEvents().get(0));
        assertTrue(event.getResource().hasProperty(Premis.hasEventType, Premis.Deletion));
        assertTrue(event.getResource().hasProperty(Premis.hasEventDetail,
                "Item deleted from repository and replaced by tombstone"));

        PremisEventObject event2 = repoObjLoader.getPremisEventObject(folderObj2.getPremisLog().listEvents().get(0));
        assertTrue(event.getResource().hasProperty(Premis.hasEventType, Premis.Deletion));
        assertTrue(event.getResource().hasProperty(Premis.hasEventDetail,
                "Item deleted from repository and replaced by tombstone"));
    }

    @Test
    public void destroyFolderTest() {
        PID folderObjPid = objsToDestroy.get(0);
        initializeJob(Arrays.asList(folderObjPid));
        FolderObject folderObj = repoObjLoader.getFolderObject(folderObjPid);
        WorkObject workObj = repoObjLoader.getWorkObject(folderObj.getMembers().get(0).getPid());
        FileObject fileObj = repoObjLoader.getFileObject(workObj.getMembers().get(0).getPid());

        job.run();

        verify(spyProxyService).destroyProxy(folderObjPid);

        assertTrue(fileObj.getModel().contains(fileObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(workObj.getModel().contains(workObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(folderObj.getModel().contains(folderObj.getResource(), RDF.type, Cdr.Tombstone));
    }

    @Test
    public void destroySingleObjectWithPreexistingPremisEventTest() {
        PID fileObjPid = objsToDestroy.get(2);
        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        Resource event = fileObj.getPremisLog().buildEvent(Premis.Ingestion, new Date(1L)).write();
        PID eventPid = PIDs.get(event.getURI());

        initializeJob(Arrays.asList(fileObjPid));

        job.run();

        verify(spyProxyService).destroyProxy(fileObjPid);
        fileObj = repoObjLoader.getFileObject(fileObjPid);
        assertTrue(fileObj.getModel().contains(fileObj.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(fileObj.getPremisLog().listEvents().contains(eventPid));
    }

    private List<PID> createContentTree() throws Exception {
        CollectionObject collection = repoObjFactory.createCollectionObject(null);
        FolderObject folder = repoObjFactory.createFolderObject(null);
        FolderObject folder2 = repoObjFactory.createFolderObject(null);
        collection.addMember(folder);
        collection.addMember(folder2);
        WorkObject work = repoObjFactory.createWorkObject(null);
        folder.addMember(work);
        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());
        FileObject file = work.addDataFile(contentStream, filename, mimetype, null, null);

        treeIndexer.indexAll(collection.getModel());

        objsToDestroy.add(folder.getPid());
        objsToDestroy.add(work.getPid());
        objsToDestroy.add(file.getPid());
        objsToDestroy.add(folder2.getPid());
        markObjsForDeletion(objsToDestroy);

        return objsToDestroy;
    }

    private void initializeJob(List<PID> objsToDestroy) {
        job = new DestroyObjectsJob(objsToDestroy);
        job.setPathFactory(pathFactory);
        job.setProxyService(spyProxyService);
        job.setRepoObjFactory(repoObjFactory);
        job.setRepoObjLoader(repoObjLoader);
        job.setTxManager(txManager);
        job.setFcrepoClient(fcrepoClient);
    }

    private void markObjsForDeletion(List<PID> objsToDestroy) {
        for (PID pid : objsToDestroy) {
            String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion,
                    true);
            sparqlUpdateService.executeUpdate(pid.getRepositoryUri().toString(), updateString);
        }
    }
}
