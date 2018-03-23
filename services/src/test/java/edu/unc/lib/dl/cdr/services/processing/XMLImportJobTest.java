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
package edu.unc.lib.dl.cdr.services.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.xml.sax.XMLReader;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;

/**
 *
 * @author harring
 *
 */
public class XMLImportJobTest {

    private XMLImportJob job;

    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessControlService aclService;
    @Mock
    private Template completeTemplate;
    @Mock
    private Template failedTemplate;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private UpdateDescriptionService updateService;

    private String fromAddress = "admin@example.com";

    @Mock
    private MimeMessage msg;
    @Mock
    private MimeMessageHelper msgHelper;
    @Mock
    private XMLReader xmlReader;

    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> emailCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void init() throws Exception {
        initMocks(this);

        when(mailSender.createMimeMessage()).thenReturn(msg);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyFileTest() throws Exception {
        String username = "username";
        String userEmail = "user@email.com";
        File importFile = tmpFolder.newFile();
        setupJob(username, userEmail, importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());

        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(importFile.getAbsolutePath(), dataMap.get("fileName"));
        assertEquals(1, dataMap.get("problemCount"));
        Set<Entry<String, String>> problems = (Set<Entry<String, String>>) dataMap.get("problems");
        Entry<String, String> problem = problems.iterator().next();
        assertEquals(problem.getValue(), "The import file contains XML errors");

        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("CDR Metadata update failed", subjectCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fileNotFoundTest() throws Exception {
        String username = "username";
        String userEmail = "user@email.com";
        File importFile = new File("");
        setupJob(username, userEmail, importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());

        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(1, dataMap.get("problemCount"));
        Set<Entry<String, String>> problems = (Set<Entry<String, String>>) dataMap.get("problems");
        Entry<String, String> problem = problems.iterator().next();
        assertEquals(problem.getValue(), "Failed to read metadata update package for " + username);

        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("CDR Metadata update failed", subjectCaptor.getValue());
    }

    @Test
    public void successfulJobTest() throws Exception {
        String username = "username";
        String userEmail = "user@email.com";
        File tempImportFile = createTempImportFile();
        setupJob(username, userEmail, tempImportFile);
        when(completeTemplate.execute(anyObject())).thenReturn("success email text");
        job.run();

        verify(mailSender).send(msg);
        verify(completeTemplate).execute(mapCaptor.capture());
        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(tempImportFile.getPath(), dataMap.get("fileName"));
        assertNull(dataMap.get("failed"));
        assertNull(dataMap.get("failedCount"));
        assertNull(dataMap.get("issues"));
        assertNotNull(dataMap.get("updated"));
        assertNotNull(dataMap.get("updatedCount"));
        verify(msg).setSubject(subjectCaptor.capture());
        assertTrue(subjectCaptor.getValue().startsWith("CDR Metadata update completed"));
    }

    // creates copy of import file to avoid test file being deleted from project
    private File createTempImportFile() throws IOException {
        File tempDir = tmpFolder.newFolder();
        File tempImportFile = new File(tempDir, "temp-mods-import");
        Files.copy(Paths.get("src/test/resources/mods/bulk-md.xml"), tempImportFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        return tempImportFile;
    }

    private void setupJob(String username, String userEmail, File importFile) {
        job = new XMLImportJob(username, userEmail, agent, importFile);
        job.setAclService(aclService);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress(fromAddress);
        job.setMailSender(mailSender);
        job.setRepoObjLoader(repoObjLoader);
        job.setUpdateService(updateService);
        job.setMimeMessage(msg);
        job.setMessageHelper(msgHelper);
        job.setFromAddress("cdrAdmin@example.org");
    }

}
