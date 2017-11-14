/*
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.sparql.JenaSparqlQueryServiceImpl;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml", "/set-as-primary-object-it-servlet.xml"})
@WebAppConfiguration
public class SetAsPrimaryObjectIT {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private AccessControlService aclService;

    private SparqlQueryService queryService;
    private Model model;
    private MockMvc mvc;

    @Before
    public void init() {

        model = ModelFactory.createDefaultModel();

        queryService = new JenaSparqlQueryServiceImpl(model);

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
    public void setPrimaryObjectTest() throws UnsupportedOperationException, Exception {
        PID fileObjPid = makePid();
        PID parentPid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(fileObjPid, null);
        InputStream stream = new FileInputStream("src/test/resources/txt.txt");
        WorkObject parent = repositoryObjectFactory.createWorkObject(parentPid, null);

        model.getResource(parentPid.getRepositoryPath())
            .addProperty(PcdmModels.hasMember, fileObjPid.getRepositoryPath());
        parent.addMember(fileObj);

        assertPrimaryObjectNotSet(parent);

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertPrimaryObjectSet(parent, fileObj);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
    }

    private void assertPrimaryObjectSet(WorkObject parent, FileObject fileObj) {
        assertNotNull(parent.getPrimaryObject());
        assertEquals(parent.getPrimaryObject(), fileObj);
    }

    private void assertPrimaryObjectNotSet(WorkObject parent) {
        assertNull(parent.getPrimaryObject());
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
