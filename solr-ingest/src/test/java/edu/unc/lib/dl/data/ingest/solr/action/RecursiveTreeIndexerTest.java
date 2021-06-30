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
package edu.unc.lib.dl.data.ingest.solr.action;

import static edu.unc.lib.dl.data.ingest.solr.test.MockRepositoryObjectHelpers.addMembers;
import static edu.unc.lib.dl.data.ingest.solr.test.MockRepositoryObjectHelpers.makeContainer;
import static edu.unc.lib.dl.data.ingest.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static edu.unc.lib.dl.data.ingest.solr.test.MockRepositoryObjectHelpers.makePid;
import static edu.unc.lib.dl.util.IndexingActionType.ADD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentContainerObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.sparql.JenaSparqlQueryServiceImpl;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class RecursiveTreeIndexerTest {
    private static final String USER = "user";

    private RecursiveTreeIndexer indexer;

    @Mock
    private AbstractContentContainerObject containerObj;

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;

    @Captor
    protected ArgumentCaptor<PID> pidCaptor;

    protected Model sparqlModel;
    protected RecursiveTreeIndexer treeIndexer;
    protected SparqlQueryService sparqlQueryService;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        containerObj = makeContainer(makePid(), repositoryObjectLoader);

        sparqlModel = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(sparqlModel);

        indexTriples(containerObj);

        indexer = new RecursiveTreeIndexer();
        indexer.setIndexingMessageSender(messageSender);
        indexer.setSparqlQueryService(sparqlQueryService);
    }

    @Test
    public void testNonContainer() throws Exception {
        FileObjectImpl fileObj = makeFileObject(makePid(), repositoryObjectLoader);

        indexer.index(fileObj, ADD, USER);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        assertEquals(fileObj.getPid(), pidCaptor.getValue());
    }

    @Test
    public void testNoChildren() throws Exception {
        AbstractContentContainerObject containerObj = makeContainer(makePid(), repositoryObjectLoader);

        indexer.index(containerObj, ADD, USER);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        assertEquals(containerObj.getPid(), pidCaptor.getValue());
    }

    @Test
    public void testHierarchy() throws Exception {
        AbstractContentContainerObject containerObj = makeContainer(makePid(), repositoryObjectLoader);
        AbstractContentContainerObject child1Obj = makeContainer(makePid(), repositoryObjectLoader);
        FileObjectImpl fileObj = makeFileObject(makePid(), repositoryObjectLoader);
        AbstractContentContainerObject child2Obj = makeContainer(makePid(), repositoryObjectLoader);

        addMembers(containerObj, child1Obj, child2Obj);
        addMembers(child1Obj, fileObj);

        indexTriples(containerObj, child1Obj, fileObj, child2Obj);

        indexer.index(containerObj, ADD, USER);

        verify(messageSender, times(4)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(containerObj.getPid()));
        assertTrue(pids.contains(child1Obj.getPid()));
        assertTrue(pids.contains(fileObj.getPid()));
        assertTrue(pids.contains(child2Obj.getPid()));
    }

    private void indexTriples(AbstractContentObject... objs) {
        for (AbstractContentObject obj : objs) {
            sparqlModel.add(obj.getResource().getModel());
        }
    }
}
