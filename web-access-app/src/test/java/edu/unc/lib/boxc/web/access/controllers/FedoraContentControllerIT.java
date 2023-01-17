package edu.unc.lib.boxc.web.access.controllers;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.auth.api.Permission;

/**
 *
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
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

    protected MockMvc mvc;
    @Autowired
    protected WebApplicationContext context;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("adminGroup"));

    }

    @Test
    public void testGetDatastream() throws Exception {
        PID filePid = makePid();

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

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), null, "text/plain", null, null);

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSetImpl.class), eq(Permission.viewOriginal));

        MvcResult result = mvc.perform(get("/content/" + filePid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("", response.getContentAsString(), "Must not return file content");
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

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(makeContentUri(BINARY_CONTENT), null, "text/plain", null, null);
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        fileObj.addBinary(fitsPid, makeContentUri(content), "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get(requestPath + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(content, result1.getResponse().getContentAsString());

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
        assertEquals("", response.getContentAsString(), "Must not return file content");
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
        assertEquals("", response.getContentAsString(), "Must not return file content");
    }

    @Test
    public void testGetContentNonFile() throws Exception {
        PID objPid = makePid();

        repositoryObjectFactory.createWorkObject(objPid, null);

        mvc.perform(get("/content/" + objPid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private URI makeContentUri(String content) throws Exception {
        File dataFile = tmpFolder.resolve("testFile").toFile();
        FileUtils.write(dataFile, content, "UTF-8");
        return dataFile.toPath().toUri();
    }
}
