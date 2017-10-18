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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.UUID;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateTreeSetActionTest {

    @Mock
    private SolrUpdateDriver driver;
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
    private RepositoryObjectLoader repositoryObjectLoader;

    @Mock
    private ChildSetRequest request;

    protected UpdateTreeSetAction action;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(sparqlQueryService.executeQuery(anyString())).thenReturn(mockQueryExecution);
        when(mockQueryExecution.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(mockQuerySolution);
        when(mockQuerySolution.getLiteral(eq("count"))).thenReturn(mockLiteral);

        action = new UpdateTreeSetAction();
        action.setSparqlQueryService(sparqlQueryService);
        action.setPipeline(pipeline);
        action.setAddDocumentMode(true);
        action.setSolrUpdateDriver(driver);
        action.setFactory(factory);
        action.setRepositoryObjectLoader(repositoryObjectLoader);

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

    @Test
    public void testSingleEmptyChild() throws Exception {
        ContentContainerObject containerObj = makeContainerObject();
        PID containerPid = containerObj.getPid();

        when(request.getChildren()).thenReturn(Arrays.asList(containerPid));

        action.performAction(request);

        verify(driver, times(1)).addDocument(any(IndexDocumentBean.class));
    }

    /**
     * Verify that all children included in the request are indexed
     */
    @Test
    public void testMultipleChildren() throws Exception {
        ContentContainerObject container1Obj = makeContainerObject();
        PID container1Pid = container1Obj.getPid();
        ContentContainerObject container2Obj = makeContainerObject();
        PID container2Pid = container2Obj.getPid();

        when(request.getChildren()).thenReturn(Arrays.asList(container1Pid, container2Pid));

        action.performAction(request);

        verify(driver, times(2)).addDocument(any(IndexDocumentBean.class));
    }

    /**
     * Verify that children of the submitted children are indexed
     */
    @Test
    public void testNestedChildren() throws Exception {
        ContentContainerObject containerObj = makeContainerObject();
        PID containerPid = containerObj.getPid();
        ContentContainerObject childObj = makeContainerObject();

        when(containerObj.getMembers()).thenReturn(Arrays.asList(childObj));

        when(request.getChildren()).thenReturn(Arrays.asList(containerPid));

        action.performAction(request);

        verify(driver, times(2)).addDocument(any(IndexDocumentBean.class));
    }

    @Test(expected = IndexingException.class)
    public void testNotChildSetRequest() throws Exception {
        SolrUpdateRequest request = mock(SolrUpdateRequest.class);

        action.performAction(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoChildrenRequest() throws Exception {
        when(request.getChildren()).thenReturn(Arrays.asList());

        action.performAction(request);
    }

    private ContentContainerObject makeContainerObject() {
        PID pid = makePid();
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getPid()).thenReturn(pid);

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);

        return container;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
