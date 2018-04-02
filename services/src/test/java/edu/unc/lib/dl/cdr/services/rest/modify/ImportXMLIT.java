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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.processing.XMLImportService;
import net.greghaines.jesque.Job;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/import-xml-it-servlet.xml")
})
public class ImportXMLIT extends AbstractAPIIT {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Autowired
    private XMLImportService service;

    private File tempDir;

    @Before
    public void init_() throws Exception {
        tempDir = tmpFolder.newFolder();
        service.setDataDir(tempDir.getAbsolutePath());
        service.init();
    }

    @Test
    public void testImportMODS() throws Exception {

        MockMultipartFile importFile = createTempImportFile();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.fileUpload(URI.create("/edit/importXML"))
                .file(importFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("import xml", respMap.get("action"));
        assertEquals(GroupsThreadStore.getUsername(), respMap.get("username"));
        assertEquals("Import of metadata has begun. " + GroupsThreadStore.getEmail()
                + " will be emailed when the update completes", respMap.get("message"));
        assertTrue(respMap.containsKey("timestamp"));

        verify(service.getClient()).enqueue(anyString(), any(Job.class));
    }

    @Test
    public void testNullQueue() throws Exception {

        MockMultipartFile importFile = createTempImportFile();

        doThrow( new IllegalArgumentException()).when(service.getClient()).enqueue(anyString(), any(Job.class));

        MvcResult result = mvc.perform(MockMvcRequestBuilders.fileUpload(URI.create("/edit/importXML"))
                .file(importFile))
                .andExpect(status().is5xxServerError())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("import xml", respMap.get("action"));
        assertEquals(GroupsThreadStore.getUsername(), respMap.get("username"));
    }

    private MockMultipartFile createTempImportFile() throws IOException {
        File tempImportFile = new File(tempDir, "temp-import");
        Files.copy(Paths.get("src/test/resources/mods/bulk-md.xml"), tempImportFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        MockMultipartFile importFile = new MockMultipartFile("file",
                Files.newInputStream(Paths.get(tempImportFile.getPath())));
        return importFile;
    }

}
