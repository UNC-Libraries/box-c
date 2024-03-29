package edu.unc.lib.boxc.services.camel.http;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.health.HealthCheckService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration("/health-check-test-context.xml")
public class HealthChecksControllerTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private CamelContext metaServicesRouter;
    @Mock
    private HealthCheckService healthCheckService;
    private MockMvc mvc;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        when(metaServicesRouter.hasService(HealthCheckService.class)).thenReturn(healthCheckService);
    }

    @Test
    public void camelUpOkTest() throws Exception {
        var healthCheck = mock(HealthCheck.class);
        var checkResult = HealthCheckResultBuilder.on(healthCheck).up().build();
        when(healthCheckService.getResults()).thenReturn(Arrays.asList(checkResult));

        mvc.perform(get("/health/camelUp"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void camelUpUnavailableTest() throws Exception {
        var healthCheck = mock(HealthCheck.class);
        var checkResult = HealthCheckResultBuilder.on(healthCheck).down().build();
        when(healthCheckService.getResults()).thenReturn(Arrays.asList(checkResult));

        mvc.perform(get("/health/camelUp"))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
    }
}
