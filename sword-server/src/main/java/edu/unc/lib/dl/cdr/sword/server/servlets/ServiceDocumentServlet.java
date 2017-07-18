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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.swordapp.server.ServiceDocumentAPI;
import org.swordapp.server.ServiceDocumentManager;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.SERVICE_DOCUMENT_PATH)
public class ServiceDocumentServlet extends BaseSwordServlet {
    private static Logger LOG = Logger.getLogger(ServiceDocumentServlet.class);
    @Resource
    protected ServiceDocumentManager serviceDocumentManager;
    protected ServiceDocumentAPI api;

    @PostConstruct
    public void init() throws ServletException {
        // load the api
        this.api = new ServiceDocumentAPI(this.serviceDocumentManager, this.config);
    }

    @RequestMapping(value = {"", "/", "/{pid}"}, method = RequestMethod.GET)
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.debug("Get request for service document " + req.getQueryString());
        this.api.get(req, resp);
    }

    public ServiceDocumentManager getServiceDocumentManager() {
        return serviceDocumentManager;
    }

    public void setServiceDocumentManager(ServiceDocumentManager serviceDocumentManager) {
        this.serviceDocumentManager = serviceDocumentManager;
    }
}