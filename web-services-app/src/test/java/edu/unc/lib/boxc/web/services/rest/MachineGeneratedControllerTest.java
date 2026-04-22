package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MachineGeneratedControllerTest {
    private static final String PARENT1_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String FILE1_ID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String FILE2_ID = "b150dca9-c4cf-4651-aeef-3ce9e279178f";
    private static final PID PARENT1_PID = PIDs.get(PARENT1_ID);
    private static final PID FILE1_PID = PIDs.get(FILE1_ID);
    private static final PID FILE2_PID = PIDs.get(FILE2_ID);
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private SearchStateFactory searchStateFactory;
    @Mock
    private SearchState searchState;
    @Mock
    private MachineGeneratedContentService machineGeneratedContentService;
    @InjectMocks
    private MachineGeneratedSearchController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        machineGeneratedContentService = new MachineGeneratedContentService();
        controller = new MachineGeneratedSearchController();
        controller.setAccessControlService(accessControlService);
        controller.setMachineGeneratedContentService(machineGeneratedContentService);
        controller.setQueryLayer(queryLayer);
        controller.setSearchStateFactory(searchStateFactory);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        when(searchStateFactory.createSearchState(any())).thenReturn(searchState);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermissionErrorResponse() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), any(), any(AccessGroupSetImpl.class), eq(viewHidden));

        mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testSuccessResponse() throws Exception {
        MvcResult result1 = mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }


}
