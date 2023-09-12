package edu.unc.lib.boxc.operations.impl.edit;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getMdDescriptivePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.validation.MODSValidator;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.DatastreamVersion;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 *
 * @author harring
 *
 */
public class UpdateDescriptionServiceTest {

    private static final String FILE_CONTENT = "Some content";
    private AutoCloseable closeable;

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private MODSValidator modsValidator;
    @Mock
    private StorageLocation destination;
    @Mock
    private BinaryTransferSession transferSession;
    @Mock
    private VersionedDatastreamService versioningService;
    @Mock
    private BinaryObject mockDescBin;

    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private ContentObject obj;

    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;
    @Captor
    private ArgumentCaptor<DatastreamVersion> versionCaptor;

    private UpdateDescriptionService service;
    private PID objPid;
    private PID modsPid;
    private InputStream modsStream;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void init() throws Exception{
        closeable = openMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("username");
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(obj);
        when(messageSender.sendUpdateDescriptionOperation(anyString(), any(Collection.class)))
                .thenReturn("message_id");

        objPid = PIDs.get(UUID.randomUUID().toString());
        modsPid = getMdDescriptivePid(objPid);
        modsStream = new ByteArrayInputStream(FILE_CONTENT.getBytes());

        when(versioningService.addVersion(any(DatastreamVersion.class))).thenReturn(mockDescBin);
        when(obj.getPid()).thenReturn(objPid);

        service = new UpdateDescriptionService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setOperationsMessageSender(messageSender);
        service.setModsValidator(modsValidator);
        service.setVersionedDatastreamService(versioningService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void updateDescriptionTest() throws Exception {
        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        verify(versioningService).addVersion(versionCaptor.capture());
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(modsPid, version.getDsPid());
        assertEquals("text/xml", version.getContentType());
        assertEquals(FILE_CONTENT, IOUtils.toString(version.getContentStream()));

        assertMessageSent();
    }

    @Test
    public void updateDescriptionAlreadyExistsTest() throws Exception {
        when(repoObjFactory.objectExists(modsPid.getRepositoryUri())).thenReturn(true);

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        verify(versioningService).addVersion(versionCaptor.capture());
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(modsPid, version.getDsPid());
        assertEquals("text/xml", version.getContentType());
        assertEquals(FILE_CONTENT, IOUtils.toString(version.getContentStream()));

        assertMessageSent();
    }

    @Test
    public void insufficientAccessTest() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(objPid), any(), any(Permission.class));

            service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));
        });
    }

    @Test
    public void insufficientAccessDisableAccessCheckTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(objPid), any(), any(Permission.class));

        service.setChecksAccess(false);

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        verify(versioningService).addVersion(any());
        assertMessageSent();
    }

    @Test
    public void invalidModsTest() throws Exception {
        Assertions.assertThrows(MetadataValidationException.class, () -> {
            doThrow(new MetadataValidationException()).when(modsValidator).validate(any(InputStream.class));

            service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));
        });
    }

    @Test
    public void invalidModsValidationOff() throws Exception {
        service.setValidate(false);
        doThrow(new MetadataValidationException()).when(modsValidator).validate(any(InputStream.class));

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream)
                .withTransferSession(transferSession));
    }

    @Test
    public void updateDescriptionProvidedSession() throws Exception {
        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream)
                .withTransferSession(transferSession));

        verify(versioningService).addVersion(versionCaptor.capture());
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(FILE_CONTENT, IOUtils.toString(version.getContentStream()));
        assertEquals(transferSession, version.getTransferSession());

        assertMessageSent();
    }

    @Test
    public void updateDescriptionDisableMassgesTest() throws Exception {
        service.setSendsMessages(false);

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        verify(versioningService).addVersion(versionCaptor.capture());
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(modsPid, version.getDsPid());
        assertEquals("text/xml", version.getContentType());
        assertEquals(FILE_CONTENT, IOUtils.toString(version.getContentStream()));

        verify(messageSender, never()).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
    }

    private void assertMessageSent() {
        verify(messageSender).sendUpdateDescriptionOperation(
                anyString(), pidsCaptor.capture(), any());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(1, pids.size());
        assertTrue(pids.contains(objPid));
    }
}
