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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.model.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.dl.ui.service.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
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
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.ui.service.DerivativeContentService;
import edu.unc.lib.dl.util.DerivativeService;

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
    @ContextConfiguration("/datastream-content-it-servlet.xml")
})
public class DatastreamRestControllerIT extends AbstractAPIIT {

    private static final String BINARY_CONTENT = "binary content";

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;

    @Autowired
    private DerivativeContentService derivativeContentService;

    @Rule
    public TemporaryFolder derivDir = new TemporaryFolder();
    private String derivDirPath;

    @Before
    public void setup() {
        derivDirPath = derivDir.getRoot().getAbsolutePath();

        DerivativeService derivService = new DerivativeService();
        derivService.setDerivativeDir(derivDirPath);

        derivativeContentService.setDerivativeService(derivService);
    }

    @Test
    public void testGetFile() throws Exception {
        PID filePid = makePid();

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), "file.txt", "text/plain", null, null);

        MvcResult result = mvc.perform(get("/file/" + filePid.getId()))
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
    public void testGetMultipleDatastreams() throws Exception {
        PID filePid = makePid();

        String content = "<fits>content</fits>";

        FileObject fileObj = repositoryObjectFactory.createFileObject(filePid, null);
        fileObj.addOriginalFile(new ByteArrayInputStream(BINARY_CONTENT.getBytes()), null, "text/plain", null, null);
        fileObj.addBinary(TECHNICAL_METADATA.getId(), new ByteArrayInputStream(content.getBytes()),
                "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get("/file/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(BINARY_CONTENT, result1.getResponse().getContentAsString());

        // Verify administrative datastream retrievable
        MvcResult result2 = mvc.perform(get("/file/" + filePid.getId() + "/" + TECHNICAL_METADATA.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result2.getResponse();
        assertEquals(content, response.getContentAsString());

        assertEquals(content.length(), response.getContentLength());
        assertEquals("application/xml", response.getContentType());
        assertEquals("inline; filename=\"fits.xml\"", response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnail() throws Exception {
        PID filePid = makePid();
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        MvcResult result = mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + THUMBNAIL_SMALL.getExtension() + "\"",
                response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetInvalidThumbnailSize() throws Exception {
        PID filePid = makePid();
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        mvc.perform(get("/thumb/" + filePid.getId() + "/megasize"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGetThumbnailNoPermission() throws Exception {
        PID filePid = makePid();
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(filePid), any(AccessGroupSet.class), eq(viewMetadata));

        MvcResult result = mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertEquals("Content must not be returned", "", response.getContentAsString());
    }

    @Test
    public void testGetFileDerivative() throws Exception {
        PID filePid = makePid();
        String id = filePid.getId();
        createDerivative(id, THUMBNAIL_SMALL, BINARY_CONTENT.getBytes());

        MvcResult result = mvc.perform(get("/file/" + filePid.getId() + "/" + THUMBNAIL_SMALL.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify content was retrieved
        MockHttpServletResponse response = result.getResponse();
        assertEquals(BINARY_CONTENT, response.getContentAsString());
        assertEquals(BINARY_CONTENT.length(), response.getContentLength());
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=\"" + id + "." + THUMBNAIL_SMALL.getExtension() + "\"",
                response.getHeader(CONTENT_DISPOSITION));
    }

    @Test
    public void testGetThumbnailNotPresent() throws Exception {
        PID filePid = makePid();

        mvc.perform(get("/thumb/" + filePid.getId()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private File createDerivative(String id, DatastreamType dsType, byte[] content) throws Exception {
        String hashedPath = idToPath(id, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path derivPath = Paths.get(derivDirPath, dsType.getId(), hashedPath,
                id + "." + dsType.getExtension());

        File derivFile = derivPath.toFile();
        derivFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(derivFile, content);

        return derivFile;
    }
}
