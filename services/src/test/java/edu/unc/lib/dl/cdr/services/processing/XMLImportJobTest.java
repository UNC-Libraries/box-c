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
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
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
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Before
    public void init() {
        initMocks(this);

        when(mailSender.createMimeMessage()).thenReturn(msg);
        when(completeTemplate.execute(anyMap())).thenReturn("some text");
    }

    @Test
    public void fileNotFoundTest() throws Exception {
        String username = "username";
        String userEmail = "user@email.com";
        File importFile = new File("path/to/nowhere");
        setupJob(username, userEmail, importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());
        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals("path/to/nowhere", dataMap.get("fileName"));
        assertEquals(1, dataMap.get("problemCount"));
        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("CDR Metadata update failed", subjectCaptor.getValue());
    }

    @Test
    public void successfulJobTest() throws Exception {
        String username = "username";
        String userEmail = "user@email.com";
        File importFile = new File("src/test/resources/mods/bulk-md.xml");
        setupJob(username, userEmail, importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("CDR Metadata update completed: src/test/resources/mods/bulk-md.xml", subjectCaptor.getValue());
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
    }

}
