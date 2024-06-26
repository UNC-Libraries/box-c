package edu.unc.lib.boxc.services.camel.http;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class HealthChecksControllerTest {
    @Mock
    private CamelContext metaServicesRouter;
    @InjectMocks
    private HealthChecksController controller;

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
    public void camelUpOkTest() throws Exception {
        try (MockedStatic<HealthCheckHelper> mockedStatic = mockStatic(HealthCheckHelper.class)) {
            var healthCheck = mock(HealthCheck.class);
            var checkResult = HealthCheckResultBuilder.on(healthCheck).up().build();
            mockedStatic.when(() -> HealthCheckHelper.invoke(metaServicesRouter)).thenReturn(Arrays.asList(checkResult));

            mvc.perform(get("/health/camelUp"))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    @Test
    public void camelUpUnavailableTest() throws Exception {
        try (MockedStatic<HealthCheckHelper> mockedStatic = mockStatic(HealthCheckHelper.class)) {
            var healthCheck = mock(HealthCheck.class);
            var checkResult = HealthCheckResultBuilder.on(healthCheck).down().build();
            mockedStatic.when(() -> HealthCheckHelper.invoke(metaServicesRouter)).thenReturn(Arrays.asList(checkResult));

            mvc.perform(get("/health/camelUp"))
                    .andExpect(status().isServiceUnavailable())
                    .andReturn();
        }
    }
}
