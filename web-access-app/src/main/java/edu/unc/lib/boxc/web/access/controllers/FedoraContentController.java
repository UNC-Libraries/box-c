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
package edu.unc.lib.boxc.web.access.controllers;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;

/**
 * Controller which handles requests for specific content datastreams from Fedora and streams the results back as the
 * response.
 *
 * @author bbpennel
 */
@Controller
public class FedoraContentController {
    private static final Logger log = LoggerFactory.getLogger(FedoraContentController.class);

    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    @RequestMapping(value = {"/content/{pid}", "/indexablecontent/{pid}"})
    public void getDefaultDatastream(@PathVariable("pid") String pid,
                                     @RequestParam(value = "dl", defaultValue = "false") boolean download,
                                     HttpServletRequest request, HttpServletResponse response) {
        streamData(pid, ORIGINAL_FILE.getId(), download, request, response);
    }

    @RequestMapping(value = {"/content/{pid}/{datastream}", "/indexablecontent/{pid}/{datastream}"})
    public void getDatastream(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
                              @RequestParam(value = "dl", defaultValue = "false") boolean download,
                              HttpServletRequest request, HttpServletResponse response) {
        streamData(pid, datastream, download, request, response);
    }

    @RequestMapping(value = {"/indexablecontent", "/content"})
    public void getDatastreamByParameters(@RequestParam("id") String id, @RequestParam("ds") String datastream,
                                          @RequestParam(value = "dl", defaultValue = "false") boolean download,
                                          HttpServletRequest request, HttpServletResponse response) {
        streamData(id, ORIGINAL_FILE.getId(), download, request, response);
    }

    private void streamData(String pidString, String datastream, boolean asAttachment, HttpServletRequest request,
                            HttpServletResponse response) {
        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
        if (repoObj instanceof WorkObject) {
            FileObject primaryObj = ((WorkObject) repoObj).getPrimaryObject();
            if (primaryObj != null) {
                pid = primaryObj.getPid();
            }
        }

        try {
            fedoraContentService.streamData(pid, datastream, principals, asAttachment, response);
            recordDownloadEvent(pid, datastream, principals, request);
        } catch (IOException e) {
            log.error("Problem retrieving {} for {}", pid.toString(), datastream, e);
        }
    }

    private void recordDownloadEvent(PID pid, String datastream, AccessGroupSet principals,
                                     HttpServletRequest request) {
        if (!(StringUtils.isBlank(datastream) || ORIGINAL_FILE.getId().equals(datastream))) {
            return;
        }
        analyticsTracker.trackEvent(request, "download", pid, principals);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class})
    public String handleResourceNotFound(HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid content");
        return "error/invalidRecord";
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessRestrictionException.class)
    public String handleInvalidRecordRequest(HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid content");
        return "error/invalidRecord";
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ObjectTypeMismatchException.class)
    public String handleObjectTypeMismatchException(HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid content");
        return "error/invalidRecord";
    }

    @ExceptionHandler(value = { RuntimeException.class })
    protected String handleUncaught(RuntimeException ex, WebRequest request) {
        log.error("Uncaught exception while streaming content", ex);
        return "error/exception";
    }
}