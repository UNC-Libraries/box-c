package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/fedora-content-it-servlet.xml")
})
public class FedoraContentControllerIT {

    private static final String BINARY_CONTENT = "binary content";

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private SolrQueryLayerService queryLayer;

    protected MockMvc mvc;
    @Autowired
    protected WebApplicationContext context;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("adminGroup"));

    }

    private ContentObjectRecord mockSolrRecord(PID pid) {
        var contentRecord = mock(ContentObjectRecord.class);
        when(contentRecord.getId()).thenReturn(pid.getId());
        when(queryLayer.getObjectById(any())).thenReturn(contentRecord);
        return contentRecord;
    }

    private Datastream makeDatastream() {
        return new DatastreamImpl(null, DatastreamType.ORIGINAL_FILE.getId(), 0l, "text/plain",
                "file.txt", "txt", null, null);
    }

    @Test
    public void testGetDatastream() throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(makeDatastream()));

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), "file.txt", "text/plain", null, null);

        MvcResult result = mvc.perform(get("/content/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());

        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("text/plain", response.getContentType());
        assertEquals("inline; filename=\"file.txt\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetDatastreamDownload() throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(makeDatastream()));

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), "file.txt", "text/plain", null, null);

        MvcResult result = mvc.perform(get("/content/" + filePid.getId())
                .param("dl", "true"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());

        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("text/plain", response.getContentType());
        assertEquals("attachment; filename=\"file.txt\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetDatastreamNoFilename() throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(makeDatastream()));

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), null, "text/plain", null, null);

        MvcResult result = mvc.perform(get("/content/" + filePid.getId())
                .param("dl", "true"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());

        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("text/plain", response.getContentType());
        assertEquals("attachment; filename=\"" + filePid.getId() + "\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetDatastreamInsufficientPermissions() throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(makeDatastream()));

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), null, "text/plain", null, null);

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(Permission.viewOriginal));

        MvcResult result = mvc.perform(get("/content/" + filePid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("Must not return file content", "", response.getContentAsString());
    }

    @Test
    public void testGetMultipleDatastreams() throws Exception {
        testGetMultipleDatastreams("/content/");
    }

    @Test
    public void testGetMultipleIndexableDatastreams() throws Exception {
        testGetMultipleDatastreams("/indexablecontent/");
    }

    private void testGetMultipleDatastreams(String requestPath) throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        var fitsDs = new DatastreamImpl(null, TECHNICAL_METADATA.getId(), 0l, "application/xml",
                "fits.xml", "xml", null, null);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(makeDatastream(), fitsDs));

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), null, "text/plain", null, null);
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        fileObj.addBinary(fitsPid, makeContentUri(content), "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get(requestPath + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(BINARY_CONTENT, result1.getResponse().getContentAsString());

        // Verify administrative datastream retrievable
        MvcResult result2 = mvc.perform(get(requestPath + filePid.getId() + "/" + TECHNICAL_METADATA.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result2.getResponse();
        assertEquals(content, response.getContentAsString());

        assertEquals(content.length(), response.getContentLength());
        assertEquals("application/xml", response.getContentType());
        assertEquals("inline; filename=\"fits.xml\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetAdministrativeDatastreamNoPermissions() throws Exception {
        PID filePid = makePid();
        var contentRecord = mockSolrRecord(filePid);
        var fitsDs = new DatastreamImpl(null, TECHNICAL_METADATA.getId(), 0l, "application/xml",
                "fits.xml", "xml", null, null);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(fitsDs));

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        fileObj.addBinary(fitsPid, makeContentUri(content), "fits.xml", "application/xml", null, null, null);

        // Requires viewHidden permission
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(Permission.viewHidden));

        // Verify administrative datastream retrievable
        MvcResult result = mvc.perform(get("/content/" + filePid.getId() + "/" + TECHNICAL_METADATA.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("Must not return file content", "", response.getContentAsString());
    }

    @Test
    public void testInvalidDatastream() throws Exception {
        PID filePid = makePid();

        repositoryObjectFactory.createFileObject(filePid, null);

        // Verify administrative datastream retrievable
        MvcResult result = mvc.perform(get("/content/" + filePid.getId() + "/some_ds"))
                .andExpect(status().isNotFound())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("Must not return file content", "", response.getContentAsString());
    }

    @Test
    public void testGetContentNonFile() throws Exception {
        PID objPid = makePid();

        repositoryObjectFactory.createWorkObject(objPid, null);

        mvc.perform(get("/content/" + objPid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGetDatastreamFromWork() throws Exception {
        PID workPid = makePid();
        var contentRecord = mockSolrRecord(workPid);
        PID filePid = makePid();
        var childOriginal = new DatastreamImpl(filePid.getId(), DatastreamType.ORIGINAL_FILE.getId(), 0l, "text/plain",
                "file.txt", "txt", null, null);
        when(contentRecord.getDatastreamObjects()).thenReturn(Arrays.asList(childOriginal));

        var workObj = repositoryObjectFactory.createWorkObject(workPid, null);
        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), "file.txt", "text/plain", null, null);
        workObj.addMember(fileObj);

        MvcResult result = mvc.perform(get("/content/" + workPid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());

        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("text/plain", response.getContentType());
        assertEquals("inline; filename=\"file.txt\"", response.getHeader(CONTENT_DISPOSITION));
    }

    private URI makeContentUri(String content) throws Exception {
        File dataFile = tmpFolder.newFile();
        FileUtils.write(dataFile, content, "UTF-8");
        return dataFile.toPath().toUri();
    }
}
