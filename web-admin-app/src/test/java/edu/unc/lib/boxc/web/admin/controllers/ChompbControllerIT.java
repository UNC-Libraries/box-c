package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.web.admin.controllers.processing.ChompbPreIngestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author lfarrell
 */
public class ChompbControllerIT {
    @Mock
    private ChompbPreIngestService chompbPreIngestService;
    @InjectMocks
    private ChompbController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testPageLoad() throws Exception {
        MvcResult result = mvc.perform(get("/chompb"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("report/chompb", result.getResponse().getForwardedUrl());
    }

    @Test
    public void testProcessingReportRequest() throws Exception {
        MvcResult result = mvc.perform(get("/chompb/project/test_proj/processing_results/velocicroptor"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("report/chompb", result.getResponse().getForwardedUrl());
    }

    @Test
    public void testListProjects() throws Exception {
        when(chompbPreIngestService.getProjectLists(any())).thenReturn("test");
        MvcResult result = mvc.perform(get("/chompb/listProjects"))
                .andExpect(status().isOk())
                .andReturn();
        var resp = result.getResponse().getContentAsString();
        assertEquals("test", resp);
    }

    @Test
    public void testGetProcessingResultsJson() throws Exception {
        var resultContent = "jsontest";
        var resultStream = new ByteArrayInputStream(resultContent.getBytes());
        when(chompbPreIngestService.getProcessingResults(any(), any(), any(), any())).thenReturn(resultStream);
        MvcResult result = mvc.perform(get("/chompb/project/test_proj/processing_results/velocicroptor/files?path=data.json"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("application/json", result.getResponse().getContentType());
        assertEquals("attachment; filename=data.json", result.getResponse().getHeader("Content-Disposition"));
        var resp = result.getResponse().getContentAsString();
        assertEquals(resultContent, resp);
    }

    @Test
    public void testGetProcessingResultsCsv() throws Exception {
        var resultContent = "csvtest";
        var resultStream = new ByteArrayInputStream(resultContent.getBytes());
        when(chompbPreIngestService.getProcessingResults(any(), any(), any(), any())).thenReturn(resultStream);
        MvcResult result = mvc.perform(get("/chompb/project/test_proj/processing_results/velocicroptor/files?path=data.csv"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("text/plain", result.getResponse().getContentType());
        assertEquals("attachment; filename=data.csv", result.getResponse().getHeader("Content-Disposition"));
        var resp = result.getResponse().getContentAsString();
        assertEquals(resultContent, resp);
    }

    @Test
    public void testGetProcessingResultsImage() throws Exception {
        var resultContent = "testimage";
        var resultStream = new ByteArrayInputStream(resultContent.getBytes());
        var imagePath = "images/path/to/chomp.jpg";
        when(chompbPreIngestService.getProcessingResults(any(), any(), any(), eq(imagePath))).thenReturn(resultStream);
        MvcResult result = mvc.perform(get("/chompb/project/test_proj/processing_results/velocicroptor/files?path=" + imagePath))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("image/jpeg", result.getResponse().getContentType());
        assertEquals("attachment; filename=chomp.jpg", result.getResponse().getHeader("Content-Disposition"));
        var resp = result.getResponse().getContentAsString();
        assertEquals(resultContent, resp);
    }
}
