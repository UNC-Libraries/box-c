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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.jdom2.Document;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLService;
import edu.unc.lib.boxc.operations.test.ModsTestHelper;

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
    private ImportXMLService service;
    @Autowired
    private JmsTemplate jmsTemplate;

    private File tempDir;

    @Before
    public void init_() throws Exception {
        reset(jmsTemplate);

        tempDir = tmpFolder.newFolder();
        service.setDataDir(tempDir.getAbsolutePath());
        service.setJmsTemplate(jmsTemplate);
        service.init();
    }

    @Test
    public void testImportMODS() throws Exception {

        MockMultipartFile importFile = createTempImportFile();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/importXML"))
                .file(importFile))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("import xml", respMap.get("action"));
        assertEquals("Import of metadata has begun. " + GroupsThreadStore.getEmail()
                + " will be emailed when the update completes", respMap.get("message"));
        assertTrue(respMap.containsKey("timestamp"));

        verify(jmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void testNullQueue() throws Exception {
        doThrow(new IllegalArgumentException()).when(jmsTemplate).send(any(MessageCreator.class));

        MockMultipartFile importFile = createTempImportFile();
        MvcResult result = mvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/importXML"))
                .file(importFile))
                .andExpect(status().is5xxServerError())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("import xml", respMap.get("action"));
        assertEquals(GroupsThreadStore.getUsername(), respMap.get("username"));
    }

    private MockMultipartFile createTempImportFile() throws Exception {
        Document updateDoc = ModsTestHelper.makeUpdateDocument();
        ModsTestHelper.addObjectUpdate(updateDoc, PIDs.get(UUID.randomUUID().toString()), null);

        MockMultipartFile importFile = new MockMultipartFile("file",
                ModsTestHelper.documentToInputStream(updateDoc));
        return importFile;
    }

}
