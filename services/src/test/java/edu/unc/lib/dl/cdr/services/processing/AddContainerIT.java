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
package edu.unc.lib.dl.cdr.services.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jgroups.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
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
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml", "/add-container-it-servlet.xml"})
@WebAppConfiguration
public class AddContainerIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private AccessControlService aclService;

    private MockMvc mvc;

    @Before
    public void init() {
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

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws UnsupportedOperationException, Exception {
        PID parentPid = makePid();
        List<String> idList = Arrays.asList(parentPid.getUUID());

        AdminUnit parent = repositoryObjectFactory.createAdminUnit(parentPid, null);

        assertChildContainerNotAdded(parent);

        MvcResult result = mvc.perform(post("/edit/create/collection/" + parentPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertChildContainerAdded(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(idList.containsAll((List<String>) respMap.get("pids")));
        assertEquals("create", respMap.get("action"));
    }

    private void assertChildContainerAdded(ContentContainerObject parent) {
        if (parent.getMembers().size() != 0) {
            assertTrue(parent.getMembers().get(0) instanceof ContentContainerObject);
        } else {
            fail("No child container was added to parent");
        }
    }

    private void assertChildContainerNotAdded(ContentContainerObject parent) {
        assertTrue(parent.getMembers().size() == 0);
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>(){});
    }

}
