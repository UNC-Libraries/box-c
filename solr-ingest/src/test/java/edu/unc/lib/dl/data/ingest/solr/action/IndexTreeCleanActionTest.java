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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
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
 * @author bbpennel
 */
public class IndexTreeCleanActionTest {

    @Mock
    private SolrUpdateDriver driver;
    @Mock
    private DocumentIndexingPipeline pipeline;
    @Mock
    private DeleteSolrTreeAction deleteAction;
    @Mock
    private SolrUpdateRequest request;
    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackageFactory factory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private DocumentIndexingPackage dip;

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
    private ContentContainerObject containerObj;

    private PID pid;

    private IndexTreeCleanAction action;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        when(request.getPid()).thenReturn(pid);

        action = new IndexTreeCleanAction();
        action.setDeleteAction(deleteAction);
        action.setSparqlQueryService(sparqlQueryService);
        action.setPipeline(pipeline);
        action.setSolrUpdateDriver(driver);
        action.setFactory(factory);
        action.setRepositoryObjectLoader(repositoryObjectLoader);

        when(sparqlQueryService.executeQuery(anyString())).thenReturn(mockQueryExecution);
        when(mockQueryExecution.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(mockQuerySolution).thenReturn(null);
        when(mockQuerySolution.getLiteral(eq("count"))).thenReturn(mockLiteral);

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(containerObj);

        when(factory.createDip(any(PID.class), any(DocumentIndexingPackage.class)))
                .thenReturn(dip);
    }

    @Test
    public void testPerformAction() throws Exception {

        action.performAction(request);

        verify(deleteAction).performAction(any(SolrUpdateRequest.class));
        verify(driver).commit();
        verify(driver).addDocument(any(IndexDocumentBean.class));
    }

    /**
     * Verify that action is always performed in add mode, so no update requests.
     *
     * @throws Exception
     */
    @Test
    public void testIgnoreUpdateMode() throws Exception {

        action.setAddDocumentMode(false);

        action.performAction(request);

        // IndexTreeClean must be performed in addMode
        verify(driver).addDocument(any(IndexDocumentBean.class));
        verify(driver, never()).updateDocument(any(IndexDocumentBean.class));
    }
}
