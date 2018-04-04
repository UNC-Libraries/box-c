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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.acl.util.AgentPrincipals;

/**
 *
 * @author harring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/import-job-it.xml")
})
public class XMLImportJobIT {

    private XMLImportJob job;

    private String userEmail = "user@example.com";
    @Mock
    private AgentPrincipals agent;
    private File importFile;

    @Autowired
    private UpdateDescriptionService updateService;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private Template completeTemplate;
    @Mock
    private Template failedTemplate;
    private String fromAddress = "admin@example.com";
    @Mock MimeMessage mimeMsg;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private File tempDir;

    @Before
    public void init_() throws IOException {
        initMocks(this);

        tempDir = tmpFolder.newFolder();

        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
    }

    @Test
    public void test() throws IOException {
        importFile = createTempImportFile();
        createJob();
        job.run();

        verify(mailSender).send(any(MimeMessage.class));
    }

    private void createJob() {
        job = new XMLImportJob(userEmail, agent, importFile);
        job.setUpdateService(updateService);
        job.setMailSender(mailSender);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress(fromAddress);
        job.setMimeMessage(mimeMsg);
    }

    private File createTempImportFile() throws IOException {
        File tempImportFile = new File(tempDir, "temp-import");
        Files.copy(Paths.get("src/test/resources/mods/bulk-md.xml"), tempImportFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        File importFile = new File(tempImportFile.getPath());
        return importFile;
    }

}
