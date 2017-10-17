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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

/**
 *
 * @author bbpennel
 *
 */
public class AbstractSwordController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(AbstractSwordController.class);
    private static final int INITIAL_BUFFER_SIZE = 2048;
    public static final ContentType APPLICATION_ATOM_XML_UTF8 = ContentType.create(
            "application/atom+xml", Consts.UTF_8);

    @Autowired
    private String swordUrl;
    @Autowired
    private String swordUsername;
    @Autowired
    private String swordPassword;

    public String updateDatastream(String pid, String datastream, HttpServletRequest request,
            HttpServletResponse response) {
        String responseString = null;
        String dataUrl = swordUrl + "object/" + pid;
        if (datastream != null) {
            dataUrl += "/" + datastream;
        }

        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();
        Parser parser = abdera.getParser();
        Document<FOMExtensibleElement> doc;
        CloseableHttpClient client;
        HttpPut method;

        ParserOptions parserOptions = parser.getDefaultParserOptions();
        parserOptions.setCharset(request.getCharacterEncoding());

        try {
            doc = parser.parse(request.getInputStream(), parserOptions);
            entry.addExtension(doc.getRoot());

            client = HttpClientUtil.getAuthenticatedClient(null, swordUsername, swordPassword);
            method = new HttpPut(dataUrl);
            // Pass the users groups along with the request
            method.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());
            method.addHeader("Content-Type", "application/atom+xml");
            StringWriter stringWriter = new StringWriter(INITIAL_BUFFER_SIZE);
            StringEntity requestEntity;
            entry.writeTo(stringWriter);
            requestEntity = new StringEntity(stringWriter.toString(), APPLICATION_ATOM_XML_UTF8);
            method.setEntity(requestEntity);
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding not supported", e);
            return null;
        } catch (IOException e) {
            log.error("IOException while writing entry", e);
            return null;
        }

        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            response.setStatus(statusCode);
            if (statusCode == HttpStatus.SC_NO_CONTENT) {
                // success
                return "";
            } else if (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    log.warn("Failed to upload " + datastream + " " + method.getURI());
                }
                // probably a validation problem
                responseString = EntityUtils.toString(httpResp.getEntity(), "UTF-8");
                return responseString;
            } else {
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                throw new Exception("Failure to update fedora content due to response of: " + httpResp.getStatusLine()
                        + "\nPath was: " + method.getURI());
            }
        } catch (Exception e) {
            log.error("Error while attempting to stream Fedora content for " + pid, e);
        }
        return responseString;
    }
}
