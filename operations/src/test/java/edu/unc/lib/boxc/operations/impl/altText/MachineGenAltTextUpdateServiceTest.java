package edu.unc.lib.boxc.operations.impl.altText;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.operations.impl.utils.MachineGenUtil.getDerivativePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class MachineGenAltTextUpdateServiceTest {

    private static final String FILE_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    private PID filePid;
    private MachineGenAltTextUpdateService service;
    private MachineGenAltTextRequest request;
    private String derivBasePath;
    @TempDir
    public Path tmpFolder;
    @Mock
    private AccessControlService aclService;
    @Mock
    private AccessGroupSet mockAccessSet;
    @Mock
    private AgentPrincipals mockAgent;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private FileObject fileObject;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        derivBasePath = tmpFolder.toString();

        service = new MachineGenAltTextUpdateService();
        service.setAclService(aclService);
        service.setDerivativeBasePath(derivBasePath);
        service.setRepositoryObjectLoader(repoObjLoader);
        filePid = PIDs.get(FILE_UUID);

        request = new MachineGenAltTextRequest();
        request.setAgent(mockAgent);
        request.setAltText("Best machine generated words ever");
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
    public void noAccessToEditDescriptionTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(
                    anyString(), eq(filePid), any(), eq(Permission.editDescription));
            service.updateMachineGenAltText(request);
        });
    }

    @Test
    public void repoObjectIsNotFileObjectTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doThrow(new ObjectTypeMismatchException("not a file object"))
                    .when(repoObjLoader).getFileObject(eq(filePid));
            service.updateMachineGenAltText(request);
        });
    }

    @Test
    public void fileDirectoriesNotCreatedTest() {
        try (MockedStatic<FileUtils> mockedStatic = mockStatic(FileUtils.class)) {
            mockedStatic.when(() -> FileUtils.write(any(), any(), eq(UTF_8)))
                    .thenThrow(new IOException());
            Assertions.assertThrows(ServiceException.class, () -> {
                service.updateMachineGenAltText(request);
            });
        }
    }

    @Test
    public void successTest() throws IOException {
        var id = filePid.getId();
        var path = getDerivativePath(derivBasePath, id);
        var derivPath = service.updateMachineGenAltText(request);

        assertTrue(Files.exists(path));
        assertEquals(derivPath, path);
        assertEquals(Files.readString(derivPath), request.getAltText());
    }
}
