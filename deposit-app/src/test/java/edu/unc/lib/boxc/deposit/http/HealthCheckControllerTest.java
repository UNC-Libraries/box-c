package edu.unc.lib.boxc.deposit.http;

import edu.unc.lib.boxc.common.util.CLIUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.deposit.http.HealthCheckController.FITS_CLI_TIMEOUT_SECONDS;
import static edu.unc.lib.boxc.deposit.http.HealthCheckController.HealthStatus.DOWN;
import static edu.unc.lib.boxc.deposit.http.HealthCheckController.HealthStatus.FITS_UNAVAILABLE;
import static edu.unc.lib.boxc.deposit.http.HealthCheckController.HealthStatus.OK;
import static edu.unc.lib.boxc.deposit.http.HealthCheckController.HealthStatus.SERVER_PROBLEM;
import static edu.unc.lib.boxc.deposit.http.HealthCheckController.HealthStatus.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthCheckControllerTest {
    @InjectMocks
    private HealthCheckController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() throws IOException {
        Path fitsHomePath = tmpFolder.resolve("fits");
        closeable = MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
        controller.setFitsHomePath(fitsHomePath.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void depositAppUpTest() throws Exception {
        try (MockedStatic<CLIUtil> mockedStatic = mockStatic(CLIUtil.class)) {
            mockedStatic.when(() -> CLIUtil.executeCommand(anyList(), eq(FITS_CLI_TIMEOUT_SECONDS)))
                    .thenReturn(List.of("success!", ""));
            var result = mvc.perform(get("/health/depositAppUp"))
                    .andExpect(status().isOk())
                    .andReturn();

            var body = result.getResponse().getContentAsString();
            JSONObject jsonObject = new JSONObject(body);

            assertEquals(UP, jsonObject.get("status"));
            assertEquals(OK, jsonObject.get("message"));
        }
    }

    @Test
    public void depositAppDownFitsUnavailableTest() throws Exception {
        try (MockedStatic<CLIUtil> mockedStatic = mockStatic(CLIUtil.class)) {
            mockedStatic.when(() -> CLIUtil.executeCommand(anyList(), eq(FITS_CLI_TIMEOUT_SECONDS)))
                    .thenReturn(List.of("", "bad stuff"));

            var result = mvc.perform(get("/health/depositAppUp"))
                    .andExpect(status().isServiceUnavailable())
                    .andReturn();

            var body = result.getResponse().getContentAsString();
            JSONObject jsonObject = new JSONObject(body);

            assertEquals(DOWN, jsonObject.get("status"));
            assertEquals(FITS_UNAVAILABLE, jsonObject.get("message"));
        }
    }

    @Test
    public void depositAppDownTempFileErrorTest() throws Exception {
        try (MockedStatic<File> mockedStatic = mockStatic(File.class)) {
            mockedStatic.when(() -> File.createTempFile(any(), any()))
                    .thenThrow(new IOException());

            var result = mvc.perform(get("/health/depositAppUp"))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            var body = result.getResponse().getContentAsString();
            JSONObject jsonObject = new JSONObject(body);

            assertEquals(DOWN, jsonObject.get("status"));
            assertEquals(SERVER_PROBLEM, jsonObject.get("message"));
        }
    }
}
