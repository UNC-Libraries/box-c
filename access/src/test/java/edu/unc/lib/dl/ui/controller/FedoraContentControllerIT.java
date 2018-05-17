/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.controller;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.TECHNICAL_METADATA;
import static edu.unc.lib.dl.test.TestHelper.makePid;
import static edu.unc.lib.dl.ui.service.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
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

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;

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

    protected MockMvc mvc;
    @Autowired
    protected WebApplicationContext context;

    @Before
    public void init() {

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername("test_user");
        GroupsThreadStore.storeGroups(new AccessGroupSet("adminGroup"));

    }

    @Test
    public void testGetDatastream() throws Exception {
        PID filePid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), "file.txt", "text/plain", null, null);

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
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), "file.txt", "text/plain", null, null);

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
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), null, "text/plain", null, null);

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
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), null, "text/plain", null, null);

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSet.class), eq(Permission.viewOriginal));

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

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), null, "text/plain", null, null);
        fileObj.addBinary(TECHNICAL_METADATA, new ByteArrayInputStream(content.getBytes()),
                "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get(requestPath + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(BINARY_CONTENT, result1.getResponse().getContentAsString());

        // Verify administrative datastream retrievable
        MvcResult result2 = mvc.perform(get(requestPath + filePid.getId() + "/" + TECHNICAL_METADATA))
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
        fileObj.addBinary(TECHNICAL_METADATA, new ByteArrayInputStream(content.getBytes()),
                "fits.xml", "application/xml", null, null, null);

        // Requires viewHidden permission
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSet.class), eq(Permission.viewHidden));

        // Verify administrative datastream retrievable
        MvcResult result = mvc.perform(get("/content/" + filePid.getId() + "/" + TECHNICAL_METADATA))
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
}
