package edu.unc.lib.boxc.operations.impl.datastreams;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
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
import edu.unc.lib.boxc.operations.jms.datastreams.DatastreamUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.search.api.models.Datastream;
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
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
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

public class DatastreamUpdateServiceTest {
    private static final String DATASTREAM_TEXT = "I'm a full description";
    private AutoCloseable closeable;
    private DatastreamUpdateService service;
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
    private PID datastreamPid;
    private AgentPrincipals agent;
    private static final AccessGroupSet ACCESS_GROUPS = new AccessGroupSetImpl("test_group");

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new DatastreamUpdateService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setVersionedDatastreamService(versioningService);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setSendsMessages(true);
        pid = makePid();
        pidString = pid.getId();
        agent = new AgentPrincipalsImpl("test_user", ACCESS_GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermissionToUpdateDatastream() {
        var request = new DatastreamUpdateRequest();
        request.setAgent(agent);
        request.setText(DATASTREAM_TEXT);
        request.setPidString(pidString);
        request.setDatastreamType(DatastreamType.FULL_DESCRIPTION);

        doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(Permission.editDescription));

        assertThrows(AccessRestrictionException.class, () -> service.updateDatastreamText(request));

        verifyNoInteractions(repositoryObjectFactory, versioningService, operationsMessageSender);
    }

    @Test
    public void testSetsNewDatastreamText() throws IOException {
        var fullDescPid = DatastreamPids.getFullDescriptionPid(pid);
        var request = new DatastreamUpdateRequest();
        request.setAgent(agent);
        request.setText(DATASTREAM_TEXT);
        request.setPidString(pidString);
        request.setDatastreamType(DatastreamType.FULL_DESCRIPTION);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(fullDescPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        var result = service.updateDatastreamText(request);

        // Verify ACL check
        verify(aclService).assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

        // Verify full description creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(FULL_DESCRIPTION.getMimetype(), capturedVersion.getContentType());
        assertEquals(FULL_DESCRIPTION.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(DATASTREAM_TEXT, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify relationship creation
        var expectedRelationObj = createResource(fullDescPid.getRepositoryPath());
        verify(repositoryObjectFactory).createRelationship(eq(fileObject), eq(Cdr.hasFullDescription), eq(expectedRelationObj));

        // Verify operation message
        verify(operationsMessageSender).sendUpdateDescriptionOperation(eq("test_user"), anyList(), eq(IndexingPriority.normal));

        assertEquals(binaryObject, result);
    }

    @Test
    public void testNoMessageSending() throws IOException {
        var fullDescPid = DatastreamPids.getFullDescriptionPid(pid);
        service.setSendsMessages(false);

        var request = new DatastreamUpdateRequest();
        request.setAgent(agent);
        request.setText(DATASTREAM_TEXT);
        request.setPidString(pidString);
        request.setDatastreamType(DatastreamType.FULL_DESCRIPTION);

        when(repositoryObjectLoader.getFileObject(eq(pid))).thenReturn(fileObject);
        when(repositoryObjectFactory.objectExists(fullDescPid.getRepositoryUri())).thenReturn(false);
        when(versioningService.addVersion(any())).thenReturn(binaryObject);

        service.updateDatastreamText(request);

        // Verify full description creation
        ArgumentCaptor<VersionedDatastreamService.DatastreamVersion> captor = ArgumentCaptor.forClass(VersionedDatastreamService.DatastreamVersion.class);
        verify(versioningService).addVersion(captor.capture());
        var capturedVersion = captor.getValue();
        assertEquals(FULL_DESCRIPTION.getMimetype(), capturedVersion.getContentType());
        assertEquals(FULL_DESCRIPTION.getDefaultFilename(), capturedVersion.getFilename());
        assertEquals(DATASTREAM_TEXT, IOUtils.toString(capturedVersion.getContentStream(), StandardCharsets.UTF_8));

        // Verify operation message
        verify(operationsMessageSender, never()).sendUpdateDescriptionOperation(any(), anyList(), any());
    }

    @Test
    public void invalidDatastreamTypeTest() {
        var request = new DatastreamUpdateRequest();
        request.setAgent(agent);
        request.setText(DATASTREAM_TEXT);
        request.setPidString(pidString);
        request.setDatastreamType(DatastreamType.ACCESS_SURROGATE);

        assertThrows(IllegalArgumentException.class, () -> service.updateDatastreamText(request));

        verifyNoInteractions(repositoryObjectFactory, versioningService, operationsMessageSender);
    }
}
