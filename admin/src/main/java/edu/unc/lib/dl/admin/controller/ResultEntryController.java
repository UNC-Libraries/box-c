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
package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class ResultEntryController extends AbstractSearchController {
    private final List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
            SearchFieldKeys.CREATOR.name(), SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.DATE_ADDED.name(),
            SearchFieldKeys.DATE_UPDATED.name(), SearchFieldKeys.RESOURCE_TYPE.name(),
            SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.STATUS.name(), SearchFieldKeys.ANCESTOR_PATH.name(),
            SearchFieldKeys.VERSION.name(), SearchFieldKeys.ROLE_GROUP.name(), SearchFieldKeys.RELATIONS.name(),
            SearchFieldKeys.CONTENT_STATUS.name(), SearchFieldKeys.IS_PART.name(), SearchFieldKeys.ROLLUP_ID.name());

    @RequestMapping(value = "entry/{pid}", method = RequestMethod.GET)
    public @ResponseBody
    String getResultEntry(@PathVariable("pid") String pid, Model model, HttpServletResponse response) {
        response.setContentType("application/json");
        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

        SimpleIdRequest entryRequest = new SimpleIdRequest(pid, resultsFieldList, accessGroups);
        BriefObjectMetadataBean entryBean = queryLayer.getObjectById(entryRequest);
        if (entryBean == null) {
            throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
        }

        return SerializationUtil.metadataToJSON(entryBean, accessGroups);
    }
}