package edu.unc.lib.boxc.web.common.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorControllerIT {
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
//        var request = mock(HttpServletRequest.class);
//        when(request.getAttribute(eq("javax.servlet.error.exception"))).thenReturn(IllegalArgumentException.class);
//        when(request.getAttribute(eq("javax.servlet.forward.request_uri"))).thenReturn("uri");

        var request = new MockHttpServletRequest();
        request.setAttribute("javax.servlet.error.exception", IllegalArgumentException.class);
        request.setAttribute("javax.servlet.forward.request_uri", "uri");

        ObjectMapper mapper = new ObjectMapper();
        var writer = mapper.writerFor(MockHttpServletRequest.class);
        var content = writer.writeValueAsString(request);
        mvc.perform(post("/exception")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }
}
