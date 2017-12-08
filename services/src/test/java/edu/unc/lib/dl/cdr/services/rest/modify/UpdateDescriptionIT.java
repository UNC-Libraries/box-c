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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.tika.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/update-description-it-servlet.xml")
})
@WebAppConfiguration
public class UpdateDescriptionIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private RepositoryObjectFactory repoFactory;

    private MockMvc mvc;

    @Before
    public void init() throws FileNotFoundException {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("user");
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));
    }

    @After
    public void tearDown() {
        GroupsThreadStore.clearStore();
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
        return repoFactory.createWorkObject(PIDs.get(UUID.randomUUID().toString()), null).getPid();
    }

    private Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>(){});
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
