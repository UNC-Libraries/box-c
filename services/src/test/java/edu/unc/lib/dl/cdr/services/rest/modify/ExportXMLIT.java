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

import static edu.unc.lib.boxc.auth.api.Permission.bulkUpdateDescription;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

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
    private static Path MODS_PATH_1 = Paths.get("src/test/resources/mods/valid-mods.xml");
    private static Path MODS_PATH_2 = Paths.get("src/test/resources/mods/valid-mods2.xml");

    @Autowired
    private EmailHandler emailHandler;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private AgentPrincipals agent;
    @Autowired
    private SolrQueryLayerService queryLayer;
    @Autowired
    private SearchStateFactory searchStateFactory;
    @Mock
    private SearchState searchState;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;

    @Captor
    private ArgumentCaptor<String> toCaptor;
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> bodyCaptor;
    @Captor
    private ArgumentCaptor<String> filenameCaptor;
    @Captor
    private ArgumentCaptor<File> attachmentCaptor;

    @Before
    public void init_() throws Exception {
        initMocks(this);
        when(aclService.hasAccess(any(PID.class), any(AccessGroupSetImpl.class), eq(bulkUpdateDescription)))
                .thenReturn(true);
        reset(emailHandler);
    }

    @Test
    public void testExportMODS() throws Exception {
        doNothing().when(aclService).assertHasAccess(anyString(), any(PID.class), any(AccessGroupSetImpl.class),
                eq(bulkUpdateDescription));

        String json = makeExportJson(createObjects(),false);
        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        verify(emailHandler).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture(),
                filenameCaptor.capture(), attachmentCaptor.capture());
        assertEquals("The XML metadata for 2 object(s) requested for export by test_user is attached.\n",
                bodyCaptor.getValue());
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));
    }

    @Test
    public void testExportChildren() throws Exception {
        List<String> exports = createObjects();
        String json = makeExportJson(createObjects(),true);

        when(searchStateFactory.createSearchState()).thenReturn(searchState);
        SearchResultResponse results = mock(SearchResultResponse.class);
        when(queryLayer.performSearch(any(SearchRequest.class))).thenReturn(results);

        BriefObjectMetadataBean md = new BriefObjectMetadataBean();
        md.setId(exports.get(0));

        BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
        md2.setId(exports.get(1));

        when(results.getResultList()).thenReturn(Arrays.asList(md, md2));

        MvcResult result = mvc.perform(post("/edit/exportXML")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        verify(emailHandler).sendEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture(),
                filenameCaptor.capture(), attachmentCaptor.capture());
        assertEquals("The XML metadata for 6 object(s) requested for export by test_user is attached.\n",
                bodyCaptor.getValue());
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("export xml", respMap.get("action"));
    }

    @Test
    public void testNoUsernameProvided() throws Exception {
        String json = makeExportJson(createObjects(),true);
        // reset username to null to simulate situation where no username exists
        GroupsThreadStore.clearStore();
        GroupsThreadStore.storeUsername(null);
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("adminGroup"));
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

    private List<String> createObjects() throws Exception {
        ContentObject folder = repositoryObjectFactory.createFolderObject(null);
        ContentObject work = repositoryObjectFactory.createWorkObject(null);
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(
                agent, folder.getPid(), Files.newInputStream(MODS_PATH_1)));
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(
                agent, work.getPid(), Files.newInputStream(MODS_PATH_2)));

        String pid1 = folder.getPid().getRepositoryPath();
        String pid2 = work.getPid().getRepositoryPath();
        List<String> pids = new ArrayList<>();
        pids.add(pid1);
        pids.add(pid2);

        return pids;
    }

    private String makeExportJson(List<String> pids, boolean exportChildren) throws JsonProcessingException {
        XMLExportRequest exports = new XMLExportRequest(pids, exportChildren, "user@example.com");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(exports);
    }
}
