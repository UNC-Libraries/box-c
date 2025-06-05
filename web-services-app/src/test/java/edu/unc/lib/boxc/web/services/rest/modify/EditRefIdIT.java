package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSender;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getEmail;
import static edu.unc.lib.boxc.web.services.rest.modify.EditRefIdController.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class EditRefIdIT extends AbstractAPIIT {
    private static final String WORK1_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_ID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String REF1_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private static final String REF2_ID = "1651ewt75rgs1517g4re2rte16874se";
    private AutoCloseable closeable;
    private Path csvPath;
    private AgentPrincipals agent;
    private String email;
    @TempDir
    public Path tmpFolder;
    @Mock
    private BulkRefIdRequestSender sender;
    @Captor
    private ArgumentCaptor<BulkRefIdRequest> requestCaptor;
    @InjectMocks
    private EditRefIdController controller;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        csvPath = tmpFolder.resolve("bulkRefId");
        agent = getAgentPrincipals();
        email = getEmail();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void importRefIdsSuccess() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(WORK1_ID, REF1_ID);
        map.put(WORK2_ID, REF2_ID);

        createCsv();
        var file = createCsvFile();

        mvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(sender).sendToQueue(requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(map, request.getRefIdMap());
        assertEquals(agent, request.getAgent());
        assertEquals(email, request.getEmail());
    }

    @Test
    public void importRefIdsError() throws Exception {
        createCsv();
        var file = createCsvFile();

        doThrow(IOException.class).when(sender).sendToQueue(any());

        mvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    private void createCsv() throws IOException {
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
            csvPrinter.printRecord(WORK1_ID, REF1_ID);
            csvPrinter.printRecord(WORK2_ID, REF2_ID);
        }
    }

    private MockMultipartFile createCsvFile() throws Exception {
        var inputStream = Files.newInputStream(csvPath);
        return new MockMultipartFile("file", inputStream);
    }
}
