package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSender;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.hadoop.fs.DU;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.DURACLOUD;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.OPEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StreamingPropertiesIT {
    @InjectMocks
    private StreamingPropertiesController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private StreamingPropertiesRequestSender streamingPropertiesRequestSender;
    @Mock
    private WorkObject workObject;
    @Mock
    private FileObject fileObject;
    @Mock
    private Resource resource;
    @Mock
    private Statement filenameStatement;
    @Mock
    private Statement hostStatement;
    @Mock
    private Statement folderStatement;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String FILE_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID FILE_PID = PIDs.get(FILE_ID);
    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetStreamingPropertiesNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(FILE_PID), any(), eq(Permission.viewHidden));

        mockMvc.perform(get("/edit/streamingProperties/" + FILE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetStreamingPropertiesNotAFileObject() throws Exception {
        var workId = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(workId)))).thenReturn(workObject);

        mockMvc.perform(get("/edit/streamingProperties/" + workId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetStreamingPropertiesSuccess() throws Exception {
        var filename = "banjo-playlist.m3u8";
        when(repositoryObjectLoader.getRepositoryObject(eq(FILE_PID))).thenReturn(fileObject);
        when(fileObject.getResource()).thenReturn(resource);
        when(resource.getProperty(eq(Cdr.streamingFile))).thenReturn(filenameStatement);
        when(filenameStatement.getString()).thenReturn(filename);
        when(resource.getProperty(eq(Cdr.streamingFolder))).thenReturn(folderStatement);
        when(folderStatement.getString()).thenReturn(OPEN);
        when(resource.getProperty(eq(Cdr.streamingHost))).thenReturn(hostStatement);
        when(hostStatement.getString()).thenReturn(DURACLOUD);

        var result = mockMvc.perform(get("/edit/streamingProperties/" + FILE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(filename, respMap.get("filename"));
        assertEquals(OPEN, respMap.get("folder"));
        assertEquals(DURACLOUD, respMap.get("host"));
    }

    @Test
    public void testUpdateStreamingPropertiesNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(FILE_PID), any(), eq(Permission.ingest));
        mockMvc.perform(put(
                "/edit/streamingProperties?action=add&filename=banjo_sounds.mp3&id=" + FILE_ID + "&folder=" + OPEN))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateStreamingPropertiesNoFilename() throws Exception {
        mockMvc.perform(put(
                "/edit/streamingProperties?action=add&id=" + FILE_ID + "&folder=" + OPEN))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testUpdateStreamingPropertiesNoFolder() throws Exception {
        mockMvc.perform(put(
                        "/edit/streamingProperties?action=add&filename=banjo_sounds.mp3&id=" + FILE_ID))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testUpdateStreamingPropertiesNoFileId() throws Exception {
        mockMvc.perform(put(
                        "/edit/streamingProperties?action=add&filename=banjo_sounds.mp3&folder=" + OPEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateStreamingPropertiesNoAction() throws Exception {
        mockMvc.perform(put(
                        "/edit/streamingProperties?&filename=banjo_sounds.mp3&id=" + FILE_ID + "&folder=" + OPEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddStreamingPropertiesSuccess() throws Exception {
        var result = mockMvc.perform(put(
                        "/edit/streamingProperties?action=add&filename=banjo_sounds.mp3&id=" + FILE_ID + "&folder=" + OPEN))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Submitted streaming properties updates for " + FILE_ID, respMap.get("status"));
    }

    @Test
    public void testDeleteStreamingPropertiesSuccess() throws Exception {
        var result = mockMvc.perform(put(
                        "/edit/streamingProperties?action=delete&id=" + FILE_ID ))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Submitted streaming properties updates for " + FILE_ID, respMap.get("status"));
    }
}
