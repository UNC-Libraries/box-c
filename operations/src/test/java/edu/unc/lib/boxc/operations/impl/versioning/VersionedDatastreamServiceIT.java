package edu.unc.lib.boxc.operations.impl.versioning;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.DCR_PACKAGING_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.operations.api.exceptions.StateUnmodifiedException;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.common.xml.SecureXMLFactory;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.versioning.DatastreamHistoryLog;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.DatastreamVersion;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceManagerImpl;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper;
import edu.unc.lib.boxc.persist.impl.transfer.BinaryTransferServiceImpl;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class VersionedDatastreamServiceIT {
    private static final String TEST_TITLE = "Historical doc";

    @TempDir
    public Path tmpFolder;

    private VersionedDatastreamService service;

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private BinaryTransferServiceImpl transferService;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private TransactionManager transactionManager;

    private IngestSourceManagerImpl sourceManager;
    private Path sourcePath;

    private PID parentPid;
    private PID dsPid;

    @BeforeEach
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);

        service = new VersionedDatastreamService();
        service.setBinaryTransferService(transferService);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setTransactionManager(transactionManager);

        File sourceMappingFile = new File(tmpFolder.toFile(), "sourceMapping.json");
        FileUtils.writeStringToFile(sourceMappingFile, "[]", "UTF-8");
        sourcePath = tmpFolder.resolve("source_dir");
        Files.createDirectory(sourcePath);
        Path sourceConfigPath = IngestSourceTestHelper.createConfigFile(
                IngestSourceTestHelper.createFilesystemConfig("source_dir", "src", sourcePath, asList("*")));

        sourceManager = new IngestSourceManagerImpl();
        sourceManager.setConfigPath(sourceConfigPath.toString());
        sourceManager.setMappingPath(sourceMappingFile.toString());
        sourceManager.init();

        transferService.setIngestSourceManager(sourceManager);

        parentPid = pidMinter.mintContentPid();
        dsPid = DatastreamPids.getMdDescriptivePid(parentPid);
    }

    @Test
    public void addVersion_NewDatastream_StreamContent() throws Exception {
        repoObjFactory.createFolderObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        DatastreamVersion newV = new DatastreamVersion(dsPid);
        newV.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV.setContentType("text/xml");

        BinaryObject dsObj = service.addVersion(newV);
        Document storedDoc = inputStreamToDocument(dsObj.getBinaryStream());

        assertHasModsTitle(TEST_TITLE, storedDoc);
    }

    @Test
    public void addVersion_NewDatastream_UriContent() throws Exception {
        repoObjFactory.createFolderObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        Path srcFile = sourcePath.resolve("src_mods.xml");
        DatastreamVersion newV = new DatastreamVersion(dsPid);
        Files.copy(getModsDocumentStream(TEST_TITLE), srcFile);
        newV.setStagedContentUri(srcFile.toUri());
        newV.setContentType("text/xml");

        BinaryObject dsObj = service.addVersion(newV);
        Document storedDoc = inputStreamToDocument(dsObj.getBinaryStream());

        assertHasModsTitle(TEST_TITLE, storedDoc);
        assertNotNull(dsObj.getSha1Checksum(), "Checksum not set");
    }

    @Test
    public void addVersion_ExistingDatastream() throws Exception {
        repoObjFactory.createFolderObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        DatastreamVersion newV1 = new DatastreamVersion(dsPid);
        newV1.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV1.setContentType("text/xml");

        BinaryObject dsObj = service.addVersion(newV1);
        Date originalCreated = dsObj.getCreatedDate();
        String digest1 = dsObj.getSha1Checksum();
        assertNotNull(digest1, "Checksum not set for first version");

        DatastreamVersion newV2 = new DatastreamVersion(dsPid);
        newV2.setContentStream(getModsDocumentStream("more titles"));
        newV2.setContentType("text/xml");

        BinaryObject dsObjUpdated = service.addVersion(newV2);
        String digest2 = dsObjUpdated.getSha1Checksum();
        assertNotNull(digest2, "Checksum not set for second version");
        assertNotEquals(digest1, digest2, "Versions must have different digests");

        Document headDoc = inputStreamToDocument(dsObjUpdated.getBinaryStream());
        assertHasModsTitle("more titles", headDoc);

        PID historyPid = DatastreamPids.getDatastreamHistoryPid(dsPid);
        BinaryObject dsHistoryObj = repoObjLoader.getBinaryObject(historyPid);
        assertNotNull(dsHistoryObj.getSha1Checksum(), "Checksum not set for history");

        Document logDoc = inputStreamToDocument(dsHistoryObj.getBinaryStream());
        List<Element> versions = listVersions(logDoc);
        assertEquals(1, versions.size());

        Element versionEl = versions.get(0);
        assertVersionAttributes(originalCreated, "text/xml", versionEl);
        assertVersionHasModsTitle(TEST_TITLE, versionEl);
    }

    @Test
    public void addVersion_DatastreamWithHistory() throws Exception {
        repoObjFactory.createFolderObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        DatastreamVersion newV1 = new DatastreamVersion(dsPid);
        newV1.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV1.setContentType("text/xml");

        BinaryObject dsObj = service.addVersion(newV1);
        Date version1Date = dsObj.getCreatedDate();
        String digest1 = dsObj.getSha1Checksum();
        assertNotNull(digest1, "Checksum not set for first version");

        DatastreamVersion newV2 = new DatastreamVersion(dsPid);
        newV2.setContentStream(getModsDocumentStream("more titles"));
        newV2.setContentType("text/xml");

        BinaryObject dsObj2 = service.addVersion(newV2);
        Date version2Date = dsObj2.getLastModified();
        String digest2 = dsObj2.getSha1Checksum();
        assertNotNull(digest2, "Checksum not set for second version");

        DatastreamVersion newV3 = new DatastreamVersion(dsPid);
        newV3.setContentStream(getModsDocumentStream("lets leave it here"));
        newV3.setContentType("text/xml");

        BinaryObject dsObjFinal = service.addVersion(newV3);
        String digest3 = dsObjFinal.getSha1Checksum();
        assertNotNull(digest3, "Checksum not set for second version");

        assertNotEquals(digest1, digest2);
        assertNotEquals(digest2, digest3);
        assertNotEquals(digest1, digest3);

        Document headDoc = inputStreamToDocument(dsObjFinal.getBinaryStream());
        assertHasModsTitle("lets leave it here", headDoc);

        assertNotEquals(version2Date, dsObjFinal.getLastModified(),
                "Second version timestamp should not match head version");

        // check historic versions
        PID historyPid = DatastreamPids.getDatastreamHistoryPid(dsPid);
        BinaryObject dsHistoryObj = repoObjLoader.getBinaryObject(historyPid);
        assertNotNull(dsHistoryObj.getSha1Checksum(), "Checksum not set for history");

        Document logDoc = inputStreamToDocument(dsHistoryObj.getBinaryStream());
        List<Element> versions = listVersions(logDoc);
        assertEquals(2, versions.size());

        Element versionEl = versions.get(0);
        assertVersionAttributes(version1Date, "text/xml", versionEl);
        assertVersionHasModsTitle(TEST_TITLE, versionEl);

        Element versionEl2 = versions.get(1);
        assertVersionAttributes(version2Date, "text/xml", versionEl2);
        assertVersionHasModsTitle("more titles", versionEl2);
    }

    @Test
    public void addVersion_NewDatastream_ProvidedTransferSession() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        try (BinaryTransferSession session = transferService.getSession(work)) {
            DatastreamVersion newV = new DatastreamVersion(dsPid);
            newV.setContentStream(getModsDocumentStream(TEST_TITLE));
            newV.setContentType("text/xml");
            newV.setTransferSession(session);

            BinaryObject dsObj = service.addVersion(newV);
            Document storedDoc = inputStreamToDocument(dsObj.getBinaryStream());

            assertHasModsTitle(TEST_TITLE, storedDoc);
            assertNotNull(dsObj.getSha1Checksum(), "Checksum not set for first version");
        }
    }

    @Test
    public void addVersion_NonDatastreamObject() throws Exception {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            repoObjFactory.createWorkObject(parentPid, null);
            treeIndexer.indexAll(baseAddress);

            DatastreamVersion newV = new DatastreamVersion(parentPid);
            newV.setContentStream(getModsDocumentStream(TEST_TITLE));
            newV.setContentType("text/xml");

            service.addVersion(newV);
        });
    }

    @Test
    public void addVersion_NonDatastreamPid() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DatastreamVersion newV = new DatastreamVersion(parentPid);
            newV.setContentStream(getModsDocumentStream(TEST_TITLE));
            newV.setContentType("text/xml");

            service.addVersion(newV);
        });
    }

    @Test
    public void addVersion_DatastreamWithSameContentSkipUnmodified() throws Exception {
        repoObjFactory.createFolderObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        DatastreamVersion newV1 = new DatastreamVersion(dsPid);
        newV1.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV1.setContentType("text/xml");
        newV1.setSkipUnmodified(false);

        BinaryObject dsObj = service.addVersion(newV1);

        // Perform update with same content and skipUnmodified
        DatastreamVersion newV2 = new DatastreamVersion(dsPid);
        newV2.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV2.setContentType("text/xml");
        newV2.setSkipUnmodified(true);

        assertThrows(StateUnmodifiedException.class, () -> {
            service.addVersion(newV2);
        });

        // No history should have been created since no update occurred
        PID historyPid = DatastreamPids.getDatastreamHistoryPid(dsPid);
        assertThrows(NotFoundException.class, () -> {
            repoObjLoader.getBinaryObject(historyPid);
        });

        // Perform third request to update with same content, but without skipping unmodified
        DatastreamVersion newV3 = new DatastreamVersion(dsPid);
        newV3.setContentStream(getModsDocumentStream(TEST_TITLE));
        newV3.setContentType("text/xml");
        newV3.setSkipUnmodified(false);

        service.addVersion(newV3);

        assertNotNull(repoObjLoader.getBinaryObject(historyPid), "History object should have been created");
    }

    private InputStream getModsDocumentStream(String title) throws Exception {
        Document document = new Document()
                .addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText(title))));

        return convertDocumentToStream(document);
    }

    private InputStream convertDocumentToStream(Document doc) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter(Format.getPrettyFormat()).output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());
    }

    private void assertHasModsTitle(String expected, Document doc) {
        assertHasModsTitle(expected, doc.getRootElement());
    }

    private void assertVersionHasModsTitle(String expected, Element versionEl) {
        assertHasModsTitle(expected, versionEl.getChild("mods", MODS_V3_NS));
    }

    private void assertHasModsTitle(String expected, Element modsEl) {
        String titleVal = modsEl.getChild("titleInfo", MODS_V3_NS)
                .getChildText("title", MODS_V3_NS);
        assertEquals(expected, titleVal);
    }

    private Document inputStreamToDocument(InputStream docStream) throws Exception{
        return SecureXMLFactory.createSAXBuilder().build(docStream);
    }

    private void assertVersionAttributes(Date expectedTime, String expectedType, Element versionEl) {
        String created = versionEl.getAttributeValue(DatastreamHistoryLog.CREATED_ATTR);
        assertEquals(expectedTime, DateTimeUtil.parseUTCToDate(created));
        String contentType = versionEl.getAttributeValue(DatastreamHistoryLog.CONTENT_TYPE_ATTR);
        assertEquals(expectedType, contentType);
    }

    private List<Element> listVersions(Document logDoc) {
        return logDoc.getRootElement()
                .getChildren(DatastreamHistoryLog.VERSION_TAG, DCR_PACKAGING_NS);
    }
}
