package edu.unc.lib.boxc.web.common.controllers;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AbstractErrorHandlingSearchControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .build();
    }

    @Test
    public void notFoundExceptionReturns404() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void forbiddenExceptionReturns403() throws Exception {
        mvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void badRequestExceptionReturns400() throws Exception {
        mvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void uncaughtRuntimeExceptionReturns500() throws Exception {
        mvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @RestController
    @RequestMapping("/test")
    private static class TestController extends AbstractErrorHandlingSearchController {

        @GetMapping("/not-found")
        public void notFound() {
            throw new NotFoundException("not found");
        }

        @GetMapping("/forbidden")
        public void forbidden() {
            throw new AccessRestrictionException();
        }

        @GetMapping("/bad-request")
        public void badRequest() {
            throw new IllegalArgumentException("bad input");
        }

        @GetMapping("/error")
        public void error() {
            throw new RuntimeException("boom");
        }
    }
}
