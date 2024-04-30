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
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSender;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.SET;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.DELETE;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessSurrogateIT {
    @InjectMocks
    private AccessSurrogateController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private FileObject fileObject;
    @Mock
    private WorkObject workObject;
    @Mock
    private MessageSender messageSender;
    @Mock
    private AccessSurrogateRequestSender accessSurrogateRequestSender;
    @Captor
    private ArgumentCaptor<AccessSurrogateRequest> requestCaptor;
    @TempDir
    public Path tmpFolder;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    private PID filePid;
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private final static String FILE_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        filePid = PIDs.get(FILE_ID);
        when(repositoryObjectLoader.getRepositoryObject(eq(filePid))).thenReturn(fileObject);
        controller.setAccessSurrogateTempPath(tmpFolder);
        controller.setAccessSurrogateRequestSender(accessSurrogateRequestSender);
        controller.setAclService(accessControlService);
        controller.setRepositoryObjectLoader(repositoryObjectLoader);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testSetAccessSurrogateNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(), eq(Permission.editDescription));
        FileInputStream input = new FileInputStream("src/test/resources/upload-files/burndown.png");
        MockMultipartFile surrogateFile = new MockMultipartFile("file", "burndown.png", "image/png", IOUtils.toByteArray(input));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/accessSurrogate/" + FILE_ID)
                .file(surrogateFile))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testSetAccessSurrogateWrongSurrogateFileType() throws Exception {
        MockMultipartFile surrogateFile = new MockMultipartFile("file", "file.txt", "plain/text", textStream());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/accessSurrogate/" + FILE_ID)
                        .file(surrogateFile))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void testSetAccessSurrogateWrongRepositoryObjectType() throws Exception {
        var workPid = makePid();
        when(repositoryObjectLoader.getRepositoryObject(eq(workPid))).thenReturn(workObject);
        FileInputStream input = new FileInputStream("src/test/resources/upload-files/burndown.png");
        MockMultipartFile surrogateFile = new MockMultipartFile("file", "burndown.png", "image/png", IOUtils.toByteArray(input));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/accessSurrogate/" + workPid)
                        .file(surrogateFile))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void testSetAccessSurrogateSuccess() throws Exception {
        FileInputStream input = new FileInputStream("src/test/resources/upload-files/burndown.png");
        MockMultipartFile surrogateFile = new MockMultipartFile("file", "burndown.png", "image/png", IOUtils.toByteArray(input));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/accessSurrogate/" + FILE_ID)
                        .file(surrogateFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(accessSurrogateRequestSender).sendToQueue(requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(filePid.getId(), request.getPidString());
        assertEquals(SET, request.getAction());
    }

    @Test
    public void testDeleteAccessSurrogateNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(), eq(Permission.editDescription));
        mockMvc.perform(delete("/edit/accessSurrogate/" + FILE_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testDeleteAccessSurrogateWrongRepositoryObjectType() throws Exception {
        var workPid = makePid();
        when(repositoryObjectLoader.getRepositoryObject(eq(workPid))).thenReturn(workObject);
        mockMvc.perform(delete("/edit/accessSurrogate/" + workPid))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void testDeleteAccessSurrogateSuccess() throws Exception {
        mockMvc.perform(delete("/edit/accessSurrogate/" + FILE_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(accessSurrogateRequestSender).sendToQueue(requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(filePid.getId(), request.getPidString());
        assertEquals(DELETE, request.getAction());
    }

    private byte[] textStream() {
        return "I am not an image".getBytes();
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
