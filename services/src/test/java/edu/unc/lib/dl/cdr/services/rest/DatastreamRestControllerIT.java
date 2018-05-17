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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.TECHNICAL_METADATA;
import static edu.unc.lib.dl.ui.service.FedoraContentService.CONTENT_DISPOSITION;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;

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
    private RepositoryObjectFactory repositoryObjectFactory;

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
        fileObj.addBinary(TECHNICAL_METADATA, new ByteArrayInputStream(content.getBytes()),
                "fits.xml", "application/xml", null, null, null);

        // Verify original file content retrievable
        MvcResult result1 = mvc.perform(get("/file/" + filePid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(BINARY_CONTENT, result1.getResponse().getContentAsString());

        // Verify administrative datastream retrievable
        MvcResult result2 = mvc.perform(get("/file/" + filePid.getId() + "/" + TECHNICAL_METADATA))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result2.getResponse();
        assertEquals(content, response.getContentAsString());

        assertEquals(content.length(), response.getContentLength());
        assertEquals("application/xml", response.getContentType());
        assertEquals("inline; filename=\"fits.xml\"", response.getHeader(CONTENT_DISPOSITION));
    }
}
