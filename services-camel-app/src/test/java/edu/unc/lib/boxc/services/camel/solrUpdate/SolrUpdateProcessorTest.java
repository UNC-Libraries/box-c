package edu.unc.lib.boxc.services.camel.solrUpdate;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_DESCRIPTION;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.services.camel.TestHelper;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.solr.client.solrj.SolrClient;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class SolrUpdateProcessorTest {
    private AutoCloseable closeable;

    private SolrUpdateProcessor processor;

    private Map<IndexingActionType, IndexingAction> indexingActionMap;

    private Document bodyDoc;
    private PID targetPid;

    @Mock
    private Exchange exchange;
    @Mock
    private Message msg;

    @Mock
    private IndexingAction mockAction;
    @Mock
    private IndexingAction mockUpdateAccessAction;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private MessageSender messageSender;
    @Mock
    private TitleRetrievalService titleRetrievalService;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private SolrClient solrClient;

    @Captor
    private ArgumentCaptor<SolrUpdateRequest> requestCaptor;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        indexingActionMap = new HashMap<>();
        indexingActionMap.put(ADD, mockAction);
        indexingActionMap.put(IndexingActionType.UPDATE_ACCESS, mockUpdateAccessAction);
        indexingActionMap.put(DELETE, mockAction);
        indexingActionMap.put(UPDATE_DESCRIPTION, mockAction);

        processor = new SolrUpdateProcessor();
        processor.setSolrIndexingActionMap(indexingActionMap);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setUpdateWorkSender(messageSender);
        processor.setTitleRetrievalService(titleRetrievalService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setSolrClient(solrClient);

        bodyDoc = new Document();
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getBody()).thenReturn(bodyDoc);

        targetPid = TestHelper.makePid();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testMessageNoChildren() throws Exception {
        populateEntry(ADD);

        processor.process(exchange);

        verify(mockAction).performAction(any(SolrUpdateRequest.class));
    }

    @Test
    public void testMessageWithChildren() throws Exception {
        populateEntry(ADD);
        List<PID> childrenPaths = addChildren(3);

        processor.process(exchange);

        verify(mockAction).performAction(requestCaptor.capture());
        ChildSetRequest childSetRequest = (ChildSetRequest) requestCaptor.getValue();

        assertTrue(childSetRequest.getChildren().containsAll(childrenPaths));
    }

    @Test
    public void testInvalidIndexingAction() throws Exception {
        // This action is not mapped
        populateEntry(IndexingActionType.UPDATE_TYPE);

        processor.process(exchange);

        verify(mockAction, never()).performAction(any());
    }

    @Test
    public void testFileMessageUpdateWork() throws Exception {
        populateEntry(IndexingActionType.ADD);
        var targetFile = mock(FileObject.class);
        when(targetFile.getPid()).thenReturn(targetPid);
        var parentWork = mock(WorkObject.class);
        var workPid = PIDs.get(UUID.randomUUID().toString());
        when(targetFile.getParent()).thenReturn(parentWork);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(targetFile);

        processor.process(exchange);

        verify(messageSender).sendMessage(workPid.getQualifiedId());
        // Regular indexing should also happen
        verify(mockAction).performAction(any(SolrUpdateRequest.class));
    }

    @Test
    public void testFileMessageNotNeedWorkUpdate() throws Exception {
        populateEntry(IndexingActionType.UPDATE_ACCESS);
        var targetFile = mock(FileObject.class);
        var parentWork = mock(WorkObject.class);
        var workPid = PIDs.get(UUID.randomUUID().toString());
        when(targetFile.getParent()).thenReturn(parentWork);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(targetFile);

        processor.process(exchange);

        verify(messageSender, never()).sendMessage(anyString());
        // Regular indexing should happen regardless
        verify(mockUpdateAccessAction).performAction(any(SolrUpdateRequest.class));
    }

    @Test
    public void testTombstoneUpdate() throws Exception {
        populateEntry(IndexingActionType.ADD);
        var tombstone = mock(Tombstone.class);
        when(tombstone.getPid()).thenReturn(targetPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(tombstone);

        processor.process(exchange);

        // Tombstones should not be indexed for update action
        verify(mockAction, never()).performAction(any());
    }

    @Test
    public void testTombstoneDelete() throws Exception {
        populateEntry(IndexingActionType.DELETE);
        var tombstone = mock(Tombstone.class);
        when(tombstone.getPid()).thenReturn(targetPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(tombstone);

        processor.process(exchange);

        // Tombstones should not be indexed for update action
        verify(mockAction).performAction(any());
    }

    private Element populateEntry(IndexingActionType type) {
        Element entry = new Element("entry", ATOM_NS);
        bodyDoc.addContent(entry);

        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getRepositoryPath()));
        entry.addContent(new Element("author", ATOM_NS).setText("someone"));

        entry.addContent(new Element("actionType", ATOM_NS)
                .setText(type.getURI().toString()));

        return entry;
    }

    @Test
    public void testUpdateCollectionDescription() throws Exception {
        populateEntry(UPDATE_DESCRIPTION);
        var targetCollection = mock(CollectionObject.class);
        when(targetCollection.getPid()).thenReturn(targetPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(targetCollection);
        when(titleRetrievalService.retrieveCachedTitle(targetPid)).thenReturn("previous title");
        when(titleRetrievalService.retrieveTitle(targetPid)).thenReturn("new title");

        processor.process(exchange);

        // Regular indexing should also happen
        verify(mockAction).performAction(any(SolrUpdateRequest.class));
        verify(indexingMessageSender).sendIndexingOperation("", targetPid, IndexingActionType.UPDATE_PARENT_PATH_TREE);
    }

    @Test
    public void testAddWithParams() throws Exception {
        populateEntry(ADD);
        bodyDoc.getRootElement()
                .addContent(new Element("params", CDR_MESSAGE_NS)
                        .addContent(new Element("param", CDR_MESSAGE_NS)
                                .setAttribute("name", "key1")
                                .setText("value1")));
        var targetWork = mock(CollectionObject.class);
        when(targetWork.getPid()).thenReturn(targetPid);
        when(repositoryObjectLoader.getRepositoryObject(targetPid)).thenReturn(targetWork);

        processor.process(exchange);

        // Regular indexing should also happen
        verify(mockAction).performAction(requestCaptor.capture());
        assertNotNull(requestCaptor.getValue().getParams());
        assertEquals("value1", requestCaptor.getValue().getParams().get("key1"));
    }

    private List<PID> addChildren(int count) {
        Element entry = bodyDoc.getRootElement();
        Element children = new Element("children", CDR_MESSAGE_NS);
        entry.addContent(children);

        List<PID> pids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PID pid = TestHelper.makePid();
            pids.add(pid);
            children.addContent(new Element("pid", CDR_MESSAGE_NS)
                    .setText(pid.getRepositoryPath()));
        }
        return pids;
    }
}
