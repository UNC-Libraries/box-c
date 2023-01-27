package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.addContainerToParent;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeContainer;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.RecursiveTreeIndexer;
import edu.unc.lib.boxc.indexing.solr.action.UpdateTreeSetAction;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.sparql.JenaSparqlQueryServiceImpl;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;

/**
 *
 * @author bbpennel
 *
 */
public class UpdateTreeSetActionTest {
    private static final String USER = "user";

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;
    @Captor
    private ArgumentCaptor<PID> pidCaptor;

    private ChildSetRequest request;

    private UpdateTreeSetAction action;

    private RecursiveTreeIndexer treeIndexer;

    protected Model sparqlModel;
    protected SparqlQueryService sparqlQueryService;

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);

        sparqlModel = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(sparqlModel);

        treeIndexer = new RecursiveTreeIndexer();
        treeIndexer.setIndexingMessageSender(messageSender);
        treeIndexer.setSparqlQueryService(sparqlQueryService);

        action = new UpdateTreeSetAction();
        action.setRepositoryObjectLoader(repositoryObjectLoader);
        action.setTreeIndexer(treeIndexer);
        action.setActionType(IndexingActionType.ADD.name());
    }

    @Test
    public void testSingleEmptyChild() throws Exception {
        ContentContainerObject containerObj = makeContainer(repositoryObjectLoader);
        PID containerPid = containerObj.getPid();

        indexTriples(containerObj);

        request = new ChildSetRequest(containerPid.getRepositoryPath(), asList(containerPid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(containerPid));
    }

    /**
     * Verify that all children included in the request are indexed
     */
    @Test
    public void testMultipleChildren() throws Exception {
        ContentContainerObject container1Obj = makeContainer(repositoryObjectLoader);
        PID container1Pid = container1Obj.getPid();
        ContentContainerObject container2Obj = makeContainer(repositoryObjectLoader);
        PID container2Pid = container2Obj.getPid();

        indexTriples(container1Obj, container2Obj);

        request = new ChildSetRequest(container1Pid.getRepositoryPath(),
                asList(container1Pid.getRepositoryPath(), container2Pid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender, times(2)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(container1Pid));
        assertTrue(pids.contains(container2Pid));
    }

    /**
     * Verify that children of the submitted children are indexed
     */
    @Test
    public void testNestedChildren() throws Exception {
        ContentContainerObject containerObj = makeContainer(repositoryObjectLoader);
        PID containerPid = containerObj.getPid();
        ContentContainerObject childObj = addContainerToParent(containerObj, repositoryObjectLoader);

        indexTriples(containerObj, childObj);

        request = new ChildSetRequest(containerPid.getRepositoryPath(), asList(containerPid.getRepositoryPath()),
                IndexingActionType.ADD, USER);
        action.performAction(request);

        verify(messageSender, times(2)).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));

        List<PID> pids = pidCaptor.getAllValues();
        assertTrue(pids.contains(containerPid));
        assertTrue(pids.contains(childObj.getPid()));
    }

    @Test
    public void testNotChildSetRequest() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            SolrUpdateRequest request = mock(SolrUpdateRequest.class);

            action.performAction(request);
        });
    }

    @Test
    public void testNoChildrenRequest() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            request = new ChildSetRequest(null, asList(),
                    IndexingActionType.ADD, USER);

            action.performAction(request);
        });
    }

    private void indexTriples(ContentObject... objs) {
        for (ContentObject obj : objs) {
            sparqlModel.add(obj.getResource().getModel());
        }
    }
}
