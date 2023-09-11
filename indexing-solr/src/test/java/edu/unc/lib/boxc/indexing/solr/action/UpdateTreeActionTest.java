package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.addContainerToParent;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.addFileObjectToParent;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeContainer;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_ADD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.test.TestCorpus;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.sparql.JenaSparqlQueryServiceImpl;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateTreeActionTest {
    protected static final String USER = "user";

    protected TestCorpus corpus;
    private AutoCloseable closeable;

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;

    protected UpdateTreeAction action;

    protected Model sparqlModel;
    protected RecursiveTreeIndexer treeIndexer;

    @Mock
    protected IndexingMessageSender messageSender;

    @Captor
    protected ArgumentCaptor<PID> pidCaptor;

    protected SparqlQueryService sparqlQueryService;

    @BeforeEach
    public void setupTreeAction() throws Exception {
        closeable = openMocks(this);

        corpus = new TestCorpus();

        // Establish basic containment relations
        ContentContainerObject obj1 = makeContainer(corpus.pid1, repositoryObjectLoader);
        ContentContainerObject obj2 = addContainerToParent(obj1, corpus.pid2, repositoryObjectLoader);
        FileObject file1 = addFileObjectToParent(obj1, corpus.pid3, repositoryObjectLoader);
        FileObject file2 = addFileObjectToParent(obj2, corpus.pid4, repositoryObjectLoader);
        FileObject file3 = addFileObjectToParent(obj2, corpus.pid6, repositoryObjectLoader);

        sparqlModel = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(sparqlModel);

        indexTriples(obj1, obj2, file1, file2, file3);

        treeIndexer = new RecursiveTreeIndexer();
        treeIndexer.setIndexingMessageSender(messageSender);
        treeIndexer.setSparqlQueryService(sparqlQueryService);

        action = getAction();
        action.setRepositoryObjectLoader(repositoryObjectLoader);
        action.setTreeIndexer(treeIndexer);
        action.setActionType(IndexingActionType.ADD.name());
    }

    protected UpdateTreeAction getAction() {
        return new UpdateTreeAction();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testVerifyQueued() throws Exception {
        action.performAction(new SolrUpdateRequest(corpus.pid2.getRepositoryPath(),
                RECURSIVE_ADD, "1", USER));

        verify(messageSender, times(3)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(corpus.pid2));
        assertTrue(pids.contains(corpus.pid4));
        assertTrue(pids.contains(corpus.pid6));
    }

    @Test
    public void testNoDescendents() throws Exception {
        ContentObject obj6 = mock(ContentObject.class);
        when(obj6.getPid()).thenReturn(corpus.pid6);
        Model model = ModelFactory.createDefaultModel();
        when(obj6.getResource()).thenReturn(model.getResource(corpus.pid6.getRepositoryPath()));
        when(repositoryObjectLoader.getRepositoryObject(eq(corpus.pid6))).thenReturn(obj6);

        action.performAction(new SolrUpdateRequest(corpus.pid6.getRepositoryPath(), IndexingActionType.RECURSIVE_ADD));

        verify(messageSender).sendIndexingOperation(eq(null),
                pidCaptor.capture(), eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(corpus.pid6));
    }

    @Test
    public void testSkipStartingObject() throws Exception {
        action.setSkipIndexingStartingObject(true);
        action.performAction(new SolrUpdateRequest(corpus.pid2.getRepositoryPath(),
                RECURSIVE_ADD, "1", USER));

        verify(messageSender, times(2)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertFalse(pids.contains(corpus.pid2));
        assertTrue(pids.contains(corpus.pid4));
        assertTrue(pids.contains(corpus.pid6));
    }

    private void indexTriples(ContentObject... objs) {
        for (ContentObject obj : objs) {
            sparqlModel.add(obj.getResource().getModel());
        }
    }
}
