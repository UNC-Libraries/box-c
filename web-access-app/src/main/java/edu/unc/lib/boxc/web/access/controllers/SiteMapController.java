package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

/**
 * Controller that returns sitemaps for DCR content
 *
 * @author lfarrell
 */
@Controller
public class SiteMapController  extends AbstractErrorHandlingSearchController {
    private String sitemapBasePath;
    private static final Logger LOG = LoggerFactory.getLogger(SiteMapController.class);

    public SiteMapController(String sitemapBasePath) {
        this.sitemapBasePath = sitemapBasePath;
    }

    @GetMapping(value="sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<Resource> xmlSitemap() {
        LOG.info("XML sitemap contents list");

        var content = Paths.get(sitemapBasePath, "sitemap.xml");
        PathResource pathResource = new PathResource(content.toUri());

        return ResponseEntity.ok().body(pathResource);
    }

    @GetMapping(value="sitemap/{page}", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody
    ResponseEntity<Resource> xmlSitemapPage(@PathVariable("page") String page) {
        LOG.debug("Searching for sitemap page: {}", page);

        try {
            if (!page.matches("^page_\\d+\\.xml$")) {
                LOG.debug("Invalid sitemap page number requested: {}", page);
                throw new IOException();
            }

            var content = Paths.get(sitemapBasePath, page);
            PathResource pathResource = new PathResource(content.toUri());
            if (!pathResource.exists()) {
                throw new FileNotFoundException();
            }

            return ResponseEntity.ok().body(pathResource);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    public void setSitemapBasePath(String sitemapBasePath) {
        this.sitemapBasePath = sitemapBasePath;
    }
}
