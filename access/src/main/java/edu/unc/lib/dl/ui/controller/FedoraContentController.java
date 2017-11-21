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
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

/**
 * Controller which handles requests for specific content datastreams from Fedora and streams the results back as the
 * response.
 *
 * @author bbpennel
 */
@Controller
public class FedoraContentController {
//    @Autowired
//    private FedoraContentService fedoraContentService;

    @RequestMapping("/indexablecontent/{pid}")
    public void getDefaultIndexableContent(@PathVariable("pid") String pid,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(pid, ContentModelHelper.Datastream.DATA_FILE.getName(), download,
//                new AnalyticsUserData(request), response);
    }

    @RequestMapping("/indexablecontent/{pid}/{datastream}")
    public void getIndexableContent(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(pid, datastream, download, new AnalyticsUserData(request), response);
    }

    @RequestMapping("/indexablecontent")
    public void getIndexableContentByParameters(@RequestParam("id") String id, @RequestParam("ds") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(id, datastream, download, new AnalyticsUserData(request), response);
    }

    @RequestMapping("/content/{pid}")
    public void getDefaultDatastream(@PathVariable("pid") String pid,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(pid, ContentModelHelper.Datastream.DATA_FILE.getName(), download,
//                new AnalyticsUserData(request), response);
    }

    @RequestMapping("/content/{pid}/{datastream}")
    public void getDatastream(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(pid, datastream, download, new AnalyticsUserData(request), response);
    }

    @RequestMapping("/content")
    public void getDatastreamByParameters(@RequestParam("id") String id, @RequestParam("ds") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request, HttpServletResponse response) {
//        fedoraContentService.streamData(id, datastream, download, new AnalyticsUserData(request), response);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid content");
        return "error/invalidRecord";
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidRecordRequestException.class)
    public String handleInvalidRecordRequest(HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid content");
        return "error/invalidRecord";
    }
}