package edu.unc.lib.boxc.services.camel.exportxml;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.NamespaceConstants.FITS_URI;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/export-xml-route-it-context.xml")
})
public class ExportXMLRouteIT {
    private static final String EMAIL = "test@example.com";

    @Autowired
    private CamelContext cdrExportXML;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private RepositoryObjectSolrIndexer solrIndexer;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    protected EmbeddedSolrServer server;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;
    @Autowired
    private ExportXMLRequestService requestService;
    @Autowired
    private EmailHandler emailHandler;
    @Autowired
    private ExportXMLProcessor exportXmlProcessor;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;

    @Captor
    private ArgumentCaptor<String> toCaptor;
    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> bodyCaptor;
    @Captor
    private ArgumentCaptor<String> filenameCaptor;
    @Captor
    private ArgumentCaptor<File> attachmentCaptor;

    private ContentRootObject rootObj;
    private AdminUnit unitObj;
    private CollectionObject collObj1;
    private WorkObject workObj1;
    private CollectionObject collObj2;
    private WorkObject workObj2;
    private FileObject fileObj1;

    private AgentPrincipals agent;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        reset(emailHandler);
        TestHelper.setContentBase("http://localhost:48085/rest");
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("adminGroup"));
        generateBaseStructure();
        exportXmlProcessor.setObjectsPerExport(500);
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
        assertNull(attachmentCaptor.getValue());
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

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithMods(rootEl, ResourceType.Work, workObj1.getPid());

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportWorkModsAndFitsTest() throws Exception {
        String fitsContent = "<fits>content</fits>";
        URI fitsUri = makeContentUri(fitsContent);
        PID fitsPid = getTechnicalMetadataPid(fileObj1.getPid());
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

        Element rootEl = getExportedDocumentRootEl();

        assertHasObjectWithoutMods(rootEl, ResourceType.Work, workObj2.getPid());
        assertHasObjectWithDatastream(rootEl, ResourceType.Work, workObj2.getPid(), DatastreamType.MD_EVENTS,
                "application/n-triples", logContent);

        assertExportDocumentCount(rootEl, 1);
    }

    @Test
    public void exportCollectionFitsExcludeNoDatastreamTest() throws Exception {
        String fitsContent = "<fits>content</fits>";
        URI fitsUri = makeContentUri(fitsContent);
        PID fitsPid = getTechnicalMetadataPid(fileObj1.getPid());
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
                bodyCaptor.capture(), filenameCaptor.capture(), attachmentCaptor.capture());
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
        return getExportedDocument(attachmentCaptor.getAllValues().get(page - 1));
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
        fileObj1 = workObj1.addDataFile(makeContentUri("hello"), "text.txt", "text/plain", null, null);
        InputStream modsStream2 = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workPid1, modsStream2));
        PID workPid2 = pidMinter.mintContentPid();
        workObj2 = repositoryObjectFactory.createWorkObject(workPid2, null);

        collObj1.addMember(workObj1);
        collObj2.addMember(workObj2);
    }

    protected URI makeContentUri(String content) throws Exception {
        File contentFile = File.createTempFile("test", ".txt");
        contentFile.deleteOnExit();
        FileUtils.write(contentFile, content, UTF_8);
        return contentFile.toPath().toUri();
    }

    protected InputStream streamResource(String resourcePath) throws Exception {
        return getClass().getResourceAsStream(resourcePath);
    }
}
