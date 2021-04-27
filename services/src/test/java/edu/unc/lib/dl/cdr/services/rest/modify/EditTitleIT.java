/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.rest.modify;

import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.documentToInputStream;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService.UpdateDescriptionRequest;

/**
 *
 * @author lfarrell
 *
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
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
        Map<String, Object> respMap = getMapFromResponse(result);
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
        Map<String, Object> respMap = getMapFromResponse(result);
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

        InputStream modsStream = documentToInputStream(document);
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(
                mock(AgentPrincipals.class), pid, modsStream));

        String newTitle = "new_work_title";
        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", newTitle))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        work.shouldRefresh();
        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
        hasTitleValue(getUpdatedDescriptionDocument(work.getDescription()), newTitle);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(Permission.editDescription));

        String title = "folder_title";
        MvcResult result = mvc.perform(put("/edit/title/" + pid.getUUID())
                .param("title", title))
                .andExpect(status().isForbidden())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editTitle", respMap.get("action"));
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
