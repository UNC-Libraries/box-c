package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.web.services.processing.SingleUseKeyService;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/single-use-key-it-servlet.xml")
})
public class SingleUseKeyControllerIT extends AbstractAPIIT {
    private AutoCloseable closeable;
    private PID pid;
    @TempDir
    public Path tmpFolder;

    @Autowired
    private SingleUseKeyService singleUseKeyService;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        pid = makePid();
        Path csvPath = tmpFolder.resolve("singleUseKey");
        singleUseKeyService.setCsvPath(csvPath);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGenerateNoAccess() throws Exception {
        var fileObject = repositoryObjectFactory.createFileObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(viewHidden));

        mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testGenerateNotAFileObject() throws Exception {
        var folder = repositoryObjectFactory.createFolderObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(folder);

        mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGenerateFailure() throws Exception {
        var id = pid.getUUID();
        var fileObject = repositoryObjectFactory.createFileObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        singleUseKeyService.setCsvPath(null);

        mvc.perform(post("/single_use_link/create/" + id))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    public void testGenerateSuccess() throws Exception {
        var fileObject = repositoryObjectFactory.createFileObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);

        MvcResult result = mvc.perform(post("/single_use_link/create/" + pid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertNotNull(respMap.get("url"));
        assertEquals(pid.getUUID(), respMap.get("target_id"));
        assertNotNull(respMap.get("expires"));
    }
}
