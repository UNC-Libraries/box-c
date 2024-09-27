package edu.unc.lib.boxc.web.admin.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author lfarrell
 */
public class ChompbControllerIT {
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
        MvcResult result = mvc.perform(get("/chompb/project/test_proj/processing_results/velocicroptor/"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("report/chompb", result.getResponse().getForwardedUrl());
    }
}
