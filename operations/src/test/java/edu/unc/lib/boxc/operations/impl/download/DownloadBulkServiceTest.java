package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static edu.unc.lib.boxc.operations.impl.download.DownloadBulkService.getZipFilename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DownloadBulkServiceTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String FILENAME1 = "filename1.txt";
    private static final String FILENAME2 = "filename2.txt";
    private AutoCloseable closeable;
    private PID parentPid;
    private PID fileObject1Pid;
    private PID fileObject2Pid;
    private DownloadBulkService service;
    private DownloadBulkRequest request;
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

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        service = new DownloadBulkService();
        service.setAclService(aclService);
        service.setRepoObjLoader(repoObjLoader);
        service.setBasePath(zipStorageBasePath);
        parentPid = PIDs.get(PARENT_UUID);
        fileObject1Pid = PIDs.get(CHILD1_UUID);
        fileObject2Pid = PIDs.get(CHILD2_UUID);
        request = new DownloadBulkRequest(PARENT_UUID, mockAgent);

        when(mockAgent.getUsername()).thenReturn("user");
        when(mockAgent.getPrincipals()).thenReturn(mockAccessSet);
        when(fileObject1.getPid()).thenReturn(fileObject1Pid);
        when(fileObject2.getPid()).thenReturn(fileObject2Pid);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessToWorkObjectTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(
                    anyString(), eq(parentPid), any(), eq(Permission.viewOriginal));

            service.downloadBulk(request);
        });
    }

    @Test
    public void notAWorkTest() {
        when(repoObjLoader.getWorkObject(any(PID.class)))
                .thenThrow(ObjectTypeMismatchException.class);
        Assertions.assertThrows(NotFoundException.class, () -> {
            service.downloadBulk(request);
        });
    }

    @Test
    public void workObjectDoesNotHaveFileObjectsTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFiles(List.of(), List.of());
    }

    @Test
    public void noAccessToFileObjectsTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1));
        when(aclService.hasAccess(eq(fileObject1Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(false);
        makeBinaryObject(fileObject1, FILENAME1);
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFiles(List.of(), List.of());
    }

    @Test
    public void accessToOnlyOneFileObjectTest() throws IOException {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1, fileObject2));
        makeBinaryObject(fileObject1, FILENAME1);
        makeBinaryObject(fileObject2, FILENAME2);
        when(aclService.hasAccess(eq(fileObject1Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(fileObject2Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(false);
        service.downloadBulk(request);
        // the zip file should have one entry
        assertZipFiles(List.of(FILENAME1), List.of("flower"));
    }

    @Test
    public void noOriginalFilesTest() throws IOException {
        when(repoObjLoader.getWorkObject(eq(parentPid))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1));
        service.downloadBulk(request);
        // the zip file should be empty
        assertZipFiles(List.of(), List.of());
    }

    @Test
    public void successTest() throws IOException {
        when(repoObjLoader.getWorkObject(eq(parentPid))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1, fileObject2));
        when(aclService.hasAccess(eq(fileObject1Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(fileObject2Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        makeBinaryObject(fileObject1, FILENAME1);
        makeBinaryObject(fileObject2, FILENAME2);

        service.downloadBulk(request);

        // the zip file should have two entries
        assertZipFiles(List.of(FILENAME1, FILENAME2), List.of("flower", "flower"));
    }

    @Test
    public void duplicateFilenameTest() throws IOException {
        when(repoObjLoader.getWorkObject(eq(parentPid))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1, fileObject2));
        when(aclService.hasAccess(eq(fileObject1Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(fileObject2Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        makeBinaryObject(fileObject1, "filename.txt");
        makeBinaryObject(fileObject2, "filename.txt");

        service.downloadBulk(request);

        // the zip file should have two entries
        assertZipFiles(List.of("filename.txt", "filename(1).txt"),
                List.of("flower", "flower"));
    }

    @Test
    public void multipleFilenamesTest() throws IOException {
        var fileObject3 = mock(FileObject.class);
        var fileObject3Pid = makePid();
        when(fileObject3.getPid()).thenReturn(fileObject3Pid);
        when(repoObjLoader.getWorkObject(eq(parentPid))).thenReturn(parentWork);
        when(parentWork.getMembers()).thenReturn(List.of(fileObject1, fileObject2, fileObject3));
        when(aclService.hasAccess(eq(fileObject1Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(fileObject2Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(fileObject3Pid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);
        makeBinaryObject(fileObject1, "filename.txt");
        makeBinaryObject(fileObject2, "filename.txt");
        makeBinaryObject(fileObject3, "flower.txt");

        service.downloadBulk(request);

        // the zip file should have 3 entries
        assertZipFiles(List.of("filename.txt", "filename(1).txt", "flower.txt"),
                List.of("flower", "flower", "flower"));
    }

    private void assertZipFiles(List<String> filenames, List<String> content) throws IOException {
        var zipFilePathTest = zipStorageBasePath.resolve(getZipFilename(PARENT_UUID));
        var actualFilenames = new ArrayList<String>();
        var fileContent = new ArrayList<String>();
        try (ZipFile zipFile = new ZipFile(zipFilePathTest.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                actualFilenames.add(entry.getName());
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    fileContent.add(result);
                }
            }
            assertEquals(filenames, actualFilenames);
            assertEquals(content, fileContent);
        }
    }

    private void makeBinaryObject(FileObject fileObject, String filename) {
        var binObj = mock(BinaryObject.class);
        var binaryStream = IOUtils.toInputStream("flower", StandardCharsets.UTF_8);

        when(fileObject.getOriginalFile()).thenReturn(binObj);
        when(binObj.getBinaryStream()).thenReturn(binaryStream);
        when(binObj.getFilename()).thenReturn(filename);

    }
}
