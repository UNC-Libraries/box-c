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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.boxc.auth.fcrepo.model.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.ImportThumbnailService;

/**
 * Controller for handling submission requests for collection display thumbnails
 *
 * @author lfarrell
 *
 */
@Controller
public class EditThumbnailController {
    private static final Logger log = LoggerFactory.getLogger(EditThumbnailController.class);

    @Autowired
    private ImportThumbnailService service;

    @PostMapping(value = "edit/displayThumbnail/{pid}")
    public @ResponseBody
    ResponseEntity<Object> ImportThumbnail(@PathVariable("pid") String pid,
                                                     @RequestParam("file") MultipartFile thumbnailFile)
            throws Exception {

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        String mimeType = thumbnailFile.getContentType();

        Map<String, Object> result = new HashMap<>();
        result.put("action", "editThumbnail");
        result.put("username", agent.getUsername());

        try (InputStream importStream = thumbnailFile.getInputStream()) {
            service.run(importStream, agent, pid, mimeType);
        } catch (IOException e) {
            log.error("Failed to get submitted file", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.error("Error queueing the job", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        result.put("destination", pid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
