package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SiteMapControllerTest {
    private MockMvc mockMvc;
    private AutoCloseable closeable;


    @InjectMocks
    private SiteMapController controller;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessGroupSetImpl accessGroups;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setup() throws IOException {
        closeable = openMocks(this);
        Files.writeString(tmpFolder.resolve("sitemap.xml"), "");
        Files.writeString(tmpFolder.resolve("page_1.xml"), "");
        Files.writeString(tmpFolder.resolve("beige_11.xml"), "");
        controller.setSitemapBasePath(tmpFolder.toString());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetSitemapRoot() throws Exception {
        mockMvc.perform(get("/sitemap.xml")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    public void testGetSitemapPage() throws Exception {
        mockMvc.perform(get("/sitemap/{page}", "page_1.xml")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    public void testGetSitemapForValidRequest() throws Exception {
        mockMvc.perform(get("/sitemap/{page}", "page_2.xml")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testGetSitemapInvalidPage() throws Exception {
        mockMvc.perform(get("/sitemap/{page}", "beige_11.xml")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
