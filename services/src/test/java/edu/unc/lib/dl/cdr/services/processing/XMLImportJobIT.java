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

import static edu.unc.lib.dl.cdr.services.processing.XMLImportTestHelper.addObjectUpdate;
import static edu.unc.lib.dl.cdr.services.processing.XMLImportTestHelper.makeUpdateDocument;
import static edu.unc.lib.dl.cdr.services.processing.XMLImportTestHelper.modsWithTitleAndDate;
import static edu.unc.lib.dl.cdr.services.processing.XMLImportTestHelper.writeToFile;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.mail.internet.MimeMessage;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgroups.util.UUID;
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
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.storage.StorageLocationManagerImpl;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.persist.services.transfer.BinaryTransferServiceImpl;
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

    private final static String USER_EMAIL = "user@example.com";

    private final static String LOC1_ID = "loc1";
    private final static String LOC2_ID = "loc2";

    private final static String ORIGINAL_TITLE = "Work Test";
    private final static String UPDATED_TITLE = "Updated Work Title";
    private final static String ORIGINAL_DATE = "2017-10-09";
    private final static String UPDATED_DATE = "2018-04-06";

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
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private ContentPathFactory pathFactory;
    private WorkObject workObj;

    private PID parentPid;

    private StorageLocationManagerImpl locationManager;
    private BinaryTransferServiceImpl transferService;

    private StorageLocationTestHelper locTestHelper;

    private Path loc1Path;
    private Path loc2Path;

    @Before
    public void init_() throws Exception {
        initMocks(this);

        tempDir = tmpFolder.newFolder();

        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
        when(completeTemplate.execute(any(Object.class))).thenReturn("update was successful");

        TestHelper.setContentBase("http://localhost:48085/rest/");

        loc1Path = tmpFolder.newFolder("loc1").toPath();
        loc2Path = tmpFolder.newFolder("loc2").toPath();

        parentPid = makePid();
        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(new ArrayList<>(asList(parentPid)));

        locTestHelper = new StorageLocationTestHelper();
        locTestHelper.addStorageLocation(LOC1_ID, "Location 1", loc1Path.toString());
        locTestHelper.addMapping(parentPid.getId(), LOC1_ID);
        locTestHelper.addStorageLocation(LOC2_ID, "Location 2", loc2Path.toString());

        locationManager = new StorageLocationManagerImpl();
        locationManager.setConfigPath(locTestHelper.serializeLocationConfig());
        locationManager.setMappingPath(locTestHelper.serializeLocationMappings());
        locationManager.setRepositoryObjectLoader(repoObjLoader);
        locationManager.setPathFactory(pathFactory);
        locationManager.init();

        transferService = new BinaryTransferServiceImpl();
    }

    @Test
    public void testMODSGetsUpdated() throws Exception {
        PID workPid = populateFedora();
        InputStream originalMods = descriptionStream(workPid);
        assertModsNotUpdated(originalMods);

        Document updateDoc = makeUpdateDocument();
        addObjectUpdate(updateDoc, workPid, null)
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));

        InputStream updatedMods = descriptionStream(workPid);
        assertModsUpdated(updatedMods);
    }

    @Test
    public void testTwoWorksOneGetsUpdated() throws Exception {
        PID workPid = populateFedora();
        // create a second work obj in Fedora and add mods to it
        PID anotherWorkPid =  populateFedora();

        InputStream originalMods = descriptionStream(workPid);
        InputStream anotherOriginalMods = descriptionStream(anotherWorkPid);
        assertModsNotUpdated(originalMods);
        assertModsNotUpdated(anotherOriginalMods);

        Document updateDoc = makeUpdateDocument();
        addObjectUpdate(updateDoc, null, null)
            .addContent(modsWithTitleAndDate("Missing pid!", null));
        addObjectUpdate(updateDoc, workPid, "2017-10-18T12:29:53.396Z")
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        assertModsUpdated(descriptionStream(workPid));
        assertModsNotUpdated(descriptionStream(anotherWorkPid));
    }

    @Test
    public void testTwoObjectsUpdatedSingleRequest() throws Exception {
        PID workPid = populateFedora();
        // create a second work obj in Fedora and add mods to it
        PID anotherWorkPid = populateFedora();

        assertModsNotUpdated(descriptionStream(workPid));
        assertModsNotUpdated(descriptionStream(anotherWorkPid));

        Document updateDoc = makeUpdateDocument();
        addObjectUpdate(updateDoc, anotherWorkPid, null)
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        addObjectUpdate(updateDoc, workPid, "2017-10-18T12:29:53.396Z")
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        assertModsUpdated(descriptionStream(workPid));
        assertModsUpdated(descriptionStream(anotherWorkPid));
    }

    @Test
    public void testUpdateFileMissingBulkMetadataTag() throws Exception {
        PID workPid = populateFedora();

        Document updateDoc = new Document();
        updateDoc.addContent(new Element("wrongRoot"));
        addObjectUpdate(updateDoc, workPid, null)
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        assertModsNotUpdated(descriptionStream(workPid));
        verify(mailSender).send(any(MimeMessage.class));
        assertEquals("File is not a bulk-metadata-update doc", job.getFailed().get(importFile.getAbsolutePath()));
    }

    @Test
    public void testUpdateFileMissingUpdateTag() throws Exception {
        PID workPid = populateFedora();

        Document updateDoc = makeUpdateDocument();
        updateDoc.getRootElement().addContent(new Element("object").setAttribute("pid", workPid.getId()));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        assertModsNotUpdated(descriptionStream(workPid));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    public void testUpdateFileHasXMLErrors() throws Exception {
        PID workPid = populateFedora();

        importFile = Files.createTempFile(tempDir.toPath(), "import", ".xml").toFile();
        writeStringToFile(importFile, "<bulkMetadata><mods>busted</mods>", UTF_8);
        createJob();

        job.run();

        assertModsNotUpdated(descriptionStream(workPid));
        verify(mailSender).send(any(MimeMessage.class));
        assertEquals("The import file contains XML errors", job.getFailed().get(importFile.getAbsolutePath()));
    }

    private void assertModsUpdated(InputStream updatedMods) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(updatedMods);
        Element rootEl = doc.getRootElement();
        String title = rootEl.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);
        String dateCreated = rootEl.getChild("originInfo", MODS_V3_NS).getChildText("dateCreated", MODS_V3_NS);
        assertEquals(UPDATED_TITLE, title);
        assertEquals(UPDATED_DATE, dateCreated);
    }

    private void assertModsNotUpdated(InputStream originalMods) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(originalMods);
        Element rootEl = doc.getRootElement();
        String title = rootEl.getChild("titleInfo", MODS_V3_NS).getChild("title",
                MODS_V3_NS).getValue();
        String dateCreated = rootEl.getChild("originInfo", MODS_V3_NS).getChild("dateCreated",
                MODS_V3_NS).getValue();
        assertEquals(ORIGINAL_TITLE, title);
        assertEquals(ORIGINAL_DATE, dateCreated);
    }

    private void createJob() {
        job = new XMLImportJob(USER_EMAIL, agent, importFile);
        job.setUpdateService(updateService);
        job.setMailSender(mailSender);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress(fromAddress);
        job.setMimeMessage(mimeMsg);
        job.setLocationManager(locationManager);
        job.setTransferService(transferService);
    }

    private PID populateFedora() throws Exception {
        PID workPid = makePid();
        workObj = factory.createWorkObject(workPid, null);
        Document doc = new Document()
                .addContent(modsWithTitleAndDate(ORIGINAL_TITLE, ORIGINAL_DATE));
        File workModsFile = writeToFile(doc);
        workObj.setDescription(workModsFile.toPath().toUri());
        return workPid;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private InputStream descriptionStream(PID pid) {
        return repoObjLoader.getWorkObject(pid).getDescription().getBinaryStream();
    }
}
