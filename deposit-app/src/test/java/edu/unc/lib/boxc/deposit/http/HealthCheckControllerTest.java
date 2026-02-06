package edu.unc.lib.boxc.deposit.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthCheckControllerTest {
    @InjectMocks
    private HealthCheckController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }
    @Test
    public void depositAppUpTest() throws Exception {
        mvc.perform(get("/health/depositAppUp"))
                .andExpect(status().isOk())
                .andReturn();
    }
    @Test
    public void depositAppDownTest() throws Exception {
        mvc.perform(get("/health/depositAppUp"))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
    }
}
