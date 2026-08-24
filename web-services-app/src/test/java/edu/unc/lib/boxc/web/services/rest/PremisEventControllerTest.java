package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PremisEventControllerTest {
    private static final String OBJECT_ID = "e2847b41-e0ee-45bb-bdb3-a97a6241bee5";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private FileObject fileObject;
    @Mock
    private PremisLog premisLog;
    @Mock
    private Model eventsModel;
    @Mock
    private Resource resource;
    @InjectMocks
    private PremisEventController controller;
    private MockMvc mockMvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        when(fileObject.getPremisLog()).thenReturn(premisLog);
        when(premisLog.getEventsModel()).thenReturn(eventsModel);
        when(eventsModel.getResource(any(String.class))).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void getPremisEventsNoPermissionTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(any(), eq(OBJECT_PID), any(), eq(Permission.viewMetadata));

        mockMvc.perform(get("/premisEvents/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

    }

    @Test
    public void getPremisEventsNotAFileObjectTest() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(OBJECT_PID)))
                .thenReturn(mock(FolderObject.class));

        mockMvc.perform(get("/premisEvents/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getPremisEventsNoApplicableEventsTest() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(OBJECT_PID))).thenReturn(fileObject);
        var inputStream = new FileInputStream("src/test/resources/rdf/premis-events-non-public.rdf");
        var model = ModelFactory.createDefaultModel();
        var readModel = model.read(inputStream, null, Lang.NTRIPLES.getName());
        when(premisLog.getEventsModel()).thenReturn(readModel);

        var result = mockMvc.perform(get("/premisEvents/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        var respJson = MvcTestHelpers.getResponseAsJson(result);
        assertTrue(respJson.isEmpty());
    }

    @Test
    public void getPremisEventsSuccessTest() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(OBJECT_PID))).thenReturn(fileObject);
        var inputStream = new FileInputStream("src/test/resources/rdf/premis-events.rdf");
        var model = ModelFactory.createDefaultModel();
        var readModel = model.read(inputStream, null, Lang.NTRIPLES.getName());
        when(premisLog.getEventsModel()).thenReturn(readModel);

        var result = mockMvc.perform(get("/premisEvents/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var firstNode = respJson.get(0);
        var lastNode = respJson.get(4);

        assertEquals(5, respJson.size());
        assertTrue(getDateTime(firstNode.get("timestamp").textValue())
                .isBefore(getDateTime(lastNode.get("timestamp").textValue())));
        assertEquals("Object migrated as a part of the CONTENTdm to Box-c 5 migration", firstNode.get("note").textValue());
        assertEquals("http://example.com/rest/agents/person/onyen/bbpennel", firstNode.get("username").textValue());
        assertEquals("original_file restored to previous version dated 2026-08-19T20:46:47.006Z", lastNode.get("note").textValue());
    }

    private DateTime getDateTime(String dateString) {
        return DateTime.parse(dateString);
    }
}
