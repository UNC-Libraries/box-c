package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.fullDescription.FullDescriptionUpdateService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.web.services.utils.MachineUpdateServiceTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FullDescriptionControllerTest {
    private MockMvc mockMvc;
    @Mock
    private FullDescriptionUpdateService service;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private BinaryTransferSession transferSession;
    @Mock
    private BinaryTransferService transferService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObject repositoryObject;
    private PID pid;
    private PID fulldescPid;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        pid = TestHelper.makePid();
        fulldescPid = DatastreamPids.getFullDescriptionPid(pid);

        var controller = new FullDescriptionController();
        controller.setFullDescriptionUpdateService(service);
        controller.setRepositoryObjectLoader(repositoryObjectLoader);
        controller.setTransferService(transferService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void updateFullDescTest() throws Exception {
        var description = "The best full description ever";
        when(binaryObject.getPid()).thenReturn(fulldescPid);
        when(service.updateFullDescription(any())).thenReturn(binaryObject);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(repositoryObject);
        when(transferService.getSession(eq(repositoryObject))).thenReturn(transferSession);

        var result = mockMvc.perform(post("/edit/fullDescription/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullDescription", description))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        MachineUpdateServiceTestHelper.assertResponse(result, "updateFullDescription", fulldescPid);

        // Verify that the service was called with the correct request
        verify(service).updateFullDescription(argThat(request ->
                request.getPidString().equals(pid.getId()) &&
                        request.getFullDescriptionText().equals(description)
        ));
    }

    @Test
    public void updateFullDescNoPermissionTest() throws Exception {
        var description = "The best full description ever";

        doThrow(new AccessRestrictionException("Access Denied"))
                .when(service).updateFullDescription(any());

        mockMvc.perform(post("/edit/fullDescription/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullDescription", description))
                .andExpect(status().isForbidden());
    }
}
