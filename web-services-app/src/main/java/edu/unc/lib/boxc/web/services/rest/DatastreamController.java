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
package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.isDerivative;

import java.io.FileNotFoundException;
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

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.DerivativeContentService;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;

/**
 *
 * Controller which provides access to datastreams of objects held in the repository.
 *
 * @author bbpennel
 *
 */
@Controller
public class DatastreamController {
    private static final Logger log = LoggerFactory.getLogger(DatastreamController.class);

    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;
    @Autowired
    private DerivativeContentService derivativeContentService;
    @Autowired
    private AccessCopiesService accessCopiesService;

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
            if (isDerivative(datastream)) {
                derivativeContentService.streamData(pid, datastream, principals, false, response);
            } else if (DatastreamType.MD_EVENTS.getId().equals(datastream)) {
                fedoraContentService.streamEventLog(pid, principals, download, response);
            } else {
                fedoraContentService.streamData(pid, datastream, principals, download, response);
                if (datastream == null || DatastreamType.ORIGINAL_FILE.getId().equals(datastream)) {
                    recordDownloadEvent(pid, datastream, principals, request);
                }
            }
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

    @RequestMapping("/thumb/{pid}")
    public void getThumbnailSmall(@PathVariable("pid") String pid,
            @RequestParam(value = "size", defaultValue = "small") String size, HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        getThumbnail(pid, size, request, response);
    }

    @RequestMapping("/thumb/{pid}/{size}")
    public void getThumbnail(@PathVariable("pid") String pidString,
            @PathVariable("size") String size, HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        String thumbName = "thumbnail_" + size.toLowerCase().trim();
        ContentObjectRecord thumbnailObj = accessCopiesService.getThumbnailObject(pid, principals);
        derivativeContentService.streamData(thumbnailObj.getPid(), thumbName, principals, false, response);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class, FileNotFoundException.class})
    public void handleResourceNotFound() {
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessRestrictionException.class)
    public void handleInvalidRecordRequest() {
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ObjectTypeMismatchException.class, IllegalArgumentException.class})
    public void handleBadRequest() {
    }
}