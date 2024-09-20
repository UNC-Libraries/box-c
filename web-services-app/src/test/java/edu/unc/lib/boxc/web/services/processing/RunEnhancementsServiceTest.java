package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import edu.unc.lib.boxc.model.api.objects.FileObject;

import java.util.List;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class RunEnhancementsServiceTest {
    private static final String USER_NAME = "user";
    private AutoCloseable closeable;
    @Mock
    private AccessControlService aclService;
    @Mock
    private MessageSender messageSender;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private SolrQueryLayerService queryLayer;
    private AgentPrincipals agent;
    private RunEnhancementsService service;
    private PID filePid;
    @Mock
    private FileObject fileObject;
    @Mock
    private ContentObjectRecord fileRecord;
    @Mock
    private Datastream originalDs;
    private PID workPid;
    @Mock
    private WorkObject workObject;
    @Mock
    private ContentObjectRecord workRecord;
    @Captor
    private ArgumentCaptor<Document> docCaptor;
    @Mock
    private SearchResultResponse searchResultResp;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new RunEnhancementsService();
        service.setAclService(aclService);
        service.setMessageSender(messageSender);
        service.setQueryLayer(queryLayer);
        service.setRepositoryObjectLoader(repositoryObjectLoader);
        agent = new AgentPrincipalsImpl(USER_NAME, new AccessGroupSetImpl("group"));

        filePid = TestHelper.makePid();
        mockObject(filePid, fileObject, fileRecord, ResourceType.File);
        when(fileRecord.getDatastreamObject(ORIGINAL_FILE.getId())).thenReturn(originalDs);

        workPid = TestHelper.makePid();
        mockObject(workPid, workObject, workRecord, ResourceType.Work);

        when(queryLayer.performSearch(any())).thenReturn(searchResultResp);
    }

    private void mockObject(PID pid, ContentObject contentObject, ContentObjectRecord record, ResourceType resourceType) {
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(contentObject);
        when(queryLayer.getObjectById(argThat(req -> req != null && pid.equals(req.getPid())))).thenReturn(record);
        when(record.getPid()).thenReturn(pid);
        when(record.getResourceType()).thenReturn(resourceType.name());
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void runFileObjectTest() {
        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(true);
        request.setForce(false);
        request.setPids(List.of(filePid.getId()));

        service.run(request);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        var dsPid = DatastreamPids.getOriginalFilePid(filePid);
        assertMessageValues(msgDoc, dsPid, false);
    }

    @Test
    public void runFileObjectWithForceTest() {
        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(true);
        request.setForce(true);
        request.setPids(List.of(filePid.getId()));

        service.run(request);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        var dsPid = DatastreamPids.getOriginalFilePid(filePid);
        assertMessageValues(msgDoc, dsPid, true);
    }

    @Test
    public void runWorkObjectShallowTest() {
        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(false);
        request.setForce(false);
        request.setPids(List.of(workPid.getId()));

        service.run(request);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        assertMessageValues(msgDoc, workPid, false);
    }

    @Test
    public void runWorkObjectRecursiveTest() {
        when(searchResultResp.getResultCount()).thenReturn(1L);
        when(searchResultResp.getSelectedContainer()).thenReturn(workRecord);
        when(searchResultResp.getResultList()).thenReturn(List.of(fileRecord));

        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(true);
        request.setForce(false);
        request.setPids(List.of(workPid.getId()));

        service.run(request);

        verify(messageSender, times(2)).sendMessage(docCaptor.capture());
        var msgDocs = docCaptor.getAllValues();
        assertMessageValues(msgDocs.get(0), workPid, false);
        var dsPid = DatastreamPids.getOriginalFilePid(filePid);
        assertMessageValues(msgDocs.get(1), dsPid, false);
    }

    @Test
    public void runMultiplePidsShallowTest() {
        PID collPid = TestHelper.makePid();
        var collObject = mock(CollectionObject.class);
        var collRecord = mock(ContentObjectRecord.class);
        mockObject(collPid, collObject, collRecord, ResourceType.Collection);

        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(false);
        request.setForce(false);
        request.setPids(List.of(workPid.getId(), filePid.getId(), collPid.getId()));

        service.run(request);

        verify(messageSender, times(3)).sendMessage(docCaptor.capture());
        var msgDocs = docCaptor.getAllValues();
        assertMessageValues(msgDocs.get(0), workPid, false);
        var dsPid = DatastreamPids.getOriginalFilePid(filePid);
        assertMessageValues(msgDocs.get(1), dsPid, false);
        assertMessageValues(msgDocs.get(2), collPid, false);
    }

    @Test
    public void runMultiplePidsRecursiveTest() {
        PID workPid2 = TestHelper.makePid();
        var workObject2 = mock(WorkObject.class);
        var workRecord2 = mock(ContentObjectRecord.class);
        mockObject(workPid2, workObject2, workRecord2, ResourceType.Work);

        PID filePid2 = TestHelper.makePid();
        var fileObject2 = mock(FileObject.class);
        var fileRecord2 = mock(ContentObjectRecord.class);
        mockObject(filePid2, fileObject2, fileRecord2, ResourceType.File);
        when(fileRecord2.getDatastreamObject(ORIGINAL_FILE.getId())).thenReturn(originalDs);

        when(searchResultResp.getResultCount()).thenReturn(1L);
        when(searchResultResp.getSelectedContainer()).thenReturn(workRecord, workRecord2);
        when(searchResultResp.getResultList()).thenReturn(List.of(fileRecord)).thenReturn(List.of(fileRecord2));

        var request = new RunEnhancementsRequest();
        request.setAgent(agent);
        request.setRecursive(true);
        request.setForce(false);
        request.setPids(List.of(workPid.getId(), workPid2.getId()));

        service.run(request);

        verify(messageSender, times(4)).sendMessage(docCaptor.capture());
        var msgDocs = docCaptor.getAllValues();
        assertMessageValues(msgDocs.get(0), workPid, false);
        var dsPid = DatastreamPids.getOriginalFilePid(filePid);
        assertMessageValues(msgDocs.get(1), dsPid, false);
        assertMessageValues(msgDocs.get(2), workPid2, false);
        var dsPid2 = DatastreamPids.getOriginalFilePid(filePid2);
        assertMessageValues(msgDocs.get(3), dsPid2, false);
    }

    private void assertMessageValues(Document msgDoc, PID expectedPid, boolean expectedForce) {
        Element entry = msgDoc.getRootElement();
        Element runEl = entry.getChild(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
        String pidString = runEl.getChildText("pid", CDR_MESSAGE_NS);
        String author = entry.getChild("author", ATOM_NS)
                .getChildText("name", ATOM_NS);
        var force = Boolean.valueOf(runEl.getChildText("force", CDR_MESSAGE_NS));

        assertEquals(expectedPid, PIDs.get(pidString));
        assertEquals(USER_NAME, author);
        assertEquals(expectedForce, force);
    }
}
