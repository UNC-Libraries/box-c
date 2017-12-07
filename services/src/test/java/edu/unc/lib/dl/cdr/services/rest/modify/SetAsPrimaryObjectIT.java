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

import static edu.unc.lib.dl.acl.util.Permission.editResourceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
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
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;
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
    @ContextConfiguration("/set-as-primary-object-it-servlet.xml")
})
@WebAppConfiguration
public class SetAsPrimaryObjectIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private Model queryModel;

    private MockMvc mvc;
    private WorkObject parent;
    private PID parentPid;
    private FileObject fileObj;
    private PID fileObjPid;

    @Before
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("user");
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));

        fileObjPid = makePid();
        fileObj = repositoryObjectFactory.createFileObject(fileObjPid, null);
        parentPid = makePid();
        parent = repositoryObjectFactory.createWorkObject(parentPid, null);
    }

    @After
    public void tearDown() {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testSetPrimaryObject() throws UnsupportedOperationException, Exception {
        addFileObjAsMember();

        assertPrimaryObjectNotSet(parent);

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertPrimaryObjectSet(parent, fileObj);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(fileObjPid.getUUID(), respMap.get("pid"));
        assertEquals("setAsPrimaryObject", respMap.get("action"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(fileObjPid), any(AccessGroupSet.class), eq(editResourceType));

        addFileObjAsMember();

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

        assertPrimaryObjectNotSet(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(fileObjPid.getUUID(), respMap.get("pid"));
        assertEquals("setAsPrimaryObject", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testAddFolderAsPrimaryObject() throws UnsupportedOperationException, Exception {
        PID folderObjPid = makePid();

        repositoryObjectFactory.createFolderObject(folderObjPid, null);

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + folderObjPid.getUUID()))
                .andExpect(status().isInternalServerError())
                .andReturn();

            assertPrimaryObjectNotSet(parent);

            // Verify response from api
            Map<String, Object> respMap = getMapFromResponse(result);
            assertEquals(folderObjPid.getUUID(), respMap.get("pid"));
            assertEquals("setAsPrimaryObject", respMap.get("action"));
            assertTrue(respMap.containsKey("error"));
    }

    private void assertPrimaryObjectSet(WorkObject parent, FileObject fileObj) {
        assertNotNull(parent.getPrimaryObject());
        assertEquals(parent.getPrimaryObject().getPid(), fileObj.getPid());
    }

    private void assertPrimaryObjectNotSet(WorkObject parent) {
        assertNull(parent.getPrimaryObject());
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private void addFileObjAsMember() {
        queryModel.getResource(parentPid.getRepositoryPath())
                .addProperty(PcdmModels.hasMember, fileObj.getResource());
        parent.addMember(fileObj);

    }

    private Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>(){});
    }

}
