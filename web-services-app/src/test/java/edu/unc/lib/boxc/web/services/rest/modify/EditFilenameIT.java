package edu.unc.lib.boxc.web.services.rest.modify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;

import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/edit-filename-it-servlet.xml")
})
public class EditFilenameIT extends AbstractAPIIT {
    private String filename = "file.txt";
    private String mimetype = "text/plain";

    @Test
    public void testCreateLabelWhereNoneExists() throws Exception {
        PID pid = makePid();

        Path file = createBinaryContent("", "test", "txt");
        FileObject fileObj = repositoryObjectFactory.createFileObject(pid, null);
        fileObj.addOriginalFile(file.toUri(), filename, mimetype, null, null);

        String label = "work_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals("work_filename",
                fileObj.getOriginalFile().getResource().getProperty(Ebucore.filename)
                        .getLiteral().toString());
    }

    @Test
    public void testReplaceLabel() throws UnsupportedOperationException, Exception {
        PID pid = makePid();
        Path file = createBinaryContent("", "test", "txt");

        FileObject fileObj = repositoryObjectFactory.createFileObject(pid, null);
        fileObj.addOriginalFile(file.toUri(), filename, mimetype, null, null);

        String newLabel = "new_work_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", newLabel))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals(newLabel,
                fileObj.getOriginalFile().getResource().getProperty(Ebucore.filename)
                        .getLiteral().toString());
    }

    @Test
    public void testWrongObjectType() throws UnsupportedOperationException, Exception {
        PID pid = makePid();

        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);

        String label = "work_filename";
        mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", label))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.editDescription));

        String label = "folder_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", label))
                .andExpect(status().isForbidden())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

}
