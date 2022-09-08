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

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.services.processing.MemberOrderCsvExporter;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration("/member-order-test-servlet.xml")
public class MemberOrderControllerTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MemberOrderCsvExporter csvExporter;
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private MockMvc mvc;

    @Before
    public void init() throws Exception {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        tmpFolder.create();
    }

    @Test
    public void memberOrderCsvExportSuccessTest() throws Exception {
        var csvPath = tmpFolder.newFile().toPath();
        var expectedContent = "some,csv,data,goes,here";
        FileUtils.writeStringToFile(csvPath.toFile(), expectedContent, StandardCharsets.UTF_8);

        var ids = PARENT1_UUID + "," + PARENT2_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID), PIDs.get(PARENT2_UUID))), any(AgentPrincipals.class)))
                .thenReturn(csvPath);

        MvcResult result = mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var response = result.getResponse();
        assertEquals(expectedContent, response.getContentAsString());
    }

    @Test
    public void memberOrderCsvExportNoIdParamTest() throws Exception {
        mvc.perform(get("/edit/memberOrder/export/csv"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportEmptyIdParamTest() throws Exception {
        mvc.perform(get("/edit/memberOrder/export/csv?ids="))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportAccessFailureTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new AccessRestrictionException());

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportInvalidResourceTypeTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new InvalidOperationForObjectType());

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportServerErrorTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new RepositoryException("Boom"));

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportInvalidPidsTest() throws Exception {
        var ids = "badpids,beingsubmitted";

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
