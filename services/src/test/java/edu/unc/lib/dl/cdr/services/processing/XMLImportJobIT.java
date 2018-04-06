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

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.mail.internet.MimeMessage;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.tika.io.IOUtils;
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
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;

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
    @Mock
    private MimeMessage mimeMsg;

    private String fromAddress = "admin@example.com";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private File tempDir;

    @Autowired
    private RepositoryObjectFactory factory;
    @Autowired
    private RepositoryObjectLoader objLoader;
    private WorkObject workObj;

    @Before
    public void init_() throws IOException {
        initMocks(this);

        tempDir = tmpFolder.newFolder();

        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
        when(completeTemplate.execute(any(Object.class))).thenReturn("update was successful");

        TestHelper.setContentBase("http://localhost:48085/rest/");
    }

    @Test
    public void test() throws IOException {
        populateFedora();
        InputStream originalMods = workObj.getMODS().getBinaryStream();
        importFile = createTempImportFile();
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));

        InputStream updatedMods = objLoader.getWorkObject(workObj.getPid()).getMODS().getBinaryStream();
        assertFalse(IOUtils.contentEquals(originalMods, updatedMods));
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
        Files.copy(Paths.get("src/test/resources/mods/update-work-mods.xml"), tempImportFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        File importFile = new File(tempImportFile.getPath());
        return importFile;
    }

    private void populateFedora() throws FileNotFoundException {
        Model model = ModelFactory.createDefaultModel();
        PID workPid = PIDs.get("uuid:ae0091e0-192d-46f9-a8ad-8b0dc82f33ad");
        workObj = factory.createWorkObject(workPid, model);
        workObj.setDescription(new FileInputStream(new File("src/test/resources/mods/work-mods.xml")));
    }

}
