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

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.ui.model.response.RssFeedBean;
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
    public String handleRequest(Model model, HttpServletRequest request) {
        LOG.debug("In front page controller");
        AccessGroupSet groups = GroupsThreadStore.getGroups();

        // Retrieve collection stats
        model.addAttribute("departmentsCount", this.queryLayer.getDepartmentsCount(groups));
        model.addAttribute("collectionsCount", this.queryLayer.getCollectionsCount(groups));
        model.addAttribute("formatCounts", this.queryLayer.getFormatCounts(groups));

        model.addAttribute("menuId", "home");

        try {
            RssFeedBean wpRssFeed =
                    RssParserService.getRssFeed(ExternalContentSettings.getUrl("wpRss"),
                            Integer.parseInt(ExternalContentSettings.get("external.wpRss.maxLinks")));
            model.addAttribute("wpRssItem", wpRssFeed.getItems().get(0));
        } catch (Exception e) {
            LOG.error("Error retreiving the CDR WordPress Collection Highlights feed: {}", e.getMessage());
        }

        return "frontPage";
    }
}
