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
package edu.unc.lib.boxc.web.services.rest.modify;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public abstract class AbstractAPIIT {
    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Autowired(required = false)
    protected String baseAddress;
    @Autowired
    protected WebApplicationContext context;
    @Autowired(required = false)
    protected AccessControlService aclService;
    @Autowired(required = false)
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired(required = false)
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired(required = false)
    protected PIDMinter pidMinter;
    @Autowired(required = false)
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired(required = false)
    protected RepositoryInitializer repoInitializer;

    protected ContentRootObject contentRoot;

    protected MockMvc mvc;

    @Before
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);

    }

    @After
    public void tearDown() {
        GroupsThreadStore.clearStore();
    }

    protected void setupContentRoot() {
        repoInitializer.initializeRepository();
        contentRoot = repositoryObjectLoader.getContentRootObject(getContentRootPid());
    }

    protected PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    protected Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(), type);
    }

    protected byte[] makeRequestBody(Object details) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(details);
    }
}
