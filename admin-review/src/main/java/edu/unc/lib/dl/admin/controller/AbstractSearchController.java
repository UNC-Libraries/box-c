package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.tags.TagProvider;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

public class AbstractSearchController extends AbstractSolrSearchController {
	protected @Resource(name = "tagProviders")
	List<TagProvider> tagProviders;

	@Autowired
	protected PID collectionsPid;
	
	private static List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
			SearchFieldKeys.CREATOR.name(), SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.DATE_ADDED.name(),
			SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.STATUS.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.VERSION.name(), SearchFieldKeys.ROLE_GROUP.name(),
			SearchFieldKeys.RELATIONS.name());
	
	protected SearchResultResponse getSearchResults(SearchRequest searchRequest) {
		SearchState searchState = searchRequest.getSearchState();
		searchState.setResultFields(resultsFieldList);
		
		SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		
		// Add tags
		for (BriefObjectMetadata record : resultResponse.getResultList()) {
			for (TagProvider provider : this.tagProviders) {
				provider.addTags(record, accessGroups);
			}
		}
		
		return resultResponse;
	}
}
