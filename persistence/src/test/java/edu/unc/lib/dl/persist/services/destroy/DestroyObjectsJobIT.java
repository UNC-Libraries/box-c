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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
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
    @Mock
    private DestroyProxyService proxyService;
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

        createContentTree();
        job = new DestroyObjectsJob(objsToDestroy);
        job.setPathFactory(pathFactory);
        job.setProxyService(proxyService);
        job.setRepoObjFactory(repoObjFactory);
        job.setRepoObjLoader(repoObjLoader);
        job.setTxManager(txManager);

        when(proxyService.destroyProxy(any(PID.class))).thenReturn("path/to/parent");
        when(pathFactory.getPath(any(PID.class))).thenReturn(path);
        when(path.toNamePath()).thenReturn("path/to/object");
    }

    @Test
    public void destroyObjectsTest() {
        job.run();

    }

    private void createContentTree() throws Exception {
        Model folderModel = ModelFactory.createDefaultModel();
        FolderObject folder = repoObjFactory.createFolderObject(folderModel);
        Model workModel = ModelFactory.createDefaultModel();
        WorkObject work = repoObjFactory.createWorkObject(workModel);
        folder.addMember(work);
        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());
        FileObject file = work.addDataFile(contentStream, filename, mimetype, null, null);

//        queryModel.add(folder.getModel());
//        queryModel.add(work.getModel());
//        queryModel.add(file.getModel());
        queryModel.removeAll();

        treeIndexer.indexAll(folder.getModel());

        objsToDestroy.add(folder.getPid());
        objsToDestroy.add(work.getPid());
        objsToDestroy.add(file.getPid());
        markObjsForDeletion(objsToDestroy);
    }

    private void markObjsForDeletion(List<PID> objsToDestroy) {
        for (PID pid : objsToDestroy) {
            String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion,
                    true);
            sparqlUpdateService.executeUpdate(pid.getRepositoryUri().toString(), updateString);
        }
    }
}
