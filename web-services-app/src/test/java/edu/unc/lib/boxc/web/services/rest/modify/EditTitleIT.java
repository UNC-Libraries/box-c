package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.Map;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author lfarrell
 *
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/edit-title-it-servlet.xml")
})
public class EditTitleIT extends AbstractAPIIT {
    @Autowired
    private UpdateDescriptionService updateDescriptionService;

    @Test
    public void testCreateTitleWhereNoneExists() throws Exception {
        PID pid = makePid();
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);
        String title = "work_title";

        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", title))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        work.shouldRefresh();
        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        hasTitleValue(getUpdatedDescriptionDocument(work.getDescription()), title);
    }

    @Test
    public void testSpecialCharactersTitle() throws Exception {
        PID pid = makePid();
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);
        String title = "work_title!*'();:@&=+$,/?%#[]special@charcters";

        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", title))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        work.shouldRefresh();
        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        hasTitleValue(getUpdatedDescriptionDocument(work.getDescription()), title);
    }

    @Test
    public void testReplaceTitle() throws Exception {
        PID pid = makePid();
        String oldTitle = "old_work_title";
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);

        Document document = new Document();
        document.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText(oldTitle))));

        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(
                mock(AgentPrincipalsImpl.class), pid, document));

        String newTitle = "new_work_title";
        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", newTitle))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        work.shouldRefresh();
        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        hasTitleValue(getUpdatedDescriptionDocument(work.getDescription()), newTitle);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.editDescription));

        String title = "folder_title";
        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", title))
                .andExpect(status().isForbidden())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testStateUnmodifiedFailureReturns200Status() throws Exception {
        PID pid = makePid();
        repositoryObjectFactory.createFolderObject(pid, null);

        String title = "folder_title";
        // submit a title change
        mvc.perform(put("/edit/title/" + pid.getUUID())
                        .param("title", title))
                .andExpect(status().isOk())
                .andReturn();

        // submit same title change again, state should be unmodified
        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                        .param("title", title))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        assertEquals("unchanged", respMap.get("status"));
        assertTrue(respMap.containsKey("error"));
    }

    private Document getUpdatedDescriptionDocument(BinaryObject binary) throws IOException, JDOMException {
        SAXBuilder sb = createSAXBuilder();
        return sb.build(binary.getBinaryStream());
    }

    private boolean hasTitleValue(Document document, String expectedTitle) {
        return document.getRootElement()
                .getChildren("titleInfo", MODS_V3_NS)
                .stream()
                .anyMatch(e -> (e.getChild("title", MODS_V3_NS).getValue().contentEquals(expectedTitle)));
    }
}
