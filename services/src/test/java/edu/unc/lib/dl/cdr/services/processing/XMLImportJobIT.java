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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

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
    public void testMODSGetsUpdated() throws Exception {
        populateFedora("uuid:ae0091e0-192d-46f9-a8ad-8b0dc82f33ad");
        InputStream originalMods = workObj.getMODS().getBinaryStream();
        assertModsNotUpdated(originalMods);
        importFile = createTempImportFile("src/test/resources/mods/update-work-mods.xml");
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));

        InputStream updatedMods = workObj.getMODS().getBinaryStream();

        assertModsUpdated(updatedMods);
    }

    @Test
    public void testTwoWorksOneGetsUpdated() throws Exception {
        populateFedora("uuid:ae0091e0-192d-57a0-a8ad-8b0dc82f33ad");
        // create a second work obj in Fedora and add mods to it
        PID anotherWorkPid = PIDs.get("uuid:bf0091e0-192d-57a0-a8ad-8b0dc82f33be");
        WorkObject anotherWorkObj = factory.createWorkObject(anotherWorkPid, null);
        anotherWorkObj.setDescription(new FileInputStream(new File("src/test/resources/mods/work-mods.xml")));

        InputStream originalMods = workObj.getMODS().getBinaryStream();
        InputStream anotherOriginalMods = anotherWorkObj.getMODS().getBinaryStream();
        assertModsNotUpdated(originalMods);
        assertModsNotUpdated(anotherOriginalMods);

        importFile = createTempImportFile("src/test/resources/mods/two-works-missing-pid-mods.xml");
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        InputStream updatedMods = workObj.getMODS().getBinaryStream();
        InputStream anotherUpdatedMods = anotherWorkObj.getMODS().getBinaryStream();
        assertModsUpdated(updatedMods);
        assertModsNotUpdated(anotherUpdatedMods);
    }

    @Test
    public void testTwoObjectsUpdatedSingleRequest() throws Exception {
        populateFedora("uuid:ae0091e0-192d-57a0-a8ad-9c1dc82f33ad");
        // create a second work obj in Fedora and add mods to it
        PID anotherWorkPid = PIDs.get("uuid:bf0091e0-192d-57a0-b9be-8b0dc82f33be");
        WorkObject anotherWorkObj = factory.createWorkObject(anotherWorkPid, null);
        anotherWorkObj.setDescription(new FileInputStream(new File("src/test/resources/mods/work-mods.xml")));

        InputStream originalMods = workObj.getMODS().getBinaryStream();
        InputStream anotherOriginalMods = anotherWorkObj.getMODS().getBinaryStream();
        assertModsNotUpdated(originalMods);
        assertModsNotUpdated(anotherOriginalMods);

        importFile = createTempImportFile("src/test/resources/mods/two-works-mods.xml");
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        InputStream updatedMods = workObj.getMODS().getBinaryStream();
        InputStream anotherUpdatedMods = anotherWorkObj.getMODS().getBinaryStream();
        assertModsUpdated(updatedMods);
        assertModsUpdated(anotherUpdatedMods);
    }

    @Test
    public void testUpdateFileMissingBulkMetadataTag() throws Exception {
        populateFedora("uuid:bf0091e0-192d-46f9-a8ad-8b0dc82f33be");
        InputStream originalMods = workObj.getMODS().getBinaryStream();
        importFile = createTempImportFile("src/test/resources/mods/bad-update-work-mods.xml");
        createJob();

        job.run();

        assertModsNotUpdated(originalMods);
        verify(mailSender).send(any(MimeMessage.class));
        assertEquals("File is not a bulk-metadata-update doc", job.getFailed().get(importFile.getAbsolutePath()));
    }

    @Test
    public void testUpdateFileMissingUpdateTag() throws Exception {
        populateFedora("uuid:bf0091e0-203e-46f9-a8ad-8b0dc82f33be");
        InputStream originalMods = workObj.getMODS().getBinaryStream();
        importFile = createTempImportFile("src/test/resources/mods/no-update-mods.xml");
        createJob();

        job.run();

        assertModsNotUpdated(originalMods);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    public void testUpdateFileHasXMLErrors() throws Exception {
        populateFedora("uuid:ca0091e0-192d-46f9-a8ad-8b0dc82f33cf");
        InputStream originalMods = workObj.getMODS().getBinaryStream();
        importFile = createTempImportFile("src/test/resources/mods/bad-xml.xml");
        createJob();

        job.run();

        assertModsNotUpdated(originalMods);
        verify(mailSender).send(any(MimeMessage.class));
        assertEquals("The import file contains XML errors", job.getFailed().get(importFile.getAbsolutePath()));
    }

    private void assertModsUpdated(InputStream updatedMods) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(updatedMods);
        Element rootEl = doc.getRootElement();
        Element title = rootEl.getChild("titleInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("title",
                JDOMNamespaceUtil.MODS_V3_NS);
        Element dateCreated = rootEl.getChild("originInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("dateCreated",
                JDOMNamespaceUtil.MODS_V3_NS);
        assertEquals("Updated Work Test", title.getValue());
        assertEquals("2018-04-06", dateCreated.getValue());
    }

    private void assertModsNotUpdated(InputStream originalMods) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(originalMods);
        Element rootEl = doc.getRootElement();
        Element title = rootEl.getChild("titleInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("title",
                JDOMNamespaceUtil.MODS_V3_NS);
        Element dateCreated = rootEl.getChild("originInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("dateCreated",
                JDOMNamespaceUtil.MODS_V3_NS);
        assertEquals("Work Test", title.getValue());
        assertEquals("2017-10-09", dateCreated.getValue());
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

    private File createTempImportFile(String filename) throws IOException {
        File tempImportFile = new File(tempDir, "temp-import");
        Files.copy(Paths.get(filename), tempImportFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        File importFile = new File(tempImportFile.getPath());
        return importFile;
    }

    private void populateFedora(String pid) throws FileNotFoundException {
        PID workPid = PIDs.get(pid);
        workObj = factory.createWorkObject(workPid, null);
        workObj.setDescription(new FileInputStream(new File("src/test/resources/mods/work-mods.xml")));
    }

}
