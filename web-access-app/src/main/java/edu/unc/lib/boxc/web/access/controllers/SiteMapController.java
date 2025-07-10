package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static edu.unc.lib.boxc.model.api.ResourceType.Work;

@Controller
public class SiteMapController  extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SiteMapController.class);

    @RequestMapping("/sitemap.xml")
    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    String XMLSitemap(@RequestParam("page") Optional<Integer> page,
                                   HttpServletRequest request, HttpServletResponse response) {
        return "";
    }
}
