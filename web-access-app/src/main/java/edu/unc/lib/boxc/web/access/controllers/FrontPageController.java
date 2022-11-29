package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller which populates the dynamic components making up the front page of
 * the public UI
 * @author bbpennel
 */
@Controller
@RequestMapping("/")
public class FrontPageController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(FrontPageController.class);

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model, HttpServletRequest request) {
        LOG.debug("In front page controller");
        AccessGroupSet groups = GroupsThreadStore.getPrincipals();

        // Retrieve collection stats
        model.addAttribute("formatCounts", this.queryLayer.getFormatCounts(groups));
        model.addAttribute("isHomepage", true);

        model.addAttribute("menuId", "home");

        return "frontPage";
    }
}
