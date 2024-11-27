package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.web.services.utils.DownloadTestHelper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DownloadBulkControllerIT {
    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String FILE_ID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private WorkObject workObject;
    @Mock
    private FileObject fileObject;
    private PID filePid;
    private PID workPid;
    @TempDir
    public Path tmpFolder;
    @InjectMocks
    private DownloadBulkController controller;
    private MockMvc mvc;
    private FileInputStream fileInputStream;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() throws FileNotFoundException {
        closeable = openMocks(this);
        DownloadBulkService downloadBulkService = new DownloadBulkService();
        downloadBulkService.setAclService(aclService);
        downloadBulkService.setBasePath(tmpFolder);
        downloadBulkService.setRepoObjLoader(repositoryObjectLoader);
        downloadBulkService.setFileLimit(5);
        controller.setDownloadBulkService(downloadBulkService);
        fileInputStream = new FileInputStream("src/test/resources/__files/bunny.jpg");
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        TestHelper.setContentBase("http://localhost:48085/rest");
        workPid = PIDs.get(WORK_ID);
        filePid = PIDs.get(FILE_ID);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessTest() throws Exception {
        when(aclService.hasAccess(eq(workPid), any(), eq(viewOriginal))).thenReturn(false);

        mvc.perform(get("/bulkDownload/" + WORK_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void userNotLoggedInTest() throws Exception {
        GroupsThreadStore.clearStore();
        when(aclService.hasAccess(eq(workPid), any(), eq(viewOriginal))).thenReturn(true);


        mvc.perform(get("/bulkDownload/" + WORK_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void successTest() throws Exception {
        when(repositoryObjectLoader.getWorkObject(any())).thenReturn(workObject);
        when(workObject.getMembers()).thenReturn(List.of(fileObject));
        when(fileObject.getPid()).thenReturn(filePid);
        var binObj = mock(BinaryObject.class);
        when(fileObject.getOriginalFile()).thenReturn(binObj);
        when(binObj.getBinaryStream()).thenReturn(fileInputStream);
        when(binObj.getFilename()).thenReturn("bunny.jpg");

        when(aclService.hasAccess(eq(workPid), any(), eq(viewOriginal))).thenReturn(true);
        when(aclService.hasAccess(eq(filePid), any(),
                eq(Permission.viewOriginal))).thenReturn(true);

        var result = mvc.perform(get("/bulkDownload/" + WORK_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var response = result.getResponse();
        var bytes = response.getContentAsByteArray();
        var zipInputStream =  new ZipInputStream(new ByteArrayInputStream(bytes));

        assertZipFiles(zipInputStream, List.of("bunny.jpg"));
        assertEquals("application/zip", response.getHeader("Content-Type"));
    }

    private void assertZipFiles(ZipInputStream stream, List<String> filenames) throws IOException {
        var actualFilenames = new ArrayList<String>();
        try (stream) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                actualFilenames.add(entry.getName());
                var actualContent = IOUtils.toByteArray(stream);
                DownloadTestHelper.assertCorrectImageReturned(actualContent);
            }
        }
        assertEquals(filenames, actualFilenames);
    }
}
