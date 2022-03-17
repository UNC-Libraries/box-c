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
package edu.unc.lib.boxc.web.admin.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.model.api.ResourceType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class ResultEntryController extends AbstractSearchController {
    private final List<String> resultsFieldList = Arrays.asList(SearchFieldKey.ID.name(), SearchFieldKey.TITLE.name(),
            SearchFieldKey.CREATOR.name(), SearchFieldKey.DATASTREAM.name(), SearchFieldKey.DATE_ADDED.name(),
            SearchFieldKey.DATE_UPDATED.name(), SearchFieldKey.RESOURCE_TYPE.name(),
            SearchFieldKey.STATUS.name(), SearchFieldKey.ANCESTOR_PATH.name(),
            SearchFieldKey.VERSION.name(), SearchFieldKey.ROLE_GROUP.name(), SearchFieldKey.RELATIONS.name(),
            SearchFieldKey.CONTENT_STATUS.name(), SearchFieldKey.ROLLUP_ID.name());

    @RequestMapping(value = "entry/{pid}", method = RequestMethod.GET)
    public @ResponseBody
    String getResultEntry(@PathVariable("pid") String pid, Model model, HttpServletResponse response) {
        response.setContentType("application/json");
        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();

        SimpleIdRequest entryRequest = new SimpleIdRequest(PIDs.get(pid), resultsFieldList, accessGroups);
        ContentObjectRecord entryBean = queryLayer.getObjectById(entryRequest);
        if (entryBean == null) {
            throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
        }

        if (!ResourceType.File.nameEquals(entryBean.getResourceType())) {
            childrenCountService.addChildrenCounts(Collections.singletonList(entryBean), accessGroups);
        }
        return SerializationUtil.metadataToJSON(entryBean, accessGroups);
    }
}