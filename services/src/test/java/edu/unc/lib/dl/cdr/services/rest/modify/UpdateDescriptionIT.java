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

import static edu.unc.lib.dl.acl.util.Permission.editDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.tika.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/update-description-it-servlet.xml")
})
public class UpdateDescriptionIT extends AbstractAPIIT {

    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private RepositoryObjectFactory repoFactory;

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
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
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
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class), eq(editDescription));

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
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateDescription", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    private PID makeWorkObject() {
        return repoFactory.createWorkObject(makePid(), null).getPid();
    }

    private void assertDescriptionUpdated(PID objPid) {
        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(objPid);
        assertNotNull(obj.getDescription());
    }

    private void assertDescriptionNotUpdated(PID objPid) {
        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(objPid);
        assertNull(obj.getDescription());
    }

}
