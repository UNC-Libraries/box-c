package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.CLEAN_REINDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;

/**
 * @author bbpennel
 */
public class IndexTreeCleanActionTest {
    private static final String USER = "user";

    private AutoCloseable closeable;

    @Mock
    private SolrUpdateDriver driver;
    @Mock
    private DocumentIndexingPipeline pipeline;
    @Mock
    private DeleteSolrTreeAction deleteAction;
    @Mock
    private SolrUpdateRequest request;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender messageSender;

    private RecursiveTreeIndexer treeIndexer;

    @Mock
    private ContentContainerObject containerObj;

    @Captor
    private ArgumentCaptor<PID> pidCaptor;

    private PID pid;

    private IndexTreeCleanAction action;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        when(request.getPid()).thenReturn(pid);

        treeIndexer = new RecursiveTreeIndexer();
        treeIndexer.setIndexingMessageSender(messageSender);

        action = new IndexTreeCleanAction();
        action.setDeleteAction(deleteAction);
        action.setTreeIndexer(treeIndexer);
        action.setSolrUpdateDriver(driver);
        action.setActionType(IndexingActionType.ADD.name());
        action.setRepositoryObjectLoader(repositoryObjectLoader);

        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(containerObj);
        when(containerObj.getPid()).thenReturn(pid);
        Model model = ModelFactory.createDefaultModel();
        when(containerObj.getResource()).thenReturn(model.getResource(pid.getRepositoryPath()));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testPerformAction() throws Exception {
        request = new SolrUpdateRequest(pid.getRepositoryPath(), CLEAN_REINDEX, "1", USER);

        action.performAction(request);

        verify(deleteAction).performAction(any(SolrUpdateRequest.class));
        verify(driver).commit();

        verify(messageSender).sendIndexingOperation(eq(USER), pidCaptor.capture(),
                eq(IndexingActionType.ADD));
        assertEquals(pid, pidCaptor.getValue());
    }
}
