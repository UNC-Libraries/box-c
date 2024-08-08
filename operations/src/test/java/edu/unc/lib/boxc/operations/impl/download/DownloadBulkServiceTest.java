package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static edu.unc.lib.boxc.operations.impl.download.DownloadBulkService.getZipFilename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DownloadBulkServiceTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private AccessGroupSet mockAccessSet;
    @Mock
    private AgentPrincipals mockAgent;
    @Mock
    private WorkObject parentWork;
    @Mock
    private FileObject fileObject1, fileObject2;
    @TempDir
    public Path zipStorageBasePath;
    private PID parentPid;
    private DownloadBulkService service;
    private DownloadBulkRequest request;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        service = new DownloadBulkService();
        service.setAclService(aclService);
        service.setRepoObjLoader(repoObjLoader);
        service.setBasePath(zipStorageBasePath.toString());
        parentPid = PIDs.get(PARENT_UUID);
        request = new DownloadBulkRequest(PARENT_UUID, mockAgent);

        when(mockAgent.getUsername()).thenReturn("user");
        when(mockAgent.getPrincipals()).thenReturn(mockAccessSet);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(parentPid), any(), eq(Permission.viewOriginal));

            service.downloadBulk(request);
        });
    }

    @Test
    public void notAWorkTest() {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.downloadBulk(request);
        });
    }

    @Test
    public void workObjectDoesNotHaveFileObjectsTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFile(PARENT_UUID, 0);
    }

    @Test
    public void noAccessToFileObjectsTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1));
        when(aclService.hasAccess(eq(fileObject1.getPid()), any(),
                eq(Permission.viewOriginal))).thenReturn(false);
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFile(PARENT_UUID, 0);
    }

    @Test
    public void accessToOnlyOneFileObjectTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1, fileObject2));
        service.downloadBulk(request);
        // the zip file should have one entry
        assertZipFile(PARENT_UUID, 1);
    }

    @Test
    public void noOriginalFilesTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1));
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFile(PARENT_UUID, 0);
    }

    @Test
    public void successTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1));
        service.downloadBulk(request);

        // the zip file should have one entry
        assertZipFile(PARENT_UUID, 1);
    }

    private void assertZipFile(String workPidString, int numberOfEntries) throws IOException {
        var zipFilePath = zipStorageBasePath.toString() + getZipFilename(workPidString);
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            //Enumeration<? extends ZipEntry> entries = zipFile.entries();
            assertEquals(numberOfEntries, zipFile.size());
        }
    }
}
