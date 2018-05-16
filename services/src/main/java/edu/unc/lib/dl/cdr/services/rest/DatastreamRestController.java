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

import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ui.service.FedoraContentService;
import edu.unc.lib.dl.ui.util.AnalyticsTrackerUtil;

/**
 *
 * Controller which provides access to datastreams of objects held in the repository.
 *
 * @author bbpennel
 *
 */
@Controller
public class DatastreamRestController {
    private static final Logger log = LoggerFactory.getLogger(DatastreamRestController.class);

    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;

    @RequestMapping("/file/{pid}")
    public void getDatastream(@PathVariable("pid") String pidString,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request,
            HttpServletResponse response) {
        getDatastream(pidString, null, download, request, response);
    }

    @RequestMapping("/file/{pid}/{datastream}")
    public void getDatastream(@PathVariable("pid") String pidString,
            @PathVariable("datastream") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request,
            HttpServletResponse response) {

        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        try {
            fedoraContentService.streamData(pid, datastream, principals, download, response);
            recordDownloadEvent(pid, datastream, principals, request);
        } catch (IOException e) {
            log.error("Problem retrieving {} for {}", pid.toString(), datastream, e);
        }
    }

    private void recordDownloadEvent(PID pid, String datastream, AccessGroupSet principals,
            HttpServletRequest request) {
        if (!(StringUtils.isBlank(datastream) || ORIGINAL_FILE.equals(datastream))) {
            return;
        }
        analyticsTracker.trackEvent(request, "download", pid, principals);
    }

    @RequestMapping("/thumb/{pid}")
    public void getThumbnailSmall(@PathVariable("pid") String pid,
            @RequestParam(value = "size", defaultValue = "small") String size, HttpServletRequest request,
            HttpServletResponse response) {
        // TODO implement retrieval of derivatives
    }

    @RequestMapping("/thumb/{pid}/{size}")
    public void getThumbnail(@PathVariable("pid") String pid,
            @PathVariable("size") String size, HttpServletRequest request,
            HttpServletResponse response) {
        // TODO implement retrieval of derivatives
    }
}