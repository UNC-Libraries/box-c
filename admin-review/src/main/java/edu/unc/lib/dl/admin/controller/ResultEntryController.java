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
import edu.unc.lib.dl.search.solr.tags.TagProvider;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.SerializationUtil;

@Controller
public class ResultEntryController extends AbstractSearchController {
	private List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
			SearchFieldKeys.CREATOR.name(), SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.DATE_ADDED.name(),
			SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.STATUS.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.VERSION.name(), SearchFieldKeys.ROLE_GROUP.name(),
			SearchFieldKeys.RELATIONS.name());
	
	@RequestMapping(value = "entry/{pid}", method = RequestMethod.GET)
	public @ResponseBody String getResultEntry(@PathVariable("pid") String pid, Model model,
			HttpServletResponse response) {
		response.setContentType("application/json");
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

		SimpleIdRequest entryRequest = new SimpleIdRequest(pid, resultsFieldList, accessGroups);
		BriefObjectMetadataBean entryBean = queryLayer.getObjectById(entryRequest);
		if (entryBean == null) {
			throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
		}

		for (TagProvider provider : this.tagProviders) {
			provider.addTags(entryBean, accessGroups);
		}
		return SerializationUtil.metadataToJSON(entryBean, accessGroups);
	}
}