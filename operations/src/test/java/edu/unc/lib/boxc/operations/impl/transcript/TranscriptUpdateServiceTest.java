package edu.unc.lib.boxc.operations.impl.transcript;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.fullDescription.FullDescriptionUpdateService;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.fullDescription.FullDescriptionUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.operations.jms.transcript.TranscriptUpdateRequest;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.api.DatastreamType.FULL_DESCRIPTION;
import static edu.unc.lib.boxc.model.api.DatastreamType.TRANSCRIPT;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TranscriptUpdateServiceTest {
    private static final String TRANSCRIPT_TEXT = "I'm a well-reviewed transcript";
    private AutoCloseable closeable;
    private TranscriptUpdateService service;
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private VersionedDatastreamService versioningService;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private FileObject fileObject;
    @Mock
    private Resource resource;
    private PID pid;
    private String pidString;
    private PID transcriptPid;
    private AgentPrincipals agent;
    private static final AccessGroupSet ACCESS_GROUPS = new AccessGroupSetImpl("test_group");

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new TranscriptUpdateService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setVersionedDatastreamService(versioningService);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setSendsMessages(true);
        pid = TestHelper.makePid();
        pidString = pid.getId();
        transcriptPid = DatastreamPids.getTranscriptPid(pid);
        agent = new AgentPrincipalsImpl("test_user", ACCESS_GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermissionToUpdateFullDescription() {
        var request = new TranscriptUpdateRequest();
        request.setAgent(agent);
        request.setTranscriptText(TRANSCRIPT_TEXT);
        request.setPidString(pidString);

        doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(Permission.editDescription));

        assertThrows(AccessRestrictionException.class, () -> service.updateTranscript(request));

        verifyNoInteractions(repositoryObjectFactory, versioningService, operationsMessageSender);
    }

    @Test
    public void testSetsNewTranscript() throws IOException {
        var request = new TranscriptUpdateRequest();
        request.setAgent(agent);
        request.setTranscriptText(TRANSCRIPT_TEXT);
        request.setPidString(pidString);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(transcriptPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateTranscript(request);

        // Verify ACL check
        verify(aclService).assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

        // Verify transcript creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(TRANSCRIPT.getMimetype(), capturedVersion.getContentType());
        assertEquals(TRANSCRIPT.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(TRANSCRIPT_TEXT, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify relationship creation
        var expectedRelationObj = createResource(transcriptPid.getRepositoryPath());
        verify(repositoryObjectFactory).createRelationship(eq(fileObject), eq(Cdr.hasTranscript), eq(expectedRelationObj));

        // Verify operation message
        verify(operationsMessageSender).sendUpdateDescriptionOperation(eq("test_user"), anyList(), eq(IndexingPriority.normal));

        assertEquals(binaryObject, result);
    }

    @Test
    public void testUpdatesTranscript() throws IOException {
        var request = new TranscriptUpdateRequest();
        request.setAgent(agent);
        request.setTranscriptText(TRANSCRIPT_TEXT);
        request.setPidString(pidString);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getResource()).thenReturn(resource);
        when(resource.hasProperty(Cdr.hasTranscript)).thenReturn(true);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateTranscript(request);

        // Verify ACL check
        verify(aclService).assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

        // Verify transcript creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(TRANSCRIPT.getMimetype(), capturedVersion.getContentType());
        assertEquals(TRANSCRIPT.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(TRANSCRIPT_TEXT, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify relationship creation is NOT called
        verify(repositoryObjectFactory, never()).createRelationship(any(), any(), any());

        // Verify operation message
        verify(operationsMessageSender).sendUpdateDescriptionOperation(eq("test_user"), anyList(), eq(IndexingPriority.normal));

        assertEquals(binaryObject, result);
    }

    @Test
    public void testSetsNewTranscriptNoMessageSending() throws IOException {
        service.setSendsMessages(false);

        var request = new TranscriptUpdateRequest();
        request.setAgent(agent);
        request.setTranscriptText(TRANSCRIPT_TEXT);
        request.setPidString(pidString);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(transcriptPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateTranscript(request);

        // Verify transcript creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(TRANSCRIPT.getMimetype(), capturedVersion.getContentType());
        assertEquals(TRANSCRIPT.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(TRANSCRIPT_TEXT, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify operation message is not sent
        verify(operationsMessageSender, never()).sendUpdateDescriptionOperation(any(), anyList(), any());

        assertEquals(binaryObject, result);
    }
}
