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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.tags.TagProvider;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

@Controller
public class ReviewController extends AbstractSolrSearchController {
	private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
	private List<String> containerFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name());
	private List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
			SearchFieldKeys.CREATOR.name(), SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.DATE_ADDED.name(),
			SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.STATUS.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.VERSION.name(), SearchFieldKeys.ROLE_GROUP.name(),
			SearchFieldKeys.RELATIONS.name());
	@Autowired
	private PID collectionsPid;
	
	private @Resource(name = "tagProviders")
	List<TagProvider> tagProviders;

	@RequestMapping(value = "review", method = RequestMethod.GET)
	public String getReviewList(Model model, HttpServletRequest request) {
		return this.getReviewList(collectionsPid.getPid(), model, request);
	}

	@RequestMapping(value = "review/{prefix}/{id}", method = RequestMethod.GET)
	public String getReviewList(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {
		return this.getReviewList(idPrefix + ":" + id, model, request);
	}

	public String getReviewList(String pid, Model model, HttpServletRequest request) {
		log.debug("Reviewing " + pid);
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

		CutoffFacet path;
		if (!collectionsPid.getPid().equals(pid)) {
			// Retrieve the record for the container being reviewed
			SimpleIdRequest containerRequest = new SimpleIdRequest(pid, containerFieldList, accessGroups);
			BriefObjectMetadataBean containerBean = queryLayer.getObjectById(containerRequest);
			if (containerBean == null) {
				log.debug("Could not find path for " + pid + " while trying to generate review list");
				throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
			}
			path = containerBean.getPath();
			model.addAttribute("containerBean", containerBean);
		} else {
			path = new CutoffFacet("ANCESTOR_PATH", "1,*");
		}

		// Retrieve the list of unpublished (not belonging to an unpublished parent) items within this container.
		SearchState reviewListState = this.searchStateFactory.createSearchState();
		GenericFacet facet = new GenericFacet("STATUS", "Unpublished");
		reviewListState.getFacets().put("STATUS", facet);
		reviewListState.getFacets().put("ANCESTOR_PATH", path);

		reviewListState.setRowsPerPage(500);
		reviewListState.setResultFields(resultsFieldList);

		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(reviewListState);

		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
		log.debug("Retrieved " + resultResponse.getResultCount() + " results for the review list");

		// Add tags
		for (BriefObjectMetadata record : resultResponse.getResultList()) {
			for (TagProvider provider : this.tagProviders) {
				provider.addTags(record, accessGroups);
			}
		}

		model.addAttribute("resultResponse", resultResponse);

		request.getSession().setAttribute("resultOperation", "review");

		return "search/resultList";
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}
	
	public void setTagProviders(List<TagProvider> tagProviders) {
		this.tagProviders = tagProviders;
	}
}
