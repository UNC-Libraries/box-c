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
package edu.unc.lib.dl.cdr.services.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.IdListRequest;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

@ContextConfiguration("/item-info-it-servlet.xml")
public class ItemInfoRestControllerIT extends AbstractAPIIT {

    @Autowired
    private SolrQueryLayerService solrSearchService;

    @Before
    public void setup() {
        initMocks(this);
        reset(solrSearchService);
    }

    @Test
    public void testGetVersion() throws Exception {
        PID objPid = makePid();
        ContentObjectSolrRecord md = mock(ContentObjectSolrRecord.class);

        String versionValue = "5693296345";

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(md);
        when(md.get_version_()).thenReturn(Long.parseLong(versionValue));

        MvcResult result = mvc.perform(get("/status/item/" + objPid.getUUID() + "/solrRecord/version"))
                .andExpect(status().isOk())
                .andReturn();

        String resultContent = result.getResponse().getContentAsString();
        assertEquals(versionValue, resultContent);
    }

    @Test
    public void testGetVersionDoesNotExist() throws Exception {
        PID objPid = makePid();

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(null);

        mvc.perform(get("/status/item/" + objPid.getUUID() + "/solrRecord/version"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testGetVersions() throws Exception {
        PID objPid1 = makePid();
        PID objPid2 = makePid();
        String versionValue1 = "5693296345";
        String versionValue2 = "3463562949";
        ContentObjectRecord md1 = mock(ContentObjectSolrRecord.class);
        when(md1.getId()).thenReturn(objPid1.getId());
        when(md1.get_version_()).thenReturn(Long.parseLong(versionValue1));
        ContentObjectRecord md2 = mock(ContentObjectSolrRecord.class);
        when(md2.getId()).thenReturn(objPid2.getId());
        when(md2.get_version_()).thenReturn(Long.parseLong(versionValue2));

        List<ContentObjectRecord> results = Arrays.asList(md1, md2);
        when(solrSearchService.getObjectsById(any(IdListRequest.class))).thenReturn(results);

        String idsValue = objPid1.getId() + "\n" + objPid2.getId();

        MvcResult result = mvc.perform(post("/status/item/solrRecord/version")
                .param("ids", idsValue))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(versionValue1, respMap.get(objPid1.getId()));
        assertEquals(versionValue2, respMap.get(objPid2.getId()));
    }

    @Test
    public void testGetVersionsNoIds() throws Exception {
        String idsValue = "";

        mvc.perform(post("/status/item/solrRecord/version")
                .param("ids", idsValue))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
