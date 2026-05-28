package edu.unc.lib.boxc.operations.impl.mimetype;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.InvalidMimeTypeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CorrectMimetypesServiceTest {
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String USER_PRINC = "user";
    private static final String GRP_PRINC = "group";

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Mock
    private TransactionManager txManager;
    @Mock
    private FedoraTransaction tx;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PremisLoggerFactory premisLoggerFactory;

    private CorrectMimetypesService service;
    private AgentPrincipals agent;

    @BeforeEach
    public void setup() {
        service = new CorrectMimetypesService();
        service.setAclService(aclService);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setPremisLoggerFactory(premisLoggerFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setTransactionManager(txManager);

        agent = new AgentPrincipalsImpl(USER_PRINC, new AccessGroupSetImpl(GRP_PRINC));
    }

    @Test
    public void testNoAgent() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.correctMimetypes(csv(CHILD1_UUID + ",image/tiff"), null);
        });

        verifyNoInteractions(repoObjLoader, repoObjFactory, operationsMessageSender);
    }

    @Test
    public void testNullInputStream() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.correctMimetypes(null, agent);
        });

        verifyNoInteractions(repoObjLoader, repoObjFactory, operationsMessageSender);
    }

    @Test
    public void testInsufficientPermissions() {
        PID filePid = PIDs.get(CHILD1_UUID);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(
                        anyString(),
                        eq(filePid),
                        eq(agent.getPrincipals()),
                        eq(Permission.editDescription));

        assertThrows(AccessRestrictionException.class, () -> {
            service.correctMimetypes(csv(CHILD1_UUID + ",image/tiff"), agent);
        });

        verify(repoObjLoader, never()).getRepositoryObject(any());
        verify(repoObjFactory, never()).createOrTransformObject(any(), any());
        verify(operationsMessageSender, never()).sendUpdateDescriptionOperation(anyString(), anyList());
    }

    @Test
    public void testEmptyId() throws Exception {
        assertThrows(NotFoundException.class, () -> {
            service.correctMimetypes(csv(",image/tiff"), agent);
        });

        verifyNoInteractions(repoObjLoader, repoObjFactory, operationsMessageSender);
    }

    @Test
    public void testEmptyMimetype() throws Exception {
        assertThrows(NotFoundException.class, () -> {
            service.correctMimetypes(csv(CHILD1_UUID + ","), agent);
        });

        verifyNoInteractions(repoObjLoader, repoObjFactory, operationsMessageSender);
    }

    @Test
    public void testInvalidMimetype() {
        assertThrows(InvalidMimeTypeException.class, () -> {
            service.correctMimetypes(csv(CHILD1_UUID + ",image"), agent);
        });

        verifyNoInteractions(repoObjLoader, repoObjFactory, operationsMessageSender);
        verify(txManager, never()).startTransaction();
    }

    @Test
    public void testNotFileObject() {
        PID filePid = PIDs.get(CHILD1_UUID);

        RepositoryObject nonFileObject = mock(RepositoryObject.class);
        when(nonFileObject.getPid()).thenReturn(filePid);

        when(repoObjLoader.getRepositoryObject(filePid)).thenReturn(nonFileObject);

        var e = assertThrows(IllegalArgumentException.class, () -> {
            service.correctMimetypes(csv(CHILD1_UUID + ",image/tiff"), agent);
        });

        assertTrue(e.getMessage().contains("Cannot update mimetype for non-file object "));

        verify(repoObjLoader).getRepositoryObject(filePid);
        verify(repoObjFactory, never()).createOrTransformObject(any(), any());
        verify(operationsMessageSender, never()).sendUpdateDescriptionOperation(anyString(), anyList());
    }

    @Test
    public void testCorrectMimetypes() throws Exception {
        when(txManager.startTransaction()).thenReturn(tx);
        PID pid1 = PIDs.get(CHILD1_UUID);
        PID pid2 = PIDs.get(CHILD2_UUID);

        mockFileObject(CHILD1_UUID, "image/png");
        mockFileObject(CHILD2_UUID, "image/jpeg");

        List<PID> result = service.correctMimetypes(
                csv(CHILD1_UUID + ",image/tiff", CHILD2_UUID + ",image/tiff"),
                agent);

        assertEquals(List.of(pid1, pid2), result);

        verify(repoObjLoader).getRepositoryObject(pid1);
        verify(repoObjLoader).getRepositoryObject(pid2);

        verify(repoObjFactory, times(2))
                .createOrUpdateBinary(any(), any(), any(), any(), any(), any(), any());

        verify(operationsMessageSender).sendUpdateDescriptionOperation(
                eq(agent.getUsername()),
                eq(Collections.singletonList(pid1)));

        verify(operationsMessageSender).sendUpdateDescriptionOperation(
                eq(agent.getUsername()),
                eq(Collections.singletonList(pid2)));

        verify(txManager, times(2)).startTransaction();
        verify(tx, times(2)).close();
        verify(tx, never()).cancel(any());
    }

    private InputStream csv(String... rows) {
        String body = String.join(",", CorrectMimetypesService.CSV_HEADERS) + "\n"
                + String.join("\n", rows);
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    private void mockFileObject(String fileUuid, String oldMimetype) {
        PID filePid = PIDs.get(fileUuid);
        PID originalFilePid = DatastreamPids.getOriginalFilePid(filePid);

        FileObject fileObject = mock(FileObject.class);
        BinaryObject binaryObject = mock(BinaryObject.class);

        when(fileObject.getOriginalFile()).thenReturn(binaryObject);

        when(binaryObject.getPid()).thenReturn(originalFilePid);
        when(binaryObject.getMimetype()).thenReturn(oldMimetype);
        when(binaryObject.getUri()).thenReturn(originalFilePid.getRepositoryUri());

        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource(originalFilePid.getRepositoryPath());
        resource.addProperty(CdrDeposit.mimetype, oldMimetype);

        when(binaryObject.getModel(true)).thenReturn(model);

        when(repoObjLoader.getRepositoryObject(filePid)).thenReturn(fileObject);
    }
}
