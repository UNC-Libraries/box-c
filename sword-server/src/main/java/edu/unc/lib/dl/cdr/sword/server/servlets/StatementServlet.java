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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.swordapp.server.StatementAPI;
import org.swordapp.server.StatementManager;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.STATE_PATH)
public class StatementServlet extends BaseSwordServlet {
    private static Logger log = LoggerFactory.getLogger(StatementServlet.class);

    @Resource
    private StatementManager statementManager;
    private StatementAPI statementApi;

    @PostConstruct
    public void init() throws ServletException {
        this.statementApi = new StatementAPI(this.statementManager, this.config);
    }

    @RequestMapping(value = {"", "/", "/{pid}"}, method = RequestMethod.GET)
    protected void getStatement(HttpServletRequest req, HttpServletResponse resp) {
        log.debug("Retrieving statement for " + req.getRequestURI());
        try {
            this.statementApi.get(req, resp);
        } catch (Exception e) {
            log.error("Error retrieving statement for " + req.getRequestURI(), e);
        }
    }

    public StatementManager getStatementManager() {
        return statementManager;
    }

    public void setStatementManager(StatementManager statementManager) {
        this.statementManager = statementManager;
    }
}
