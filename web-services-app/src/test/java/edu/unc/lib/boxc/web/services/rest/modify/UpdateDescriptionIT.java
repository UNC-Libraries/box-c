package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/update-description-it-servlet.xml")
})
public class UpdateDescriptionIT extends AbstractAPIIT {
    private AutoCloseable closeable;
    @Mock
    private ContentPathFactory pathFactory;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        updateDescriptionService.setValidate(true);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateDescription() throws Exception {
        File file = new File("src/test/resources/mods/valid-mods.xml");
        InputStream stream = new FileInputStream(file);
        PID objPid = makeWorkObject();

        assertDescriptionNotUpdated(objPid);

        MvcResult result = mvc.perform(post("/edit/description/" + objPid.getUUID())
                .content(IOUtils.toByteArray(stream)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertDescriptionUpdated(objPid);

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
        assertEquals("updated", respMap.get("status"));
    }

    @Test
    public void testUpdateDescriptionNoChange() throws Exception {
        File file = new File("src/test/resources/mods/valid-mods.xml");
        var content = IOUtils.toByteArray(new FileInputStream(file));
        PID objPid = makeWorkObject();

        mvc.perform(post("/edit/description/" + objPid.getUUID())
                .content(content))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ContentObject obj1 = (ContentObject) repositoryObjectLoader.getRepositoryObject(objPid);
        var modifiedAfterFirst = obj1.getDescription().getLastModified();

        // Repeat the update, so there should be no changes the second time
        MvcResult result2 = mvc.perform(post("/edit/description/" + objPid.getUUID())
                .content(content))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Invalid cache to make sure we get a fresh copy
        repositoryObjectLoader.invalidate(objPid);
        repositoryObjectLoader.invalidate(obj1.getDescription().getPid());

        ContentObject obj2 = (ContentObject) repositoryObjectLoader.getRepositoryObject(objPid);
        var modifiedAfterSecond = obj2.getDescription().getLastModified();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result2);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
        assertEquals("unchanged", respMap.get("status"));
        assertNotEquals(modifiedAfterFirst, modifiedAfterSecond, "Description should not have been updated");
    }

    @Test
    public void testInvalidMods() throws Exception {
        File file = new File("src/test/resources/mods/invalid-mods.xml");
        InputStream stream = new FileInputStream(file);
        PID objPid = makeWorkObject();

        assertDescriptionNotUpdated(objPid);

        MvcResult result = mvc.perform(post("/edit/description/" + objPid.getUUID())
                .content(IOUtils.toByteArray(stream)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertDescriptionNotUpdated(objPid);

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), any(PID.class), any(AccessGroupSetImpl.class), eq(editDescription));

        File file = new File("src/test/resources/mods/valid-mods.xml");
        InputStream stream = new FileInputStream(file);
        PID objPid = makeWorkObject();

        assertDescriptionNotUpdated(objPid);

        MvcResult result = mvc.perform(post("/edit/description/" + objPid.getUUID())
                .content(IOUtils.toByteArray(stream)))
                .andExpect(status().isForbidden())
                .andReturn();

        assertDescriptionNotUpdated(objPid);

        // Verify response from api
        assertEquals("Insufficient permissions", result.getResponse().getContentAsString());
    }

    private PID makeWorkObject() {
        return repositoryObjectFactory.createWorkObject(makePid(), null).getPid();
    }

    private void assertDescriptionUpdated(PID objPid) {
        ContentObject obj = (ContentObject) repositoryObjectLoader.getRepositoryObject(objPid);
        assertNotNull(obj.getDescription());
    }

    private void assertDescriptionNotUpdated(PID objPid) {
        ContentObject obj = (ContentObject) repositoryObjectLoader.getRepositoryObject(objPid);
        assertNull(obj.getDescription());
    }

}
