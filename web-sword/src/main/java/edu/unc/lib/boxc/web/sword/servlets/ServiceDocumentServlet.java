package edu.unc.lib.boxc.web.sword.servlets;

import java.io.IOException;

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
import org.swordapp.server.ServiceDocumentAPI;
import org.swordapp.server.ServiceDocumentManager;

import edu.unc.lib.boxc.web.sword.SwordConfigurationImpl;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.SERVICE_DOCUMENT_PATH)
public class ServiceDocumentServlet extends BaseSwordServlet {
    private static Logger LOG = LoggerFactory.getLogger(ServiceDocumentServlet.class);
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