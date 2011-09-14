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

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.model.response.RssFeedBean;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.service.RssParserService;
import edu.unc.lib.dl.ui.util.ExternalContentSettings;

/**
 * Controller which populates the dynamic components making up the front page of
 * the public UI
 * @author bbpennel
 */
@Controller
@RequestMapping("/")
public class FrontPageController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(FrontPageController.class);
	
	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request){
		LOG.debug("In front page controller");
		
		//Retrieve the list of newly added items
		AccessGroupSet accessGroups = getUserAccessGroups(request);
		SearchResultResponse resultResponse = queryLayer.getNewlyAdded(accessGroups);
		model.addAttribute("newlyAddedList", resultResponse.getResultList());
		
		//Retrieve news list
		try {
			RssFeedBean newsRssFeed = RssParserService.getRssFeed(ExternalContentSettings.getUrl("newsRss"), Integer.parseInt(ExternalContentSettings.get("external.newsRss.maxLinks")));
			model.addAttribute("newsRssFeed", newsRssFeed);
			RssFeedBean featuredContentFeed = RssParserService.getRssFeed(ExternalContentSettings.getUrl("featuredContent"));
			model.addAttribute("featuredContentFeed", featuredContentFeed);
		} catch (Exception e) {
			LOG.error("Error retrieving the news RSS feed: ", e);
			
		}
		
		
		return "frontPage";
	}
}
