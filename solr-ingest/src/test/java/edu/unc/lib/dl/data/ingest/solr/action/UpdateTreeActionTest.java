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

import static edu.unc.lib.dl.util.IndexingActionType.RECURSIVE_ADD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.test.TestCorpus;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateTreeActionTest extends BaseEmbeddedSolrTest {
    protected TestCorpus corpus;

    @Mock
    private SparqlQueryService sparqlQueryService;
    @Mock
    private QueryExecution mockQueryExecution;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private QuerySolution mockQuerySolution;
    @Mock
    private Literal mockLiteral;

    @Mock
    protected DocumentIndexingPipeline pipeline;
    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackageFactory factory;
    @Mock
    protected RepositoryObjectLoader repositoryObjectLoader;

    protected UpdateTreeAction action;

    @Before
    public void setupTreeAction() throws Exception {
        initMocks(this);

        corpus = new TestCorpus();

        // Establish basic containment relations
        ContentContainerObject obj1 = makeContainer(corpus.pid1);
        ContentContainerObject obj2 = addContainerToParent(obj1, corpus.pid2);
        addFileObjectToParent(obj1, corpus.pid3);
        addFileObjectToParent(obj2, corpus.pid4);
        addFileObjectToParent(obj2, corpus.pid6);

        server.add(corpus.populate());
        server.commit();

        when(sparqlQueryService.executeQuery(anyString())).thenReturn(mockQueryExecution);
        when(mockQueryExecution.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(mockQuerySolution).thenReturn(null);
        when(mockQuerySolution.getLiteral(eq("count"))).thenReturn(mockLiteral);

        action = getAction();
        action.setSparqlQueryService(sparqlQueryService);
        action.setPipeline(pipeline);
        action.setSolrUpdateDriver(driver);
        action.setAddDocumentMode(false);
        action.setFactory(factory);
        action.setRepositoryObjectLoader(repositoryObjectLoader);

        mockDipCreation();

        mockPipelineSetField();
    }

    protected UpdateTreeAction getAction() {
        return new UpdateTreeAction();
    }

    @Test
    public void testVerifyUpdated() throws Exception {
        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(corpus.pid2.getRepositoryPath(),
                RECURSIVE_ADD, "1"));

        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        // Verify that only the object itself and its children, excluding orphans, were updated
        assertOnlySolrRecordsModified(docListBefore, docListAfter, corpus.pid2, corpus.pid4, corpus.pid6);
    }

    @Test
    public void testDanglingContains() throws Exception {
        // Add containment of non-existent object
        ContentContainerObject obj4 = makeContainer(corpus.pid4);
        addFileObjectToParent(obj4, corpus.nonExistentPid);

        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(corpus.pid2.getRepositoryPath(),
                IndexingActionType.RECURSIVE_ADD, "1"));

        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertOnlySolrRecordsModified(docListBefore, docListAfter, corpus.pid2, corpus.pid4, corpus.pid6);
    }

    @Test
    public void testNoDescendents() throws Exception {
        ContentObject obj6 = mock(ContentObject.class);
        when(obj6.getPid()).thenReturn(corpus.pid6);
        when(repositoryObjectLoader.getRepositoryObject(eq(corpus.pid6))).thenReturn(obj6);

        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(corpus.pid6.getRepositoryPath(), IndexingActionType.RECURSIVE_ADD));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertOnlySolrRecordsModified(docListBefore, docListAfter, corpus.pid6);
    }

    private void assertOnlySolrRecordsModified(SolrDocumentList docListBefore, SolrDocumentList docListAfter,
            PID... modifiedPids) {

        for (SolrDocument docAfter : docListAfter) {
            String id = (String) docAfter.getFieldValue("id");
            Long afterVersion = (Long) docAfter.getFieldValue("_version_");

            for (SolrDocument docBefore : docListBefore) {
                if (id.equals(docBefore.getFieldValue("id"))) {
                    if (isChildPid(id, modifiedPids)) {
                        assertNotEquals(afterVersion, docBefore.getFieldValue("_version_"));
                    } else {
                        assertEquals(afterVersion, docBefore.getFieldValue("_version_"));
                    }
                }
            }
        }
    }

    private boolean isChildPid(String id, PID... pids) {
        for (PID pid : pids) {
            if (pid.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    protected Map<PID, ContentObject> repoObjTree;

    protected ContentContainerObject makeContainer(PID pid) {
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getMembers()).thenReturn(new ArrayList<>());
        when(container.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);

        return container;
    }

    protected ContentContainerObject addContainerToParent(ContentContainerObject container, PID childPid) {
        ContentContainerObject memberObj = makeContainer(childPid);
        container.getMembers().add(memberObj);
        return memberObj;
    }

    protected void addFileObjectToParent(ContentContainerObject container, PID childPid) {
        ContentObject memberObj = mock(FileObject.class);
        when(memberObj.getPid()).thenReturn(childPid);
        when(repositoryObjectLoader.getRepositoryObject(eq(childPid))).thenReturn(memberObj);
        container.getMembers().add(memberObj);
    }

    protected void mockDipCreation() {
        // Mock factory to produce DIPs
        when(factory.createDip(any(PID.class), any(DocumentIndexingPackage.class)))
                .thenAnswer(new Answer<DocumentIndexingPackage>() {
            @Override
            public DocumentIndexingPackage answer(InvocationOnMock invocation) throws Throwable {
                PID pid = invocation.getArgumentAt(0, PID.class);
                DocumentIndexingPackage dip = mock(DocumentIndexingPackage.class);

                IndexDocumentBean document = new IndexDocumentBean();
                document.setId(pid.getId());

                when(dip.getDocument()).thenReturn(document);

                return dip;
            }
        });
    }

    protected void mockPipelineSetField() throws Exception {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DocumentIndexingPackage dip = invocation.getArgumentAt(0, DocumentIndexingPackage.class);
                dip.getDocument().setCreator(Arrays.asList("Added"));
                return null;
            }
        }).when(pipeline).process(any(DocumentIndexingPackage.class));
    }
}
