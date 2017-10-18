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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.IdListRequest;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value = { "/status/item*", "/status/item" })
public class ItemInfoRestController implements ServletContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(ItemInfoRestController.class);
    protected ServletContext servletContext = null;

    @Resource
    private SolrSearchService solrSearchService;

    @Resource(name = "contextUrl")
    protected String contextUrl = null;

    @RequestMapping(value = "{id}/solrRecord", method = RequestMethod.GET)
    public @ResponseBody
    BriefObjectMetadataBean getItemSolrRecord(HttpServletResponse response, @PathVariable("id") String id) {
        response.setContentType("application/xml");
        AccessGroupSet groupSet = new AccessGroupSet(AccessGroupConstants.ADMIN_GROUP);
        SimpleIdRequest idRequest = new SimpleIdRequest(id, groupSet);
        BriefObjectMetadataBean metadata = solrSearchService.getObjectById(idRequest);
        return metadata;
    }

    @RequestMapping(value = "{id}/solrRecord/version", method = RequestMethod.GET)
    public @ResponseBody
    Long getItemLastIndexed(@PathVariable("id") String id) {
        // For when group forwarding is enabled here
        /* AccessGroupSet groupSet = new AccessGroupSet(GroupsThreadStore.getGroups().split(";")); */
        AccessGroupSet groupSet = new AccessGroupSet(AccessGroupConstants.ADMIN_GROUP);
        SimpleIdRequest idRequest = new SimpleIdRequest(id, Arrays.asList("_version_"), groupSet);
        BriefObjectMetadataBean md = solrSearchService.getObjectById(idRequest);
        if (md == null || md.get_version_() == null) {
            return null;
        }
        return md.get_version_();
    }

    @RequestMapping(value = "solrRecord/version", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> getItemsLastIndexed(@RequestParam("ids") String idsString) {
        if (idsString == null) {
            return null;
        }

        List<String> ids = Arrays.asList(idsString.split("\n"));

        // For when group forwarding is enabled here
        /*AccessGroupSet groupSet = new AccessGroupSet(GroupsThreadStore.getGroups().split(";"));*/
        AccessGroupSet groupSet = new AccessGroupSet(AccessGroupConstants.ADMIN_GROUP);
        List<String> resultFields = Arrays.asList("_version_");

        IdListRequest listRequest = new IdListRequest(ids, resultFields, groupSet);
        List<BriefObjectMetadata> listResults = solrSearchService.getObjectsById(listRequest);
        Map<String, String> results = new HashMap<>(listResults.size());

        for (BriefObjectMetadata result: listResults) {
            results.put(result.getId(), Long.toString(result.get_version_()));
        }

        return results;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
