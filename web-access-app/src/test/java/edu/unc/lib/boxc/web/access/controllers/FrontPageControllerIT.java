package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author krwong
 */
public class FrontPageControllerIT {
    protected MockMvc mvc;

    @Mock
    private SolrQueryLayerService queryLayer;
    @InjectMocks
    private FrontPageController controller;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("adminGroup"));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetCollectionStats() throws Exception {
        var collectionStats = Map.of("image", 386710L, "audio", 5027L,
                "video", 21444L, "text", 46936L);
        when(queryLayer.getFormatCounts(any())).thenReturn(collectionStats);

        MvcResult result = mvc.perform(get("/collectionStats")
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn();

        String resultJson = result.getResponse().getContentAsString();
        assertTrue(resultJson.contains("formatCounts"));
        assertTrue(resultJson.contains("\"image\":386710"));
        assertTrue(resultJson.contains("\"audio\":5027"));
        assertTrue(resultJson.contains("\"video\":21444"));
        assertTrue(resultJson.contains("\"text\":46936"));
    }
}
