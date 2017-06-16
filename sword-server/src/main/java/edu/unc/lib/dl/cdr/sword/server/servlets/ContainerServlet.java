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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.swordapp.server.ContainerAPI;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.StatementManager;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.EDIT_PATH)
public class ContainerServlet extends BaseSwordServlet {
    private static Logger log = Logger.getLogger(ContainerServlet.class);

    @Resource
    private ContainerManager containerManager;
    private ContainerAPI api;
    private StatementManager statementManager;

    @PostConstruct
    public void init() throws ServletException {
        statementManager = null;
        this.api = new ContainerAPI(containerManager, statementManager, this.config);
    }

    @RequestMapping(value = { "/{pid}", "/{pid}/*" }, method = RequestMethod.DELETE)
    public void deleteContainer(HttpServletRequest req, HttpServletResponse resp) {
        try {
            this.api.delete(req, resp);
        } catch (Exception e) {
            log.error("Failed to delete container " + req.getQueryString(), e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @RequestMapping(value = { "/{pid}", "/{pid}/*" }, method = RequestMethod.PUT)
    public void replaceMetadataOrMetadataAndContent(HttpServletRequest req, HttpServletResponse resp) {
        try {
            this.api.put(req, resp);
        } catch (Exception e) {
            log.error("Failed to update container " + req.getQueryString(), e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @RequestMapping(value = {"/{pid}", "/{pid}/*" }, method = RequestMethod.POST)
    public void addMetadataOrMetadataAndContent(HttpServletRequest req, HttpServletResponse resp) {
        try {
            this.api.post(req, resp);
        } catch (Exception e) {
            log.error("Failed to update container " + req.getQueryString(), e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @RequestMapping(value = {"/{pid}", "/{pid}/*" }, method = RequestMethod.GET)
    public void getDepositReceiptOrStatement(HttpServletRequest req, HttpServletResponse resp) {
        try {
            this.api.get(req, resp);
        } catch (Exception e) {
            log.error("Failed to get deposit receipt for " + req.getQueryString(), e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public void setContainerManager(ContainerManager containerManager) {
        this.containerManager = containerManager;
    }

    public void setStatementManager(StatementManager statementManager) {
        this.statementManager = statementManager;
    }
}
