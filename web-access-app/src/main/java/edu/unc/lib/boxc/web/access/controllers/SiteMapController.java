package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class SiteMapController  extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SiteMapController.class);

    @GetMapping(value="sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<String> XMLSitemap(HttpServletRequest request, HttpServletResponse response) {
        try {
            String content = Files.readString(Paths.get("/opt/data/sitemaps/sitemap.xml"));
            return new ResponseEntity<>(content, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value="sitemap/{page}", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<String> XMLSitemapPage(@PathVariable("page") String page,
                                      HttpServletRequest request, HttpServletResponse response) {
        try {
            String content = Files.readString(Paths.get("/opt/data/sitemaps/" + page));
            return new ResponseEntity<>(content, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
