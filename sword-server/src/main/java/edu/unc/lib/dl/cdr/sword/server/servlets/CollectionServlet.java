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
package edu.unc.lib.dl.cdr.sword.server.servlets;

import java.io.IOException;
import java.util.Enumeration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.swordapp.server.CollectionAPI;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.COLLECTION_PATH)
public class CollectionServlet extends BaseSwordServlet {
    private static Logger LOG = LoggerFactory.getLogger(CollectionServlet.class);

    @Resource
    protected CollectionListManager collectionListManager;
    @Resource
    protected CollectionDepositManager collectionDepositManager;
    protected CollectionAPI api;

    @PostConstruct
    public void init() throws ServletException {
        // load the API
        this.api = new CollectionAPI(this.collectionListManager, this.collectionDepositManager, this.config);
    }

    @RequestMapping(value = { "", "/", "/{pid}", "/{pid}/*" }, method = RequestMethod.GET)
    public void collectionList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.debug("GET request for collection content list");
        this.api.get(req, resp);
    }

    @RequestMapping(value = { "", "/", "/{pid}" }, method = RequestMethod.POST)
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("POST request to submit to collection: " + req.getQueryString());
            LOG.debug("Packaging: " + req.getHeader("Packaging"));
            String headers = null;
            @SuppressWarnings("unchecked")
            Enumeration<String> e = req.getHeaderNames();
            LOG.debug("Collection submission headers:");
            while (e.hasMoreElements()) {
                headers = e.nextElement();
                if (headers != null) {
                    LOG.debug("  " + headers + ":" + req.getHeader(headers));
                }
            }
        }

        try {
            this.api.post(req, resp);
        } catch (Exception e) {
            LOG.error("An unhandled exception occurred while attempting to ingest to " + req.getRequestURI(), e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            resp.getWriter().write("An unexpected exception occurred while attempting to process your submission.");
        }
    }

    public CollectionListManager getCollectionListManager() {
        return collectionListManager;
    }

    public void setCollectionListManager(CollectionListManager collectionListManager) {
        this.collectionListManager = collectionListManager;
    }

    public CollectionDepositManager getCollectionDepositManager() {
        return collectionDepositManager;
    }

    public void setCollectionDepositManager(CollectionDepositManager collectionDepositManager) {
        this.collectionDepositManager = collectionDepositManager;
    }
}
