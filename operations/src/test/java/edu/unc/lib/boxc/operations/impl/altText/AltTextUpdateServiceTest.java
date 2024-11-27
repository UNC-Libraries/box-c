package edu.unc.lib.boxc.operations.impl.altText;

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
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.altText.AltTextUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.api.DatastreamType.ALT_TEXT;
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

/**
 * @author bbpennel
 */
public class AltTextUpdateServiceTest {

    private AltTextUpdateService service;

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
    private PID pid;
    private String pidString;
    private PID altTextPid;
    private AgentPrincipals agent;
    private static final AccessGroupSet ACCESS_GROUPS = new AccessGroupSetImpl("test_group");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AltTextUpdateService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setVersionedDatastreamService(versioningService);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setSendsMessages(true);
        pid = TestHelper.makePid();
        pidString = pid.getId();
        altTextPid = DatastreamPids.getAltTextPid(pid);
        agent = new AgentPrincipalsImpl("test_user", ACCESS_GROUPS);
    }

    @Test
    void testUpdateAltTextCreatesNewAltText() throws Exception {
        var altTextContent = "Sample Alt Text";
        var request = new AltTextUpdateRequest();
        request.setAltText(altTextContent);
        request.setPidString(pidString);
        request.setAgent(agent);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(altTextPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateAltText(request);

        // Verify ACL check
        verify(aclService).assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

        // Verify alt text creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(ALT_TEXT.getMimetype(), capturedVersion.getContentType());
        assertEquals(ALT_TEXT.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(altTextContent, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify relationship creation
        var expectedRelationObj = createResource(altTextPid.getRepositoryPath());
        verify(repositoryObjectFactory).createRelationship(eq(fileObject), eq(Cdr.hasAltText), eq(expectedRelationObj));

        // Verify operation message
        verify(operationsMessageSender).sendUpdateDescriptionOperation(eq("test_user"), anyList(), eq(IndexingPriority.normal));

        assertEquals(binaryObject, result);
    }

    @Test
    void testUpdateAltTextUpdatesExistingAltText() throws Exception {
        var altTextContent = "Updated Alt Text";
        var request = new AltTextUpdateRequest();
        request.setAltText(altTextContent);
        request.setPidString(pidString);
        request.setAgent(agent);

        when(repositoryObjectLoader.getFileObject(any())).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(altTextPid.getRepositoryUri())).thenReturn(true);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateAltText(request);

        // Verify ACL check
        verify(aclService).assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

        // Verify alt text update
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(ALT_TEXT.getMimetype(), capturedVersion.getContentType());
        assertEquals(ALT_TEXT.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(altTextContent, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify relationship creation is NOT called
        verify(repositoryObjectFactory, never()).createRelationship(any(), any(), any());

        // Verify operation message
        verify(operationsMessageSender).sendUpdateDescriptionOperation(eq("test_user"), anyList(), eq(IndexingPriority.normal));

        assertEquals(binaryObject, result);
    }

    @Test
    void testUpdateAltTextFailsACLCheck() {
        var altTextContent = "Sample Alt Text";
        var request = new AltTextUpdateRequest();
        request.setAltText(altTextContent);
        request.setPidString(pidString);
        request.setAgent(agent);

        doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(Permission.editDescription));

        assertThrows(AccessRestrictionException.class, () -> service.updateAltText(request));

        verifyNoInteractions(repositoryObjectFactory, versioningService, operationsMessageSender);
    }

    @Test
    void testUpdateAltTextDoesNotSendMessageWhenTurnedOff() throws Exception {
        service.setSendsMessages(false);

        var altTextContent = "Sample Alt Text";
        var request = new AltTextUpdateRequest();
        request.setAltText(altTextContent);
        request.setPidString(pidString);
        request.setAgent(agent);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(altTextPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateAltText(request);

        // Verify alt text creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(altTextContent, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify operation message
        verify(operationsMessageSender, never()).sendUpdateDescriptionOperation(any(), anyList(), any());
    }
}
