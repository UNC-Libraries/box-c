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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.IndexingActionType;

public class UpdateTreeActionTest extends BaseEmbeddedSolrTest {
    private static final Logger log = LoggerFactory.getLogger(UpdateTreeActionTest.class);

    private PID pid1;
    private PID pid2;
    private PID pid3;
    private PID pid4;
    private PID pid5;
    private PID pid6;
    private PID nonExistentPid;

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

    protected Map<String, List<PID>> children;

    protected UpdateTreeAction action;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        pid1 = makePid();
        pid2 = makePid();
        pid3 = makePid();
        pid4 = makePid();
        pid5 = makePid();
        pid6 = makePid();
        nonExistentPid = makePid();

        // Establish basic containment relations
        mockContainerMembers(pid2, pid4, pid6);
        mockContainerMembers(pid1, pid2, pid3);

        server.add(populate());
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

    protected UpdateTreeAction getAction() {
        return new UpdateTreeAction();
    }

    @Test
    public void testVerifyUpdated() throws Exception {
        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(pid2.getRepositoryPath(),
                RECURSIVE_ADD, "1"));

        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        // Verify that only the object itself and its children, excluding orphans, were updated
        assertOnlySolrRecordsModified(docListBefore, docListAfter, pid2, pid4, pid6);
    }

    @Test
    public void danglingContains() throws Exception {
        // Add containment of non-existent object
        mockContainerMembers(pid4, nonExistentPid);

        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(pid2.getRepositoryPath(),
                IndexingActionType.RECURSIVE_ADD, "1"));

        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertOnlySolrRecordsModified(docListBefore, docListAfter, pid2, pid4, pid6);
    }

    @Test
    public void testNoDescendents() throws Exception {
        mockContainerMembers(pid1, pid2, pid3);
        mockContainerMembers(pid2, pid4, pid6);
        ContentObject obj6 = mock(ContentObject.class);
        when(obj6.getPid()).thenReturn(pid6);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid6))).thenReturn(obj6);

        SolrDocumentList docListBefore = getDocumentList();

        action.performAction(new SolrUpdateRequest(pid6.getRepositoryPath(), IndexingActionType.RECURSIVE_ADD));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertOnlySolrRecordsModified(docListBefore, docListAfter, pid6);
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

    protected List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", pid1.getId());
        newDoc.addField("rollup", pid1.getId());
        newDoc.addField("roleGroup", "");
        newDoc.addField("readGroup", "");
        newDoc.addField("adminGroup", "");
        newDoc.addField("ancestorIds", "");
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "A collection");
        newDoc.addField("id", pid2.getId());
        newDoc.addField("rollup", pid2.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Subfolder 1");
        newDoc.addField("id", pid4.getId());
        newDoc.addField("rollup", pid4.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2, pid4));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Orphaned");
        newDoc.addField("id", pid5.getId());
        newDoc.addField("rollup", pid5.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "");
        newDoc.addField("ancestorPath", Collections.emptyList());
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File");
        newDoc.addField("id", pid6.getId());
        newDoc.addField("rollup", pid6.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid2));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1, pid2));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Second collection");
        newDoc.addField("id", pid3.getId());
        newDoc.addField("rollup", pid3.getId());
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", makeAncestorIds(pid1, pid3));
        newDoc.addField("ancestorPath", makeAncestorPath(pid1));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        return docs;
    }

    private String makeAncestorIds(PID... pids) {
        return "/" + Arrays.stream(pids).map(p -> p.getId()).collect(Collectors.joining("/"));
    }

    private List<String> makeAncestorPath(PID... pids) {
        List<String> result = new ArrayList<>();
        int i = 0;
        for (PID pid : pids) {
            i++;
            result.add(i + "," + pid.getId());
        }
        return result;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private boolean isChildPid(String id, PID... pids) {
        for (PID pid : pids) {
            if (pid.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void mockContainerMembers(PID pid, PID... childPids) {
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getPid()).thenReturn(pid);

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(container);

        List<ContentObject> members = new ArrayList<>();
        for (PID childPid : childPids) {
            ContentObject memberObj = mock(ContentObject.class);
            when(memberObj.getPid()).thenReturn(childPid);
            members.add(memberObj);
        }
        when(container.getMembers()).thenReturn(members);
    }
}
