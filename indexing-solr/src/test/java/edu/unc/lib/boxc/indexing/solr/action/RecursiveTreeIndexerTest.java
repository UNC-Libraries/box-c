package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.addMembers;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeContainer;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makePid;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
public class RecursiveTreeIndexerTest {
    private static final String USER = "user";

    private RecursiveTreeIndexer indexer;
    private AutoCloseable closeable;

    @Mock
    private ContentContainerObject containerObj;

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;

    @Captor
    protected ArgumentCaptor<PID> pidCaptor;

    protected Model sparqlModel;
    protected RecursiveTreeIndexer treeIndexer;
    protected SparqlQueryService sparqlQueryService;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        containerObj = makeContainer(makePid(), repositoryObjectLoader);

        sparqlModel = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(sparqlModel);

        indexTriples(containerObj);

        indexer = new RecursiveTreeIndexer();
        indexer.setIndexingMessageSender(messageSender);
        indexer.setSparqlQueryService(sparqlQueryService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNonContainer() throws Exception {
        FileObject fileObj = makeFileObject(makePid(), repositoryObjectLoader);

        indexer.index(fileObj, ADD, USER);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        assertEquals(fileObj.getPid(), pidCaptor.getValue());
    }

    @Test
    public void testNoChildren() throws Exception {
        ContentContainerObject containerObj = makeContainer(makePid(), repositoryObjectLoader);

        indexer.index(containerObj, ADD, USER);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        assertEquals(containerObj.getPid(), pidCaptor.getValue());
    }

    @Test
    public void testHierarchy() throws Exception {
        ContentContainerObject containerObj = makeContainer(makePid(), repositoryObjectLoader);
        ContentContainerObject child1Obj = makeContainer(makePid(), repositoryObjectLoader);
        FileObject fileObj = makeFileObject(makePid(), repositoryObjectLoader);
        ContentContainerObject child2Obj = makeContainer(makePid(), repositoryObjectLoader);

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

    private void indexTriples(ContentObject... objs) {
        for (ContentObject obj : objs) {
            sparqlModel.add(obj.getResource().getModel());
        }
    }
}
