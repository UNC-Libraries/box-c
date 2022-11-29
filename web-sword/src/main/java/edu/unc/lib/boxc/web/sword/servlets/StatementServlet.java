package edu.unc.lib.boxc.web.sword.servlets;

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

import edu.unc.lib.boxc.web.sword.SwordConfigurationImpl;

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
