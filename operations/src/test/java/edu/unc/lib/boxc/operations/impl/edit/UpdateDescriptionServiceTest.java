package edu.unc.lib.boxc.operations.impl.edit;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.validation.MODSValidator;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.DatastreamVersion;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import org.apache.commons.io.IOUtils;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getMdDescriptivePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author harring
 *
 */
public class UpdateDescriptionServiceTest {
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
    private String modsString;
    private Path modsPath;

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
        modsPath = Path.of("src/test/resources/samples/mods.xml");
        modsStream = Files.newInputStream(modsPath);

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

        assertUpdateSuccessful();
        assertMessageSent();
    }

    @Test
    public void updateDescriptionFromDocumentTest() throws Exception {
        var modsDoc = createSAXBuilder().build(Files.newInputStream(modsPath));
        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsDoc));

        assertUpdateSuccessful();
        assertMessageSent();
    }

    @Test
    public void updateWithContentObjectTest() throws Exception {
        var contentObj = mock(WorkObject.class);
        when(contentObj.getPid()).thenReturn(objPid);
        service.updateDescription(new UpdateDescriptionRequest(agent, contentObj, modsStream));

        assertUpdateSuccessful();
        assertMessageSent();
        verify(repoObjLoader, never()).getRepositoryObject(any());
    }

    @Test
    public void updateWithUnmodifiedSinceTest() throws Exception {
        var unmodifedSince = Instant.now();
        var request = new UpdateDescriptionRequest(agent, objPid, modsStream);
        request.withUnmodifiedSince(unmodifedSince);
        service.updateDescription(request);

        assertUpdateSuccessful();
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(unmodifedSince, version.getUnmodifiedSince());
        assertMessageSent();
    }

    @Test
    public void updateWithIndexingPriorityTest() throws Exception {
        var request = new UpdateDescriptionRequest(agent, objPid, modsStream);
        request.withPriority(IndexingPriority.high);
        service.updateDescription(request);

        assertUpdateSuccessful();
        assertMessageSent();
        verify(messageSender).sendUpdateDescriptionOperation(
                anyString(), any(), eq(IndexingPriority.high));
    }

    @Test
    public void updateDescriptionAlreadyExistsTest() throws Exception {
        when(repoObjFactory.objectExists(modsPid.getRepositoryUri())).thenReturn(true);

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        assertUpdateSuccessful();
        assertMessageSent();
    }

    @Test
    public void insufficientAccessTest() {
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
    public void invalidModsTest() {
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
    public void invalidXMLTest() {
        Assertions.assertThrows(MetadataValidationException.class, () -> {
            var badXmlStream = new ByteArrayInputStream("BAD XML".getBytes());
            service.updateDescription(new UpdateDescriptionRequest(agent, objPid, badXmlStream));
        });
    }

    @Test
    public void updateDescriptionProvidedSession() throws Exception {
        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream)
                .withTransferSession(transferSession));

        assertUpdateSuccessful();
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(transferSession, version.getTransferSession());

        assertMessageSent();
    }

    @Test
    public void updateDescriptionDisableMessagesTest() throws Exception {
        service.setSendsMessages(false);

        service.updateDescription(new UpdateDescriptionRequest(agent, objPid, modsStream));

        assertUpdateSuccessful();
        verify(messageSender, never()).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
    }

    private void assertMessageSent() {
        verify(messageSender).sendUpdateDescriptionOperation(
                anyString(), pidsCaptor.capture(), any());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(1, pids.size());
        assertTrue(pids.contains(objPid));
    }

    private void assertUpdateSuccessful() throws Exception {
        verify(versioningService).addVersion(versionCaptor.capture());
        DatastreamVersion version = versionCaptor.getValue();
        assertEquals(modsPid, version.getDsPid());
        assertEquals("text/xml", version.getContentType());
        assertEquals(Files.readString(Path.of("src/test/resources/samples/modsNormalized.xml")),
                IOUtils.toString(version.getContentStream()));
    }
}
