package edu.unc.lib.boxc.deposit.http;

import edu.unc.lib.boxc.common.util.CLIUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.deposit.http.HealthCheckController.FITS_CLI_TIMEOUT_SECONDS;
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
            mvc.perform(get("/health/depositAppUp"))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }
    @Test
    public void depositAppDownTest() throws Exception {
        try (MockedStatic<CLIUtil> mockedStatic = mockStatic(CLIUtil.class)) {
            mockedStatic.when(() -> CLIUtil.executeCommand(anyList(), eq(FITS_CLI_TIMEOUT_SECONDS)))
                    .thenReturn(List.of("", "there was an error"));
            mvc.perform(get("/health/depositAppUp"))
                    .andExpect(status().isServiceUnavailable())
                    .andReturn();
        }
    }
}
