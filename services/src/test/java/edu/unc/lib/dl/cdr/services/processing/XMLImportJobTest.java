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

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
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
    private String username = "username";
    private String userEmail = "user@email.com";
    @Mock
    private AgentPrincipals agent;
    private File importFile = new File("src/test/resources/mods/bulk-md.xml");
    @Mock
    private AccessControlService aclService;
    @Mock
    private Template completeTemplate;
    @Mock
    private Template failedTemplate;
    private String fromAddress = "admin@example.com";
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private UpdateDescriptionService updateService;

    @Mock
    private MimeMessage msg;
    @Mock
    private XMLReader xmlReader;

    @Before
    public void init() {
        initMocks(this);
        job = new XMLImportJob(username, userEmail, agent, importFile);
        job.setAclService(aclService);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress(fromAddress);
        job.setMailSender(mailSender);
        job.setRepoObjLoader(repoObjLoader);
        job.setUpdateService(updateService);

        when(mailSender.createMimeMessage()).thenReturn(msg);
        when(completeTemplate.execute(anyMap())).thenReturn("some text");
    }

    @Test
    public void fileNotFoundTest() {
        job.run();
    }

}
