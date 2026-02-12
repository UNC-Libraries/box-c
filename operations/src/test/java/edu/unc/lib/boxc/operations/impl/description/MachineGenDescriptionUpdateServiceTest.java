package edu.unc.lib.boxc.operations.impl.description;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class MachineGenDescriptionUpdateServiceTest {
    private static final String FILE_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    private PID filePid;
    private MachineGenDescriptionUpdateService service;
    private MachineGenDescriptionRequest request;
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private AccessGroupSet mockAccessSet;
    @Mock
    private AgentPrincipals mockAgent;
    @Mock
    private FileObject fileObject;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        service = new MachineGenDescriptionUpdateService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        filePid = PIDs.get(FILE_UUID);

        request = new MachineGenDescriptionRequest();
        request.setAgent(mockAgent);
        request.setDescription("Best machine generated words ever");
        request.setPidString(FILE_UUID);

        when(mockAgent.getUsername()).thenReturn("user");
        when(mockAgent.getPrincipals()).thenReturn(mockAccessSet);
        when(repoObjLoader.getFileObject(eq(filePid))).thenReturn(fileObject);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessToViewMetadataTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(
                    anyString(), eq(filePid), any(), eq(Permission.viewMetadata));
            service.updateMachineGenDescription(request);
        });
    }

    @Test
    public void repoObjectIsNotFileObjectTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doThrow(new ObjectTypeMismatchException("not a file object"))
                    .when(repoObjLoader).getFileObject(eq(filePid));
            service.updateMachineGenDescription(request);
        });
    }

    @Test
    public void successTest() {
        service.updateMachineGenDescription(request);
        verify(repositoryObjectFactory).createExclusiveRelationship(eq(fileObject),
                eq(Cdr.hasMachineGenDescription), anyString());
    }
}
