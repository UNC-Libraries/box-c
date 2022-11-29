package edu.unc.lib.boxc.web.sword.servlets;

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
import org.swordapp.server.MediaResourceAPI;
import org.swordapp.server.MediaResourceManager;

import edu.unc.lib.boxc.web.sword.MediaResourceAPITidy;
import edu.unc.lib.boxc.web.sword.SwordConfigurationImpl;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(SwordConfigurationImpl.EDIT_MEDIA_PATH)
public class MediaResourceServlet extends BaseSwordServlet {
    private static Logger log = LoggerFactory.getLogger(MediaResourceServlet.class);

    @Resource
    protected MediaResourceManager mediaResourceManager;
    protected MediaResourceAPI api;

    @PostConstruct
    public void init() throws ServletException {
        this.api = new MediaResourceAPITidy(mediaResourceManager, this.config);
    }

    /**
     * Retrieves the content files of the selected resource.
     * @param req
     * @param resp
     * @param pid
     */
    @RequestMapping(value = "/{pid}/{datastream}", method = RequestMethod.GET)
    public void doRetrieveContent(HttpServletRequest req, HttpServletResponse resp) {
        log.debug("Called retrieve content");
        try {
            this.api.get(req, resp);
        } catch (Exception e) {
            log.error("Failed to retrieve content for " + req.getQueryString(), e);
        }
    }

    /**
     * Overwrites all the content files of the selected resource
     * @param req
     * @param resp
     * @param pid
     */
    @RequestMapping(value = "/{pid}", method = RequestMethod.PUT)
    public void doReplaceFileContent(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
    }

    /**
     * Adds additional content to an existing resource.  If the POSTed content is a Atom Multipart, then it will add
     * both new content and new metadata.  Does not overwrite the previous contents,
     * but may overlay previous metadata.
     * @param req
     * @param resp
     * @param pid
     */
    @RequestMapping(value = "/{pid}", method = RequestMethod.POST)
    public void doAddAdditionalContent(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
    }

    /**
     * Deletes all the contents of the select resource, but not the resource itself.
     * @param req
     * @param resp
     * @param pid
     */
    @RequestMapping(value = "/{pid}", method = RequestMethod.DELETE)
    public void doDeleteContent(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
    }

    public void setMediaResourceManager(MediaResourceManager mediaResourceManager) {
        this.mediaResourceManager = mediaResourceManager;
    }
}
