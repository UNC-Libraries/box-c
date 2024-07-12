package edu.unc.lib.boxc.services.camel.exportxml;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.common.util.ZipFileUtil;
import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.fcrepo.client.FcrepoClient;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.NamespaceConstants.FITS_URI;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class ExportXMLRouteIT extends CamelSpringTestSupport {
    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "spring-test/acl-service-context.xml",
                "spring-test/solr-indexing-context.xml",
                "export-xml-route-it-context.xml");
    }
    private static final String EMAIL = "test@example.com";

    private AutoCloseable closeable;

    private String baseAddress;
    private CamelContext cdrExportXML;
    private RepositoryObjectTreeIndexer treeIndexer;
    private RepositoryObjectSolrIndexer solrIndexer;
    private RepositoryInitializer repoInitializer;
    protected RepositoryObjectLoader repositoryObjectLoader;
    protected RepositoryObjectFactory repositoryObjectFactory;
    protected PIDMinter pidMinter;
    protected EmbeddedSolrServer server;
    private UpdateDescriptionService updateDescriptionService;
    private ExportXMLRequestService requestService;
    private EmailHandler emailHandler;
    private ExportXMLProcessor exportXmlProcessor;
    private PremisLoggerFactory premisLoggerFactory;
    private FcrepoClient fcrepoClient;
    private StorageLocationTestHelper storageLocationTestHelper;

    @Captor
    private ArgumentCaptor<String> toCaptor;
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> bodyCaptor;
    @Captor
    private ArgumentCaptor<String> filenameCaptor;
    private List<Path> attachmentPaths;
    @TempDir
    public Path tmpFolder;

    private ContentRootObject rootObj;
    private AdminUnit unitObj;
    private CollectionObject collObj1;
    private WorkObject workObj1;
    private CollectionObject collObj2;
    private WorkObject workObj2;
    private FileObject fileObj1;

    private AgentPrincipals agent;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        cdrExportXML = applicationContext.getBean("cdrExportXML", CamelContext.class);
        treeIndexer = applicationContext.getBean(RepositoryObjectTreeIndexer.class);
        solrIndexer = applicationContext.getBean(RepositoryObjectSolrIndexer.class);
        repoInitializer = applicationContext.getBean(RepositoryInitializer.class);
        repositoryObjectLoader = applicationContext.getBean("repositoryObjectLoader", RepositoryObjectLoader.class);
        repositoryObjectFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        pidMinter = applicationContext.getBean(PIDMinter.class);
        server = applicationContext.getBean(EmbeddedSolrServer.class);
        updateDescriptionService = applicationContext.getBean(UpdateDescriptionService.class);
        requestService = applicationContext.getBean(ExportXMLRequestService.class);
        emailHandler = applicationContext.getBean(EmailHandler.class);
        exportXmlProcessor = applicationContext.getBean(ExportXMLProcessor.class);
        premisLoggerFactory = applicationContext.getBean(PremisLoggerFactory.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        storageLocationTestHelper = applicationContext.getBean(StorageLocationTestHelper.class);

        TestHelper.setContentBase(baseAddress);
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("adminGroup"));
        generateBaseStructure();
        exportXmlProcessor.setObjectsPerExport(500);

        attachmentPaths = new ArrayList<>();
        doAnswer(invocation -> {
            var attachment = invocation.getArgument(4, File.class);
            if (attachment == null) {
                return null;
            }
            var copiedPath = Files.createTempDirectory(tmpFolder, "email").resolve(attachment.getName());
            Files.copy(attachment.toPath(), copiedPath);
            attachmentPaths.add(copiedPath);
            return null;
        }).when(emailHandler).sendEmail(any(), any(), any(), any(), any());
    }

    @AfterEach
    public void closeService() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
    }

    @Test
    public void exportWorksExcludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(false, false, workObj1.getPid(), workObj2.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithoutMods(rootEl, ResourceType.Work, workObj2.getPid());

        assertExportDocumentCount(rootEl, 2);
    }

    @Test
    public void exportCollectionExcludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(false, false, collObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Collection, collObj1.getPid());

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportCollectionIncludeChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(true, false, collObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Collection, collObj1.getPid());
        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithoutMods(rootEl, ResourceType.File, fileObj1.getPid());

        assertExportDocumentCount(rootEl, 3);
    }

    @Test
    public void exportWorksNoPermissionTest() throws Exception {
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("public"));

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenDone(1)
                .create();

        sendRequest(createRequest(false, false, workObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();
        assertExportDocumentCount(rootEl, 0);
    }

    @Test
    public void exportCollectionNoPermissionTest() throws Exception {
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("public"));

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenDone(1)
                .create();

        sendRequest(createRequest(false, false, collObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        Element rootEl = getExportedDocumentRootEl();
        assertExportDocumentCount(rootEl, 0);
    }

    @Test
    public void exportUnitIncludeChildrenPagedTest() throws Exception {
        exportXmlProcessor.setObjectsPerExport(2);

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(true, false, unitObj.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent(3);
        assertTempFilesDeleted();

        Element rootEl1 = getExportedDocumentRootEl(1);
        assertHasObjectWithoutMods(rootEl1, ResourceType.AdminUnit, unitObj.getPid());
        assertHasObjectWithMods(rootEl1, ResourceType.Collection, collObj1.getPid());
        assertExportDocumentCount(rootEl1, 2);

        Element rootEl2 = getExportedDocumentRootEl(2);
        assertHasObjectWithoutMods(rootEl2, ResourceType.Collection, collObj2.getPid());
        assertHasObjectWithMods(rootEl2, ResourceType.Work, workObj1.getPid());
        assertExportDocumentCount(rootEl1, 2);

        Element rootEl3 = getExportedDocumentRootEl(3);
        assertHasObjectWithoutMods(rootEl3, ResourceType.Work, workObj2.getPid());
        assertHasObjectWithoutMods(rootEl3, ResourceType.File, fileObj1.getPid());
        assertExportDocumentCount(rootEl3, 2);
    }

    @Test
    public void exportUnitIncludeChildrenPagedExcludeNoDatastreamsTest() throws Exception {
        exportXmlProcessor.setObjectsPerExport(1);

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(true, true, unitObj.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent(2);
        assertTempFilesDeleted();

        Element rootEl1 = getExportedDocumentRootEl(1);
        assertHasObjectWithMods(rootEl1, ResourceType.Collection, collObj1.getPid());
        assertExportDocumentCount(rootEl1, 1);

        Element rootEl2 = getExportedDocumentRootEl(2);
        assertHasObjectWithMods(rootEl2, ResourceType.Work, workObj1.getPid());
        assertExportDocumentCount(rootEl1, 1);
    }

    @Test
    public void exportCollectionExcludeChildrenExcludeNoDatastreamsTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(false, true, collObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent(1);
        assertTempFilesDeleted();

        Element rootEl1 = getExportedDocumentRootEl(1);
        assertHasObjectWithMods(rootEl1, ResourceType.Collection, collObj1.getPid());
        assertExportDocumentCount(rootEl1, 1);
    }

    @Test
    public void exportWorkNoModsExcludeNoDatastreamsTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(true, true, workObj2.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();

        assertNull(filenameCaptor.getValue());
        assertTrue(attachmentPaths.isEmpty());
        assertEquals("DCR Metadata Export returned no results", subjectCaptor.getValue());
    }

    @Test
    public void exportWorkWithModsExcludeNoDatastreamsIncChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(true, true, workObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportWorkWithModsExcludeNoDatastreamsExcChildrenTest() throws Exception {
        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        sendRequest(createRequest(false, true, workObj1.getPid()));

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportWorkModsAndFitsTest() throws Exception {
        String fitsContent = "<fits>content</fits>";
        PID fitsPid = getTechnicalMetadataPid(fileObj1.getPid());
        URI fitsUri = makeContentUri(fitsPid, fitsContent);
        fileObj1.addBinary(fitsPid, fitsUri, TECHNICAL_METADATA.getDefaultFilename(), TECHNICAL_METADATA.getMimetype(),
                null, null, IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        ExportXMLRequest request = createRequest(true, true, workObj1.getPid());
        request.setDatastreams(EnumSet.of(DatastreamType.MD_DESCRIPTIVE, DatastreamType.TECHNICAL_METADATA));
        sendRequest(request);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithDatastream(rootEl, ResourceType.File, fileObj1.getPid(), DatastreamType.TECHNICAL_METADATA,
                "text/xml", fitsContent);

        // The FITS belongs to the FileObject, so it will be returned as a separate object
        assertExportDocumentCount(rootEl, 2);
    }

    @Test
    public void exportWorkModsAndPremisTest() throws Exception {
        PremisLogger logger = premisLoggerFactory.createPremisLogger(workObj1);
        logger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested this thing")
                .writeAndClose();
        BinaryObject premisDs = repositoryObjectLoader.getBinaryObject(
                DatastreamPids.getMdEventsPid(workObj1.getPid()));
        String logContent = IOUtils.toString(premisDs.getBinaryStream(), StandardCharsets.UTF_8);

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        ExportXMLRequest request = createRequest(true, true, workObj1.getPid());
        request.setDatastreams(EnumSet.of(DatastreamType.MD_DESCRIPTIVE, DatastreamType.MD_EVENTS));
        sendRequest(request);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());
        assertHasObjectWithDatastream(rootEl, ResourceType.Work, workObj1.getPid(), DatastreamType.MD_EVENTS,
                "application/n-triples", logContent);

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportWorkModsAndPremisNoModsTest() throws Exception {
        PremisLogger logger = premisLoggerFactory.createPremisLogger(workObj2);
        logger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested this other thing")
                .writeAndClose();
        BinaryObject premisDs = repositoryObjectLoader.getBinaryObject(
                DatastreamPids.getMdEventsPid(workObj2.getPid()));
        String logContent = IOUtils.toString(premisDs.getBinaryStream(), StandardCharsets.UTF_8);

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        ExportXMLRequest request = createRequest(true, true, workObj2.getPid());
        request.setDatastreams(EnumSet.of(DatastreamType.MD_DESCRIPTIVE, DatastreamType.MD_EVENTS));
        sendRequest(request);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithoutMods(rootEl, ResourceType.Work, workObj2.getPid());
        assertHasObjectWithDatastream(rootEl, ResourceType.Work, workObj2.getPid(), DatastreamType.MD_EVENTS,
                "application/n-triples", logContent);

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportCollectionFitsExcludeNoDatastreamTest() throws Exception {
        String fitsContent = "<fits>content</fits>";
        PID fitsPid = getTechnicalMetadataPid(fileObj1.getPid());
        URI fitsUri = makeContentUri(fitsPid, fitsContent);
        fileObj1.addBinary(fitsPid, fitsUri, TECHNICAL_METADATA.getDefaultFilename(), TECHNICAL_METADATA.getMimetype(),
                null, null, IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));
        workObj1.setPrimaryObject(fileObj1.getPid());

        indexAll();

        NotifyBuilder notify = new NotifyBuilder(cdrExportXML)
                .whenCompleted(1)
                .create();

        ExportXMLRequest request = createRequest(true, true, collObj1.getPid());
        request.setDatastreams(EnumSet.of(DatastreamType.TECHNICAL_METADATA));
        sendRequest(request);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        assertEmailSent();
        assertTempFilesDeleted();

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithoutMods(rootEl, ResourceType.File, fileObj1.getPid());
        assertHasObjectWithDatastream(rootEl, ResourceType.File, fileObj1.getPid(), DatastreamType.TECHNICAL_METADATA,
                "text/xml", fitsContent);

        assertExportDocumentCount(rootEl, 1);
    }

    private void indexAll() throws Exception{
        treeIndexer.indexAll(rootObj.getPid().getRepositoryPath());
        solrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj1.getPid(), collObj2.getPid(),
                workObj1.getPid(), workObj2.getPid(), fileObj1.getPid());
    }

    private void assertEmailSent() {
        assertEmailSent(1);
    }

    private void assertEmailSent(int numberEmails) {
        verify(emailHandler, times(numberEmails)).sendEmail(toCaptor.capture(), subjectCaptor.capture(),
                bodyCaptor.capture(), filenameCaptor.capture(), any());
    }

    private ExportXMLRequest createRequest(boolean exportChildren, boolean excludeNoDs, PID... pids) {
        ExportXMLRequest request = new ExportXMLRequest();
        request.setAgent(agent);
        request.setExportChildren(exportChildren);
        request.setOnlyIncludeValidDatastreams(excludeNoDs);
        request.setPids(Arrays.stream(pids).map(PID::getId).collect(Collectors.toList()));
        request.setEmail(EMAIL);
        request.setRequestedTimestamp(Instant.now());
        return request;
    }

    private ExportXMLRequest sendRequest(ExportXMLRequest request) throws IOException {
        requestService.sendRequest(request);
        return request;
    }

    private Element getExportedDocumentRootEl() throws Exception {
        return getExportedDocumentRootEl(1);
    }

    private Element getExportedDocumentRootEl(int page) throws Exception {
        Document doc = getExportedDocument(page);

        Element rootEl = doc.getRootElement();
        assertEquals("bulkMetadata", rootEl.getName());
        return rootEl;
    }

    private Document getExportedDocument(int page) throws Exception {
        String toEmail = toCaptor.getAllValues().get(page - 1);
        assertEquals(EMAIL, toEmail);
        String exportFile = filenameCaptor.getAllValues().get(page - 1);
        assertTrue("Unexpected export filename: " + exportFile,
                exportFile.matches("xml\\_export\\_.*\\_0+" + page + "\\.zip"));
        return getExportedDocument(attachmentPaths.get(page - 1).toFile());
    }

    private Document getExportedDocument(File reportZip) throws Exception {
        File unzipDir = ZipFileUtil.unzipToTemp(reportZip);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(unzipDir.toPath(), "*.xml")) {
            SAXBuilder builder = new SAXBuilder();
            return builder.build(Files.newInputStream(dirStream.iterator().next()));
        }
    }

    private void assertExportDocumentCount(Element rootEl, int expectedCnt) {
        List<Element> objEls = rootEl.getChildren("object");
        assertEquals(expectedCnt, objEls.size());
    }

    private void assertHasObjectWithoutMods(Element rootEl, ResourceType expectedType, PID pid) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertEquals(expectedType.name(), objEl.getAttributeValue("type"));
        Element dsEl = getDatastreamElByType(objEl, DatastreamType.MD_DESCRIPTIVE);
        assertNull(dsEl);
    }

    private void assertHasObjectWithMods(Element rootEl, ResourceType expectedType, PID pid) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertEquals(expectedType.name(), objEl.getAttributeValue("type"));
        Element dsEl = getDatastreamElByType(objEl, DatastreamType.MD_DESCRIPTIVE);
        assertNotNull(dsEl);
        assertNotNull(dsEl.getAttributeValue("lastModified"));
        assertEquals("update", dsEl.getAttributeValue("operation"));
        Element modsEl = dsEl.getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
        assertNotNull(modsEl);
    }

    private void assertHasObjectWithDatastream(Element rootEl, ResourceType expectedType, PID pid,
            DatastreamType expectedDsType, String expectedMimetype, String expectedContent) {
        Element objEl = getObjectElByPid(rootEl, pid);
        assertNotNull("Did not contain expected child object " + pid, objEl);
        assertEquals(expectedType.name(), objEl.getAttributeValue("type"));
        Element dsEl = getDatastreamElByType(objEl, expectedDsType);
        assertNotNull(dsEl);
        assertNotNull(dsEl.getAttributeValue("lastModified"));
        assertEquals(expectedMimetype, dsEl.getAttributeValue("mimetype"));

        String content;
        if (expectedMimetype.equals("text/xml")) {
            Element contentEl = dsEl.getChildren().get(0);
            content = new XMLOutputter(Format.getRawFormat()).outputString(contentEl);
        } else {
            content = dsEl.getTextTrim();
        }
        assertEquals(expectedContent.trim(), content);
    }

    private void assertTempFilesDeleted() {
        var tempDir = System.getProperty("java.io.tmpdir");
        String wildCardValue = "xml_export*";
        try (var stream = Files.list(Paths.get(tempDir))) {
            var filters = stream.filter(file ->
                    wildcardMatch(file.getFileName().toString(), wildCardValue));
            var results = filters.collect(Collectors.toList());
            assertTrue(results.isEmpty());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Element getDatastreamElByType(Element objEl, DatastreamType dsType) {
        return objEl.getChildren("datastream").stream()
                .filter(e -> e.getAttributeValue("type").equals(dsType.getId()))
                .findFirst().orElse(null);
    }

    private Element getObjectElByPid(Element rootEl, PID pid) {
        List<Element> objEls = rootEl.getChildren("object");
        for (Element objEl : objEls) {
            String pidAttr = objEl.getAttributeValue("pid");
            if (pid.getQualifiedId().equals(pidAttr)) {
                return objEl;
            }
        }
        return null;
    }

    private void generateBaseStructure() throws Exception {
        repoInitializer.initializeRepository();
        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        PID unitPid = pidMinter.mintContentPid();
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin unit")
                    .addUnitOwner("adminGroup").model);
        rootObj.addMember(unitObj);

        PID collPid1 = pidMinter.mintContentPid();
        collObj1 = repositoryObjectFactory.createCollectionObject(collPid1,
                new AclModelBuilder("Collection 1").model);
        InputStream modsStream1 = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, collObj1, modsStream1));
        PID collPid2 = pidMinter.mintContentPid();
        collObj2 = repositoryObjectFactory.createCollectionObject(collPid2,
                new AclModelBuilder("Collection 2")
                    .addCanManage("coll2_manage").model);

        unitObj.addMember(collObj1);
        unitObj.addMember(collObj2);

        PID workPid1 = pidMinter.mintContentPid();
        workObj1 = repositoryObjectFactory.createWorkObject(workPid1, null);
        PID filePid = pidMinter.mintContentPid();
        PID originalPid = DatastreamPids.getOriginalFilePid(filePid);
        fileObj1 = workObj1.addDataFile(filePid, makeContentUri(originalPid, "hello"), "text.txt", "text/plain", null, null, null);
        InputStream modsStream2 = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workPid1, modsStream2));
        PID workPid2 = pidMinter.mintContentPid();
        workObj2 = repositoryObjectFactory.createWorkObject(workPid2, null);

        collObj1.addMember(workObj1);
        collObj2.addMember(workObj2);
    }

    protected URI makeContentUri(PID binaryPid, String content) throws Exception {
        var uri = storageLocationTestHelper.makeTestStorageUri(binaryPid);
        FileUtils.write(new File(uri), content, UTF_8);
        return uri;
    }

    protected InputStream streamResource(String resourcePath) throws Exception {
        return getClass().getResourceAsStream(resourcePath);
    }
}
