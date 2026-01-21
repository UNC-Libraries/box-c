package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.operations.impl.metadata.ExportDominoMetadataService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class ExportDominoControllerTest {
    @Mock
    private ExportDominoMetadataService exportDominoMetadataService;
    @InjectMocks
    private ExportDominoController controller;
    protected MockMvc mvc;
    private AutoCloseable closeable;
    @TempDir
    public Path tmpFolder;
    private Path csvFilePath;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        csvFilePath = tmpFolder.resolve("test.csv");
        Files.createFile(csvFilePath);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void testExportDominoSuccess() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();

        when(exportDominoMetadataService.exportCsv(anyList(), any(), eq("2020-01-01T00:00:00.0Z"), eq("*")))
                .thenReturn(csvFilePath);

        mvc.perform(get("/exportDomino/" + pidString))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"domino_export.csv\""))
                .andReturn();
    }

    @Test
    void testExportDominoWithCustomStartDate() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var customStartDate = "2024-01-01T00:00:00.0Z";

        when(exportDominoMetadataService.exportCsv(anyList(), any(), eq(customStartDate), eq("*")))
                .thenReturn(csvFilePath);

        mvc.perform(get("/exportDomino/" + pidString)
                        .param("startDate", customStartDate))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    void testExportDominoNotFound() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();

        when(exportDominoMetadataService.exportCsv(anyList(), any(), any(), eq("*")))
                .thenThrow(new NotFoundException("Object not found"));

        mvc.perform(get("/exportDomino/" + pidString))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void testExportDominoIOException() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();

        when(exportDominoMetadataService.exportCsv(anyList(), any(), any(), eq("*")))
                .thenThrow(new IOException("IO error"));

        mvc.perform(get("/exportDomino/" + pidString))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    void testExportDominoRepositoryException() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();

        when(exportDominoMetadataService.exportCsv(anyList(), any(), any(), eq("*")))
                .thenThrow(new RepositoryException("Repository error"));

        mvc.perform(get("/exportDomino/" + pidString))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }
}
