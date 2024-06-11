package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.AccessControlServiceImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import edu.unc.lib.boxc.web.services.processing.SingleUseKeyService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.DAY_MILLISECONDS;
import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.KEY;
import static edu.unc.lib.boxc.web.services.utils.SingleUseKeyUtil.UUID_TEST;
import static edu.unc.lib.boxc.web.services.utils.SingleUseKeyUtil.generateDefaultCsv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class SingleUseKeyControllerIT extends AbstractAPIIT {
    private AutoCloseable closeable;
    private PID pid;
    private Path csvPath;
    @TempDir
    public Path tmpFolder;

    private SingleUseKeyService singleUseKeyService;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private AnalyticsTrackerUtil analyticsTrackerUtil;
    private FedoraContentService fedoraContentService;
    @InjectMocks
    private SingleUseKeyController controller;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        aclService = mock(AccessControlService.class);
        fedoraContentService = new FedoraContentService();
        fedoraContentService.setAccessControlService(aclService);
        fedoraContentService.setRepositoryObjectLoader(repositoryObjectLoader);
        singleUseKeyService = new SingleUseKeyService();
        controller.setFedoraContentService(fedoraContentService);
        controller.setSingleUseKeyService(singleUseKeyService);
        controller.setAclService(aclService);
        controller.setRepositoryObjectLoader(repositoryObjectLoader);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        pid = makePid();
        csvPath = tmpFolder.resolve("singleUseKey");
        singleUseKeyService.setCsvPath(csvPath);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGenerateNoAccess() throws Exception {
        repositoryObjectFactory.createFileObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(viewHidden));

        mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testGenerateNotAFileObject() throws Exception {
        repositoryObjectFactory.createFolderObject(pid, null);

        mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    // This test intentionally generates a NullPointerException in order to trigger error handling
    @Test
    public void testGenerateFailure() throws Exception {
        var id = pid.getUUID();
        repositoryObjectFactory.createFileObject(pid, null);
        singleUseKeyService.setCsvPath(null);

        mvc.perform(post("/single_use_link/create/" + id))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    public void testGenerateSuccess() throws Exception {
        repositoryObjectFactory.createFileObject(pid, null);

        MvcResult result = mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertNotNull(respMap.get(KEY));
        assertEquals(pid.getUUID(), respMap.get("target_id"));
        assertNotNull(respMap.get("expires"));
    }

    @Test
    public void testDownloadAccessKeyInvalidCsvDoesNotExist() throws Exception {
        var accessKey = SingleUseKeyService.getKey();
        mvc.perform(get("/single_use_link/" + accessKey))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    public void testDownloadAccessKeyIsNotInCsv() throws Exception {
        var accessKey = SingleUseKeyService.getKey();
        var expirationTimestamp = System.currentTimeMillis() + DAY_MILLISECONDS;
        generateDefaultCsv(csvPath, null, expirationTimestamp);
        mvc.perform(get("/single_use_link/" + accessKey))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testDownloadSuccess() throws Exception {
        var content = "binary content";
        var filePid = PIDs.get(UUID_TEST);
        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        Path contentPath = createBinaryContent(content);
        fileObj.addOriginalFile(contentPath.toUri(), "file.txt", "text/plain", null, null);

        MvcResult generateResult = mvc.perform(post("/single_use_link/create/" + filePid.getUUID()))
                .andReturn();
        var map = MvcTestHelpers.getMapFromResponse(generateResult);
        var accessKey = map.get(KEY).toString();

        MvcResult result = mvc.perform(get("/single_use_link/" + accessKey))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals(content, response.getContentAsString());
        assertEquals("text/plain", response.getContentType());
        assertEquals("attachment; filename=\"file.txt\"", response.getHeader(CONTENT_DISPOSITION));
    }
}
