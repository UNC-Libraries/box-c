package edu.unc.lib.boxc.operations.impl.machineGenerated;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.machineGenerated.MachineGenRequest;
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

import static edu.unc.lib.boxc.operations.impl.utils.ExternalDerivativesUtil.getDerivativePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class MachineGenUpdateServiceTest {
    private static final String FILE_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    private PID filePid;
    private MachineGenUpdateService service;
    private MachineGenRequest request;
    private String derivBasePath;
    @TempDir
    public Path tmpFolder;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private FileObject fileObject;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        derivBasePath = tmpFolder.toString();

        service = new MachineGenUpdateService();
        service.setDerivativeBasePath(derivBasePath);
        service.setRepositoryObjectLoader(repoObjLoader);
        filePid = PIDs.get(FILE_UUID);

        request = new MachineGenRequest();
        request.setText("Best machine generated words ever");
        request.setPidString(FILE_UUID);

        when(repoObjLoader.getFileObject(eq(filePid))).thenReturn(fileObject);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void repoObjectIsNotFileObjectTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doThrow(new ObjectTypeMismatchException("not a file object"))
                    .when(repoObjLoader).getFileObject(eq(filePid));
            service.updateMachineGenText(request);
        });
    }

    @Test
    public void fileDirectoriesNotCreatedTest() {
        try (MockedStatic<FileUtils> mockedStatic = mockStatic(FileUtils.class)) {
            mockedStatic.when(() -> FileUtils.write(any(), any(), eq(UTF_8)))
                    .thenThrow(new IOException());
            Assertions.assertThrows(ServiceException.class, () -> {
                service.updateMachineGenText(request);
            });
        }
    }

    @Test
    public void successTest() throws IOException {
        var id = filePid.getId();
        var derivPath = service.updateMachineGenText(request);
        var path = getDerivativePath(derivBasePath, id);

        assertTrue(Files.exists(path));
        assertEquals(derivPath, path);
        assertEquals(Files.readString(derivPath), request.getText());
    }
}
