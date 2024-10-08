package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.AccessControlServiceImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.processing.RunEnhancementsService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static edu.unc.lib.boxc.auth.api.Permission.runEnhancements;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author lfarrell
 *
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class RunEnhancementsIT extends AbstractAPIIT {
    private static final String BINARY_CONTENT = "binary content";
    private static final String USER_NAME = "user";
    private static final String ADMIN_GROUP = "adminGroup";

    private AutoCloseable closeable;

    @Mock
    private AccessControlServiceImpl aclService;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private MessageSender messageSender;
    @Mock
    private RunEnhancementsService runEnhancementsService;
    @InjectMocks
    private RunEnhancementsController controller;

    @Captor
    private ArgumentCaptor<Document> docCaptor;

    private SearchResultResponse results;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void initLocal() {
        closeable = openMocks(this);
        runEnhancementsService = new RunEnhancementsService();
        runEnhancementsService.setMessageSender(messageSender);
        runEnhancementsService.setAclService(aclService);
        runEnhancementsService.setQueryLayer(queryLayer);
        runEnhancementsService.setRepositoryObjectLoader(repositoryObjectLoader);
        controller.setEnhancementService(runEnhancementsService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        AccessGroupSet testPrincipals = new AccessGroupSetImpl(ADMIN_GROUP);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);

        // Non file
        results = mock(SearchResultResponse.class);
        when(queryLayer.performSearch(any(SearchRequest.class))).thenReturn(results);

        setupContentRoot();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void runEnhancementsFileObject() throws Exception {
        FileObject fileObj = repositoryObjectFactory.createFileObject(null);
        PID filePid = fileObj.getPid();
        fileObj.addOriginalFile(makeContentUri(filePid, BINARY_CONTENT), "file.png", "image/png", null, null);
        setResultMetadataObject(filePid, ResourceType.File.name());

        MvcResult result = mvc.perform(post("/runEnhancements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"force\":false,\"pids\":[\"" + filePid.getId() + "\"]}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertResponseSuccess(result);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        assertMessageValues(msgDoc, fileObj.getOriginalFile().getPid(), USER_NAME);
    }

    @Test
    public void runEnhancementsWorkObject() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);

        PID filePid = TestHelper.makePid();
        FileObject workFile = workObj
                .addDataFile(filePid, makeContentUri(filePid, BINARY_CONTENT), "file.png", "image/png", null, null, null);
        PID workPid = workObj.getPid();
        setResultMetadataObject(workPid, ResourceType.Work.name());

        MvcResult result = mvc.perform(post("/runEnhancements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"force\":false,\"recursive\":true,\"pids\":[\"" + workFile.getPid().getId() + "\"]}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertResponseSuccess(result);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        assertMessageValues(msgDoc, workPid, USER_NAME);
    }

    @Test
    public void runEnhancementsNonFileNonWorkObject() throws Exception {
        FolderObject folderObj = repositoryObjectFactory.createFolderObject(null);
        PID filePid = TestHelper.makePid();
        FileObject fileObj = folderObj.addWork()
                .addDataFile(filePid, makeContentUri(filePid, BINARY_CONTENT), "file.png", "image/png", null, null, null);
        setResultMetadataObject(filePid, ResourceType.Folder.name());
        System.out.println("fileObj.getPid() = " + fileObj.getPid());
        System.out.println("filePid = " + filePid);
        System.out.println("folder.getPid() = " + folderObj.getPid());

        MvcResult result = mvc.perform(post("/runEnhancements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"force\":false,\"recursive\":true,\"pids\":[\"" + filePid.getId() + "\"]}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertResponseSuccess(result);

        verify(messageSender).sendMessage(docCaptor.capture());
        Document msgDoc = docCaptor.getValue();
        assertMessageValues(msgDoc, fileObj.getPid(), USER_NAME);
    }

    @Test
    public void runEnhancementsNoAccess() throws Exception {
        FileObject fileObj = repositoryObjectFactory.createFileObject(null);
        fileObj.addOriginalFile(makeContentUri(fileObj.getPid(), BINARY_CONTENT), "file.png", "image/png", null, null);
        setResultMetadataObject(fileObj.getPid(), ResourceType.File.name());

        PID objPid = fileObj.getPid();
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(objPid), any(AccessGroupSetImpl.class), eq(runEnhancements));

        mvc.perform(post("/runEnhancements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"force\":false,\"recursive\":true,\"pids\":[\"" + objPid.toString() + "\"]}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        verify(messageSender, never()).sendMessage(any(Document.class));
    }

    private void assertResponseSuccess(MvcResult mvcResult) throws Exception {
        Map<String, Object> resp = MvcTestHelpers.getMapFromResponse(mvcResult);
        assertTrue(resp.containsKey("message"), "Missing run enhancements message");
        assertEquals("runEnhancements", resp.get("action"));
    }

    private URI makeContentUri(PID filePid, String content) throws Exception {
        var originalPid = DatastreamPids.getOriginalFilePid(filePid);
        var uri = storageLocationTestHelper.getTestStorageLocation().getNewStorageUri(originalPid);
        var path = Path.of(uri);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes());
        return uri;
    }

    private void setResultMetadataObject(PID pid, String resourceType) {
        ContentObjectSolrRecord md = new ContentObjectSolrRecord();
        md.setId(pid.getId());
        md.setDatastream(asList("original_file|image/png|small|png|3333||"));
        md.setResourceType(resourceType);

        when(results.getResultList()).thenReturn(Arrays.asList(md));

        when(queryLayer.getObjectById(any(SimpleIdRequest.class))).thenReturn(md);
    }

    private void assertMessageValues(Document msgDoc, PID expectedPid, String expectedAuthor) {
        Element entry = msgDoc.getRootElement();
        String pidString = entry.getChild(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS)
                .getChildText("pid", CDR_MESSAGE_NS);
        String author = entry.getChild("author", ATOM_NS)
                             .getChildText("name", ATOM_NS);

        assertEquals(expectedPid, PIDs.get(pidString));
        assertEquals(expectedAuthor, author);
    }
}
