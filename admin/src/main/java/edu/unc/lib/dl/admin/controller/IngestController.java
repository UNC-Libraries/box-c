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
package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * Controller for handling forms which submit ingests to SWORD
 * 
 * @author bbpennel
 * 
 */
@Controller
public class IngestController {
    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    @Autowired
    private String swordUrl;
    @Autowired
    private String swordUsername;
    @Autowired
    private String swordPassword;
    private static QName SWORD_VERBOSE_DESCRIPTION = new QName(
            "http://purl.org/net/sword/terms/", "verboseDescription");

    @RequestMapping(value = "ingest/{pid}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, ? extends Object> ingestPackageController(@PathVariable("pid") String pid,
            @RequestParam("type") String type, @RequestParam(value = "name", required = false) String name,
            @RequestParam("file") MultipartFile ingestFile, HttpServletRequest request, HttpServletResponse response) {

        String destinationUrl = swordUrl + "collection/" + pid;
        CloseableHttpClient client = HttpClientUtil
                .getAuthenticatedClient(null, swordUsername, swordPassword);
        HttpPost method = new HttpPost(destinationUrl);

        // Set SWORD related headers for performing ingest
        method.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());
        method.addHeader("Packaging", type);
        method.addHeader("On-Behalf-Of", GroupsThreadStore.getUsername());
        method.addHeader("Content-Type", ingestFile.getContentType());
        method.addHeader("mail", request.getHeader("mail"));

        if (ingestFile.getOriginalFilename() != null) {
            try {
                method.setHeader("Content-Disposition",
                        "attachment; filename=" + URLEncoder.encode(ingestFile.getOriginalFilename(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.warn("Unable to properly encode value to UTF-8", e);
                method.addHeader("Content-Disposition", "attachment; filename=" + ingestFile.getOriginalFilename());
            }
        }
        if (name != null && name.trim().length() > 0) {
            try {
                method.addHeader("Slug", URLEncoder.encode(name, "UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                log.warn("Unable to properly encode value to UTF-8", e1);
                method.addHeader("Slug", name);
            }
        }

        // Setup the json response
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("action", "ingest");
        result.put("destination", pid);

        try {
            InputStreamEntity entity = new InputStreamEntity(ingestFile.getInputStream(), ingestFile.getSize());
            method.setEntity(entity);
        } catch (IOException e) {
            log.error("Failed to read ingest file", e);
            return null;
        }

        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            response.setStatus(statusCode);

            // Object successfully "create", or at least queued
            if (statusCode == 201) {
                String newPid = httpResp.getFirstHeader("Location").getValue();
                newPid = newPid.substring(newPid.lastIndexOf('/'));
                result.put("pid", newPid);
            } else if (statusCode == 401) {
                // Unauthorized
                result.put("error", "Not authorized to ingest to container " + pid);
            } else if (statusCode == 400 || statusCode >= 500) {
                // Server error, report it to the client
                result.put("error", "A server error occurred while attempting to ingest \"" + ingestFile.getName()
                        + "\" to " + pid);

                // Inspect the SWORD response, extracting the stacktrace
                InputStream entryPart = httpResp.getEntity().getContent();
                Abdera abdera = new Abdera();
                Parser parser = abdera.getParser();
                Document<Entry> entryDoc = parser.parse(entryPart);
                Object rootEntry = entryDoc.getRoot();
                String stackTrace;
                if (rootEntry instanceof FOMExtensibleElement) {
                    stackTrace = ((org.apache.abdera.parser.stax.FOMExtensibleElement) entryDoc.getRoot()).getExtension(
                            SWORD_VERBOSE_DESCRIPTION).getText();
                    result.put("errorStack", stackTrace);
                } else {
                    stackTrace = ((Entry) rootEntry).getExtension(SWORD_VERBOSE_DESCRIPTION).getText();
                    result.put("errorStack", stackTrace);
                }
                log.warn(
                        "Failed to upload ingest package file " + ingestFile.getName() + " from user "
                                + GroupsThreadStore.getUsername(), stackTrace);
            }
            return result;
        } catch (Exception e) {
            log.warn("Encountered an unexpected error while ingesting package " + ingestFile.getName() + " from user "
                    + GroupsThreadStore.getUsername(), e);
            result.put("error", "A server error occurred while attempting to ingest \""
                    + ingestFile.getName() + "\" to " + pid);
            return result;
        }
    }
}
