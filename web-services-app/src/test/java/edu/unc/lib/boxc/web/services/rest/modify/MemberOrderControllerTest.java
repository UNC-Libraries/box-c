package edu.unc.lib.boxc.web.services.rest.modify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.web.services.processing.MemberOrderCsvExporter;
import edu.unc.lib.boxc.web.services.processing.MemberOrderCsvTransformer;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/member-order-test-servlet.xml")
public class MemberOrderControllerTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MemberOrderCsvExporter csvExporter;
    @Autowired
    private MemberOrderRequestSender requestSender;
    @Autowired
    private MemberOrderCsvTransformer csvTransformer;
    @TempDir
    public Path tmpFolder;

    private MockMvc mvc;

    @BeforeEach
    public void init() throws Exception {
        reset(csvTransformer, csvExporter, requestSender);
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @Test
    public void memberOrderCsvExportSuccessTest() throws Exception {
        var csvPath = tmpFolder.resolve("testFile").toAbsolutePath();
        var expectedContent = "some,csv,data,goes,here";
        FileUtils.writeStringToFile(csvPath.toFile(), expectedContent, StandardCharsets.UTF_8);

        var ids = PARENT1_UUID + "," + PARENT2_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID), PIDs.get(PARENT2_UUID))), any(AgentPrincipals.class)))
                .thenReturn(csvPath);

        MvcResult result = mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var response = result.getResponse();
        assertEquals(expectedContent, response.getContentAsString());
    }

    @Test
    public void memberOrderCsvExportNoIdParamTest() throws Exception {
        mvc.perform(get("/edit/memberOrder/export/csv"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportEmptyIdParamTest() throws Exception {
        mvc.perform(get("/edit/memberOrder/export/csv?ids="))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportAccessFailureTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new AccessRestrictionException());

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportInvalidResourceTypeTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new InvalidOperationForObjectType());

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportServerErrorTest() throws Exception {
        var ids = PARENT1_UUID;
        when(csvExporter.export(eq(Arrays.asList(PIDs.get(PARENT1_UUID))), any(AgentPrincipals.class)))
                .thenThrow(new RepositoryException("Boom"));

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvExportInvalidPidsTest() throws Exception {
        var ids = "badpids,beingsubmitted";

        mvc.perform(get("/edit/memberOrder/export/csv?ids=" + ids))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvImportSuccessTest() throws Exception {
        var generatedRequest = new MultiParentOrderRequest();
        when(csvTransformer.toRequest(any())).thenReturn(generatedRequest);

        MockMultipartFile importFile = new MockMultipartFile("file",
                "some,csv,content".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/memberOrder/import/csv"))
                        .file(importFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("import member order", respMap.get("action"));
        verify(requestSender).sendToQueue(eq(generatedRequest));
    }

    @Test
    public void memberOrderCsvImportInvalidInputTest() throws Exception {
        when(csvTransformer.toRequest(any())).thenThrow(new IllegalArgumentException("this isn't right"));

        MockMultipartFile importFile = new MockMultipartFile("file",
                "some,csv,content".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/memberOrder/import/csv"))
                        .file(importFile))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void memberOrderCsvImportReadFailureTest() throws Exception {
        var generatedRequest = new MultiParentOrderRequest();
        when(csvTransformer.toRequest(any())).thenReturn(generatedRequest);

        doThrow(new IOException()).when(requestSender).sendToQueue(any());

        MockMultipartFile importFile = new MockMultipartFile("file",
                "some,csv,content".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/memberOrder/import/csv"))
                        .file(importFile))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    protected Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(), type);
    }
}
