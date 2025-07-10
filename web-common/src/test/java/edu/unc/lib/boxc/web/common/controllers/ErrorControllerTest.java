package edu.unc.lib.boxc.web.common.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorControllerTest {
    @InjectMocks
    private ErrorController controller;
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
    public void test403() throws Exception {
        mvc.perform(get("/403"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void test404() throws Exception {
        mvc.perform(get("/404"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testException() throws Exception {
        mvc.perform(get("/exception")
                .with(request -> {
                    request.setAttribute("jakarta.servlet.error.exception", new IllegalStateException("Test Exception"));
                    request.setAttribute("jakarta.servlet.forward.request_uri", "/test-uri");
                    return request;
                }))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }
}
