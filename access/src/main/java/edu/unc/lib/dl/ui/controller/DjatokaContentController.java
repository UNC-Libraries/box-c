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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.DjatokaContentService;
import edu.unc.lib.dl.ui.util.AccessControlSettings;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Controller for requests related to accessing jp2's through djatoka.  Applies 
 * cdr access control as a prerequisite to connecting with djatoka.
 * @author bbpennel
 */
@Controller
public class DjatokaContentController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(DjatokaContentController.class);
	
	/*@Autowired
	private AccessControlSettings accessSettings;*/
	@Autowired
	private DjatokaContentService djatokaContentService;
	
	/**
	 * Determines if the user is allowed to access a specific datastream on 
	 * the selected object.  If so, then the result is cached for future use.
	 * @param id
	 * @param datastream
	 * @param request
	 * @return
	 */
	private boolean hasAccess(String id, String datastream){
		//Defaults to jp2 surrogate if no datastream specified
		if (datastream == null){
			datastream = ContentModelHelper.Datastream.IMAGE_JP2000.toString();
		}
		
		Datastream datastreamClass = Datastream.getDatastream(datastream);
		
		// TODO make this work with the new model
		/*AccessType accessType = accessSettings.getAccessType(datastream); 
		String accessField = null;
		//Administrative, Original, Metadata, Derivative
		switch (datastreamClass.getCategory()){
			case Original:
				accessField = SearchFieldKeys.FILE_ACCESS;
				break;
			case Derivative:
				accessField = SearchFieldKeys.SURROGATE_ACCESS;
				break;
			default:
				return false;
		}
		
		//Check to see if the user has gotten this type of datastream for this object before
		if (user.getDatastreamAccessCache().contains(id, datastreamClass.getCategory())){
			return true;
		}*/
		
		//Check to see if the user can access this datastream/object
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID);
		resultFields.add(SearchFieldKeys.CONTENT_TYPE);
		SimpleIdRequest idRequest = new SimpleIdRequest(id, resultFields, GroupsThreadStore.getGroups()/*, accessField*/);
		
		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		//If the record isn't accessible then invalid record exception.
		if (briefObject == null){
			return false;
		}
		
		// TODO Access allowed, cache this result.
		//user.getDatastreamAccessCache().put(id, datastreamClass.getCategory());
		
		return true;
	}
	
	/**
	 * Handles requests for jp2 metadata
	 * @param model
	 * @param request
	 * @param response
	 */
	@RequestMapping("/jp2Metadata")
	public void getMetadata(Model model, HttpServletRequest request, HttpServletResponse response){
		//Check if the user is allowed to view this object
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID));
		String datastream = request.getParameter("ds");
		if (this.hasAccess(id, datastream)){
			try {
				djatokaContentService.getMetadata(id, datastream, response.getOutputStream(), response);
			} catch (IOException e){
				LOG.error("Error retrieving JP2 metadata content for " + id, e);
			}
		}	
	}
	
	/**
	 * Handles requests for individual region tiles.
	 * @param model
	 * @param request
	 * @param response
	 */
	@RequestMapping("/jp2Region")
	public void getRegion(Model model, HttpServletRequest request, HttpServletResponse response){
		//Check if the user is allowed to view this object
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID));
		String datastream = request.getParameter("ds");
		if (this.hasAccess(id, datastream)){
			//Retrieve region specific parameters
			String region = request.getParameter("svc.region");
			String scale = request.getParameter("svc.level");
			String rotate = request.getParameter("svc.rotate");
			try {
				djatokaContentService.streamJP2(id, region, scale, rotate, 
						datastream, response.getOutputStream(), response);
			} catch (IOException e){
				LOG.error("Error retrieving JP2 metadata content for " + id, e);
			}
		}	
	}
}
