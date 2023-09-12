package edu.unc.lib.boxc.operations.impl.importxml;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.addObjectUpdate;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.documentToInputStream;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.makeUpdateDocument;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.modsWithTitleAndDate;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.writeToFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.samskivert.mustache.Template;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.BulkXMLConstants;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.transfer.BinaryTransferServiceImpl;

/**
 *
 * @author harring
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/import-job-it.xml")
})
public class ImportXMLJobIT {

    private ImportXMLJob job;
    private AutoCloseable closeable;

    private final static String USER_EMAIL = "user@example.com";

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

    private String fromAddress = "no-reply@example.com";
    private String adminAddress = "admin@example.com";

    @TempDir
    public Path tmpFolder;
    private File tempDir;

    @Autowired
    private RepositoryObjectFactory factory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private ContentPathFactory pathFactory;
    private WorkObject workObj;

    @Autowired
    private StorageLocationManager locationManager;
    @Autowired
    private BinaryTransferServiceImpl transferService;
    @Autowired
    private String baseAddress;

    @BeforeEach
    public void init_() throws Exception {
        closeable = openMocks(this);

        tempDir = tmpFolder.resolve("tempDir").toFile();

        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
        when(completeTemplate.execute(any(Object.class))).thenReturn("update was successful");

        TestHelper.setContentBase(baseAddress);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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
        addObjectUpdate(updateDoc, workPid, "2999-10-18T12:29:53.396Z")
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
        addObjectUpdate(updateDoc, workPid, "2999-10-18T12:29:53.396Z")
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
        assertEquals(job.getFailed().get(importFile.getAbsolutePath()), "File is not a bulk-metadata-update doc");
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

        importFile = tempDir.toPath().resolve("import.xml").toFile();
        writeStringToFile(importFile, "<bulkMetadata><mods>busted</mods>", UTF_8);
        createJob();

        job.run();

        assertModsNotUpdated(descriptionStream(workPid));
        verify(mailSender).send(any(MimeMessage.class));
        assertEquals(job.getFailed().get(importFile.getAbsolutePath()), "The import file contains XML errors");
    }

    @Test
    public void testObjectDoesNotExist() throws Exception {
        PID workPid = PIDs.get(UUID.randomUUID().toString());

        Document updateDoc = makeUpdateDocument();
        addObjectUpdate(updateDoc, workPid, null)
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        assertEquals(job.getFailed().get(workPid.getQualifiedId()), "Object not found");
    }

    @Test
    public void testNoOperation() throws Exception {
        PID workPid = populateFedora();
        InputStream originalMods = descriptionStream(workPid);
        assertModsNotUpdated(originalMods);

        Document updateDoc = makeUpdateDocument();
        Element dsEl = addObjectUpdate(updateDoc, workPid, null)
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        // Remove the operation
        dsEl.removeAttribute(BulkXMLConstants.OPERATION_ATTR);
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));

        InputStream updatedMods = descriptionStream(workPid);
        assertModsNotUpdated(updatedMods);
    }

    @Test
    public void testMultipleWorksOneOptimisticLockingFailure() throws Exception {
        // With MODS, obj timestamp older than last modified in import
        PID workPid1 = populateFedora();
        // With optimistic locking failure, obj timestamp newer than last modified in import
        PID workPid2 = populateFedora();
        // Without MODS, with optimistic lock timestamp provided
        PID workPid3 = populateFedora();

        InputStream workMods1 = descriptionStream(workPid1);
        InputStream workMods2 = descriptionStream(workPid2);

        Document updateDoc = makeUpdateDocument();
        Instant work1ModsUpdated = repoObjLoader.getWorkObject(workPid1).getDescription().getLastModified().toInstant();
        addObjectUpdate(updateDoc, workPid1, work1ModsUpdated.toString())
                .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        addObjectUpdate(updateDoc, workPid2, "1999-10-18T12:29:53.396Z")
                .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        addObjectUpdate(updateDoc, workPid3, "2999-10-18T12:29:53.396Z")
                .addContent(modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        importFile = writeToFile(updateDoc);
        createJob();

        job.run();

        verify(mailSender).send(any(MimeMessage.class));
        assertModsUpdated(descriptionStream(workPid1));
        assertModsNotUpdated(descriptionStream(workPid2));
        assertModsUpdated(descriptionStream(workPid3));
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
        ImportXMLRequest request = new ImportXMLRequest(USER_EMAIL, agent, importFile);
        job = new ImportXMLJob(request);
        job.setUpdateService(updateService);
        job.setMailSender(mailSender);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress(fromAddress);
        job.setAdminAddress(adminAddress);
        job.setMimeMessage(mimeMsg);
        job.setLocationManager(locationManager);
        job.setTransferService(transferService);
    }

    private PID populateFedora() throws Exception {
        PID workPid = makePid();
        workObj = factory.createWorkObject(workPid, null);
        Document doc = new Document()
                .addContent(modsWithTitleAndDate(ORIGINAL_TITLE, ORIGINAL_DATE));
        InputStream modsStream = documentToInputStream(doc);
        updateService.updateDescription(new UpdateDescriptionRequest(agent, workObj, modsStream));
        return workPid;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private InputStream descriptionStream(PID pid) {
        return repoObjLoader.getWorkObject(pid).getDescription().getBinaryStream();
    }
}
