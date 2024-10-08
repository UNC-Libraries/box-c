package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Map;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequestSender;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.web.services.processing.ImportThumbnailService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author lfarrell
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class ThumbnailIT extends AbstractAPIIT {
    private CollectionObject collection;
    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;
    @Captor
    private ArgumentCaptor<ImportThumbnailRequest> importRequestCaptor;
    @Captor
    private ArgumentCaptor<ThumbnailRequest> requestCaptor;
    private ImportThumbnailService service;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private ThumbnailRequestSender thumbnailRequestSender;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @InjectMocks
    private ThumbnailController controller;
    private File tempDir;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        tempDir = tmpFolder.toFile();
        aclService = mock(AccessControlService.class);
        service = new ImportThumbnailService();
        service.setAclService(aclService);
        service.setMessageSender(thumbnailRequestSender);
        service.setSourceImagesDir(tempDir.getAbsolutePath());
        service.init();
        controller.setImportThumbnailService(service);
        controller.setAclService(aclService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        collection = repositoryObjectFactory.createCollectionObject(null);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void addEditThumbnail() throws Exception {
        FileInputStream input = new FileInputStream("src/test/resources/upload-files/burndown.png");
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "burndown.png", "image/png", IOUtils.toByteArray(input));
        var id = collection.getPid().getUUID();

        mvc.perform(MockMvcRequestBuilders.multipart("/edit/displayThumbnail/" + id)
                .file(thumbnailFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(thumbnailRequestSender).sendToImportQueue(importRequestCaptor.capture());
        var request = importRequestCaptor.getValue();
        assertEquals(id, request.getPidString());
        assertEquals("image/png", request.getMimetype());
    }

    @Test
    public void addCollectionThumbWrongFileType() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "file.txt", "plain/text", textStream());

        mvc.perform(MockMvcRequestBuilders.multipart("/edit/displayThumbnail/" + collection.getPid().getUUID())
                .file(thumbnailFile))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(thumbnailRequestSender, never()).sendToImportQueue(any(ImportThumbnailRequest.class));
    }

    @Test
    public void addCollectionThumbNoAccess() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile("file", "file.txt", "plain/text", textStream());

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(collection.getPid()), any(AccessGroupSetImpl.class), eq(editDescription));

        mvc.perform(MockMvcRequestBuilders.multipart("/edit/displayThumbnail/" + collection.getPid().getUUID())
                .file(thumbnailFile))
                .andExpect(status().isForbidden())
                .andReturn();

        verify(thumbnailRequestSender, never()).sendToImportQueue(any(ImportThumbnailRequest.class));
    }

    @Test
    public void assignThumbnailSuccess() throws Exception {
        var pid = makePid();
        var filePidString = pid.getId();
        var file = repositoryObjectFactory.createFileObject(pid, null);
        var work = repositoryObjectFactory.createWorkObject(makePid(), null);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(file);
        work.addMember(file);

        MvcResult result = mvc.perform(put("/edit/assignThumbnail/" + filePidString))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(thumbnailRequestSender).sendToQueue(requestCaptor.capture());
        ThumbnailRequest request = requestCaptor.getValue();
        assertEquals(filePidString, request.getFilePidString());

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("assignThumbnail", respMap.get(ThumbnailController.ACTION));
        assertEquals(filePidString, respMap.get("newThumbnailId"));
    }

    @Test
    public void assignThumbnailNoAccess() throws Exception {
        var pid = makePid();
        var filePidString = pid.getId();
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(editDescription));

        mvc.perform(put("/edit/assignThumbnail/" + filePidString))
                .andExpect(status().isForbidden())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void assignThumbnailInvalidPidString() throws Exception {
        var badPidString = "NotAPid";
        mvc.perform(put("/edit/assignThumbnail/" + badPidString))
                .andExpect(status().is4xxClientError())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void assignThumbnailPidIsAWorkWithPreviousAssignedThumbnail() throws Exception {
        var pid = makePid();
        var filePidString = pid.getId();
        var file = repositoryObjectFactory.createFileObject(pid, null);
        var oldThumbnailPid = makePid();
        var oldThumbnail = repositoryObjectFactory.createFileObject(oldThumbnailPid, null);
        var workPid = makePid();
        var work = repositoryObjectFactory.createWorkObject(workPid, null);
        work.addMember(file);
        work.addMember(oldThumbnail);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(file);
        repositoryObjectFactory.createExclusiveRelationship(work, Cdr.useAsThumbnail, oldThumbnail.getResource());

        MvcResult result = mvc.perform(put("/edit/assignThumbnail/" + filePidString))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        verify(thumbnailRequestSender).sendToQueue(requestCaptor.capture());
        ThumbnailRequest request = requestCaptor.getValue();
        assertEquals(filePidString, request.getFilePidString());

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("assignThumbnail", respMap.get(ThumbnailController.ACTION));
        assertEquals(filePidString, respMap.get("newThumbnailId"));
        assertEquals(oldThumbnailPid.getId(), respMap.get(ThumbnailController.OLD_THUMBNAIL_ID));
    }

    @Test
    public void assignThumbnailPidIsNotAFileOrWork() throws Exception {
        var pid = makePid();
        var folderPidString = pid.getId();
        var folder = repositoryObjectFactory.createFolderObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(folder);

        mvc.perform(put("/edit/assignThumbnail/" + folderPidString))
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void deleteThumbnailSuccess() throws Exception {
        var pid = makePid();
        var filePidString = pid.getId();
        var file = repositoryObjectFactory.createFileObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(file);
        MvcResult result = mvc.perform(delete("/edit/deleteThumbnail/" + filePidString))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(thumbnailRequestSender).sendToQueue(requestCaptor.capture());
        ThumbnailRequest request = requestCaptor.getValue();
        assertEquals(filePidString, request.getFilePidString());
        assertEquals(ThumbnailRequest.DELETE, request.getAction());

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("deleteThumbnail", respMap.get(ThumbnailController.ACTION));
        assertEquals(filePidString, respMap.get(ThumbnailController.OLD_THUMBNAIL_ID));
    }

    @Test
    public void deleteThumbnailNoAccess() throws Exception {
        var pid = makePid();
        var filePidString = pid.getId();

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(editDescription));

        mvc.perform(delete("/edit/deleteThumbnail/" + filePidString))
                .andExpect(status().isForbidden())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void deleteThumbnailPidIsAWork() throws Exception {
        var pid = makePid();
        var workPidString = pid.getId();
        var work = repositoryObjectFactory.createWorkObject(pid, null);
        var filePid = makePid();
        var file = repositoryObjectFactory.createFileObject(filePid, null);
        work.addMember(file);
        repositoryObjectFactory.createExclusiveRelationship(work, Cdr.useAsThumbnail, file.getResource());
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(work);

        mvc.perform(delete("/edit/deleteThumbnail/" + workPidString))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        verify(thumbnailRequestSender).sendToQueue(requestCaptor.capture());
        ThumbnailRequest request = requestCaptor.getValue();
        assertEquals(filePid.getId(), request.getFilePidString());
        assertEquals(ThumbnailRequest.DELETE, request.getAction());
    }

    @Test
    public void deleteThumbnailPidIsAWorkButNoAssignedThumbnail() throws Exception {
        var pid = makePid();
        var workPidString = pid.getId();
        var work = repositoryObjectFactory.createWorkObject(pid, null);
        var filePid = makePid();
        var file = repositoryObjectFactory.createFileObject(filePid, null);
        work.addMember(file);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(work);

        mvc.perform(delete("/edit/deleteThumbnail/" + workPidString))
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    @Test
    public void deleteThumbnailPidIsNotAFileOrWork() throws Exception {
        var pid = makePid();
        var folderPidString = pid.getId();
        var folder = repositoryObjectFactory.createFolderObject(pid, null);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(folder);

        mvc.perform(delete("/edit/deleteThumbnail/" + folderPidString))
                .andExpect(status().isBadRequest())
                .andReturn();
        verify(thumbnailRequestSender, never()).sendMessage(any(Document.class));
    }

    private byte[] textStream() {
        return "I am not an image".getBytes();
    }

}
