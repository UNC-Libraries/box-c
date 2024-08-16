package edu.unc.lib.boxc.web.services.rest;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WireMockTest(httpPort = 46887)
public class DownloadBulkControllerIT {
    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID WORK_PID = PIDs.get(WORK_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private WorkObject workObject;
    @TempDir
    public Path tmpFolder;
    @InjectMocks
    private DownloadBulkController controller;
    private DownloadBulkService downloadBulkService;
    private MockMvc mvc;
    private FileInputStream fileInputStream;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() throws FileNotFoundException {
        closeable = openMocks(this);
        downloadBulkService = new DownloadBulkService();
        downloadBulkService.setAclService(aclService);
        downloadBulkService.setBasePath(tmpFolder);
        downloadBulkService.setRepoObjLoader(repositoryObjectLoader);
        controller.setDownloadBulkService(downloadBulkService);
        fileInputStream = new FileInputStream("src/test/resources/__files/bunny.jpg");
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        TestHelper.setContentBase("http://localhost:48085/rest");
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noAccessTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(
                anyString(), any(), any(AccessGroupSetImpl.class), eq(viewOriginal));

        mvc.perform(get("/bulkDownload/" + WORK_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void successTest() throws Exception {
        when(repositoryObjectLoader.getWorkObject(any())).thenReturn(workObject);
        var result = mvc.perform(get("/bulkDownload/" + WORK_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var zipFile = (ZipFile) result;

        assertZipFiles(zipFile);
    }

    private void assertZipFiles(ZipFile zipFile) throws IOException {
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            var zipInputStream = zipFile.getInputStream(entry);
            assertEquals(fileInputStream, zipInputStream);
        }
    }
}
