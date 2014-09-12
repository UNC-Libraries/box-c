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

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.invalidAffiliationTerm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.tags.TagProvider;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 * @author bbpennel
 * @date Sep 5, 2014
 */
@Controller
public class VocabularyController extends AbstractSearchController {

	@RequestMapping(value = { "invalidVocab", "invalidVocab/{pid}" }, method = RequestMethod.GET)
	public String invalidVocab() {
		return "report/invalidVocabulary";
	}

	@RequestMapping(value = "getInvalidVocab/{pid}", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getInvalidVocab(@PathVariable("pid") String pid, HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("application/json");

		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(collectionsPid.getPid());

		AccessGroupSet groups = GroupsThreadStore.getGroups();

		Map<String, Object> results = new LinkedHashMap<String, Object>();

		SearchResultResponse resultResponse = queryLayer.getRelationSet(searchRequest,
				invalidAffiliationTerm.getPredicate());

		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		for (BriefObjectMetadata record : resultResponse.getResultList()) {
			for (TagProvider provider : this.tagProviders) {
				provider.addTags(record, accessGroups);
			}
		}

		List<Map<String, Object>> vocabTypeResults = SerializationUtil.resultsToList(resultResponse, groups);
		results.put("departments", vocabTypeResults);

		return results;
	}

}
