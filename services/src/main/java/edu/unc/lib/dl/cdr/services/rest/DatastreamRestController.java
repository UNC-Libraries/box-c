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
package edu.unc.lib.dl.cdr.services.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.ui.service.FedoraContentService;
import edu.unc.lib.dl.ui.util.AnalyticsTrackerUtil.AnalyticsUserData;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
public class DatastreamRestController {
    @Autowired
    private FedoraContentService fedoraContentService;

    @RequestMapping("/file/{pid}")
    public void getDatastream(@PathVariable("pid") String pid,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
        fedoraContentService.streamData(pid, null, download, new AnalyticsUserData(request), response);
    }

    @RequestMapping("/file/{pid}/{datastream}")
    public void getDatastream(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
        fedoraContentService.streamData(pid, datastream, download, new AnalyticsUserData(request), response);
    }

    @RequestMapping("/thumb/{pid}")
    public void getThumbnailSmall(@PathVariable("pid") String pid,
            @RequestParam(value = "size", defaultValue = "small") String size, HttpServletRequest request,
            HttpServletResponse response) {
        fedoraContentService.streamData(pid, ContentModelHelper.Datastream.THUMB_SMALL.getName(),
                false, null, response);
    }

    @RequestMapping("/thumb/{pid}/{size}")
    public void getThumbnail(@PathVariable("pid") String pid,
            @PathVariable("size") String size, HttpServletRequest request,
            HttpServletResponse response) {
        String datastream = ("large".equals(size)) ? ContentModelHelper.Datastream.THUMB_LARGE.getName()
                : ContentModelHelper.Datastream.THUMB_SMALL.getName();
        fedoraContentService.streamData(pid, datastream, false, null, response);
    }
}