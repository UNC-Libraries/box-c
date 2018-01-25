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

import static edu.unc.lib.dl.acl.util.Permission.bulkUpdateDescription;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.persist.services.EmailHandler;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/export-xml-it-servlet.xml")
})
public class ExportXMLIT extends AbstractAPIIT {

    @Mock
    private EmailHandler emailHandler;
    @Mock
    private MimeMessage mimeMessage = mock(MimeMessage.class);

    @Before
    public void init_() throws Exception {

        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(bulkUpdateDescription))).thenReturn(true);
    }

    @Test
    public void testExportMODS() throws Exception {
        doNothing().when(aclService).assertHasAccess(anyString(), any(PID.class), any(AccessGroupSet.class),
                eq(bulkUpdateDescription));

        String json = makeJSON(false);
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));
    }

    @Test
    public void testNoUsernameProvided() throws Exception {
        String json = makeJSON(false);
        // reset username to null to simulate situation where no username exists
        GroupsThreadStore.clearStore();
        GroupsThreadStore.storeUsername(null);
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));
        assertEquals("User must have a username to export xml", respMap.get("error"));
    }

    private String makeJSON(boolean exportChildren) throws JsonGenerationException, JsonMappingException, IOException {
        String pid1 = makePid().getRepositoryPath();
        String pid2 = makePid().getRepositoryPath();
        List<String> pids = new ArrayList<>();
        pids.add(pid1);
        pids.add(pid2);

        XMLExportRequest exportRequest = new XMLExportRequest(pids, exportChildren, "user@example.com");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(exportRequest);
    }

}
