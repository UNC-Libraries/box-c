package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controller which returns sitemaps for DCR content
 *
 * @author lfarrell
 */
@Controller
public class SiteMapController  extends AbstractErrorHandlingSearchController {
    private final String sitemapBasePath;
    private static final Logger LOG = LoggerFactory.getLogger(SiteMapController.class);

    public SiteMapController(String sitemapBasePath) {
        this.sitemapBasePath = sitemapBasePath;
    }

    @GetMapping(value="sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<Resource> xmlSitemap() {
        LOG.info("XML sitemap contents list");

        try {
            var content = Paths.get(sitemapBasePath, "sitemap.xml");
            UrlResource urlResource = new UrlResource(content.toUri());
            return ResponseEntity.ok()
                    .body(urlResource);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value="sitemap/{page}", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<String> xmlSitemapPage(@PathVariable("page") String page) {
        LOG.debug("Searching for sitemap page: " + page);

        try {
            if (!page.matches("^page_\\d+\\.xml$")) {
                throw new IOException("Invalid sitemap page number requested: " + page);
            }

            String content = Files.readString(Paths.get(sitemapBasePath + page));
            return new ResponseEntity<>(content, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
