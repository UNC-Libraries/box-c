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
package edu.unc.lib.dl.ui.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.service.FedoraContentService;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Controller which handles requests for specific content datastreams from Fedora and streams the results back as the
 * response.
 * 
 * @author bbpennel
 */
@Controller
public class FedoraContentController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(FedoraContentController.class);
	@Autowired
	private FedoraContentService fedoraContentService;
	@Autowired
	private SearchSettings searchSettings;

	@RequestMapping("/indexablecontent")
	public void handleIndexableRequest(@RequestParam("ds") String datastream,
			@RequestParam(value = "dl", defaultValue = "false") boolean download, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		handleRequest(datastream, download, model, request, response);
	}

	@RequestMapping("/content")
	public void handleRequest(@RequestParam("ds") String datastream,
			@RequestParam(value = "dl", defaultValue = "false") boolean download, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		
		if (datastream == null) {
			datastream = Datastream.DATA_FILE.toString();
		}
		

		// Use solr to check if the user is allowed to view this item.
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID.name()));

		// Get the content type of the object if its accessible
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID.name());
		resultFields.add(SearchFieldKeys.DATASTREAM.name());

		SimpleIdRequest idRequest = new SimpleIdRequest(id, resultFields, accessGroups);

		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		// If the record isn't accessible then invalid record exception.
		if (briefObject == null) {
			throw new InvalidRecordRequestException();
		}

		try {
			String fileExtension = null;

			edu.unc.lib.dl.search.solr.model.Datastream datastreamResult = briefObject.getDatastreamObject(datastream);
			if (datastreamResult != null) {
				fileExtension = datastreamResult.getExtension();
				response.setContentLength(datastreamResult.getFilesize().intValue());
			}

			fedoraContentService.streamData(id, datastream, response.getOutputStream(), response,
					fileExtension, download);
		} catch (AuthorizationException e) {
			throw new InvalidRecordRequestException(e);
		} catch (Exception e) {
			LOG.error("Failed to retrieve content for " + id + " datastream: " + datastream, e);
			throw new ResourceNotFoundException();
		}
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN)
	@ExceptionHandler(InvalidRecordRequestException.class)
	public String handleInvalidRecordRequest(HttpServletRequest request) {
		request.setAttribute("pageSubtitle", "Invalid content");
		return "error/invalidRecord";
	}
}
