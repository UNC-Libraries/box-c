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
package edu.unc.lib.dcr.migration.deposit;

import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.PREMIS_DS;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.createPremisDoc;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.listEventResources;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newOutputStream;
import static java.util.stream.Collectors.toList;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dcr.migration.paths.PathIndexingService;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
@TestPropertySource(properties = {"fcrepo.properties.management = relaxed"})
public class DepositRecordTransformerIT {
    static {
        System.setProperty("fcrepo.properties.management", "relaxed");
    }

    private final static Date DEFAULT_CREATED_DATE = DateTimeUtil.parseUTCToDate(
            FoxmlDocumentBuilder.DEFAULT_CREATED_DATE);
    private final static Date DEFAULT_MODIFIED_DATE = DateTimeUtil.parseUTCToDate(
            FoxmlDocumentBuilder.DEFAULT_LAST_MODIFIED);

    @Autowired
    private RepositoryPIDMinter pidMinter;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private StorageLocationManager locManager;
    @Autowired
    private BinaryTransferService transferService;

    private BinaryTransferSession transferSession;
    @Autowired
    private PathIndex pathIndex;
    @Autowired
    PathIndexingService pathIndexingService;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private Path objectsPath;

    private Path datastreamsPath;

    private DepositRecordTransformer transformer;

    private PID bxc3Pid;
    private PID bxc5Pid;

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase("http://localhost:48085/rest");

        datastreamsPath = tmpFolder.newFolder("datastreams").toPath();
        objectsPath = tmpFolder.newFolder("objects").toPath();

        StorageLocation loc = locManager.getStorageLocationById(StorageLocationTestHelper.LOC1_ID);
        transferSession = transferService.getSession(loc);

        bxc3Pid = pidMinter.mintDepositRecordPid();
        bxc5Pid = pidMinter.mintDepositRecordPid();
        transformer = new DepositRecordTransformer(bxc3Pid, bxc5Pid, transferSession);
        transformer.setPathIndex(pathIndex);
        transformer.setPremisLoggerFactory(premisLoggerFactory);
        transformer.setRepositoryObjectFactory(repoObjFactory);
        transformer.setTransactionManager(txManager);
    }

    @After
    public void tearDown() {
        transferSession.close();
    }

    @Test(expected = RepositoryException.class)
    public void transform_NoFoxml() throws Exception {
        transformer.compute();
    }

    @Test(expected = RepositoryException.class)
    public void transform_NonDepositRecord() throws Exception {
        Model bxc3Model = createModelWithTypes(bxc3Pid, ContentModel.CONTAINER);

        Document foxml = new FoxmlDocumentBuilder(bxc3Pid, "work")
                .relsExtModel(bxc3Model)
                .build();
        serializeFoxml(bxc3Pid, foxml);

        updatePathIndex();

        transformer.compute();
    }

    @Test
    public void transform_DepositRecord() throws Exception {
        Model bxc3Model = createModelWithTypes(bxc3Pid, ContentModel.DEPOSIT_RECORD);

        Document foxml = new FoxmlDocumentBuilder(bxc3Pid, "Migrated Deposit Record")
                .relsExtModel(bxc3Model)
                .build();
        serializeFoxml(bxc3Pid, foxml);

        addPremisLog(bxc3Pid);

        updatePathIndex();

        transformer.compute();

        DepositRecord depRec = repoObjLoader.getDepositRecord(bxc5Pid);
        Resource recResc = depRec.getResource();

        assertTrue("Did not have deposit record type", recResc.hasProperty(RDF.type, Cdr.DepositRecord));
        assertTrue(recResc.hasProperty(DC.title, "Migrated Deposit Record"));
        assertEquals(DEFAULT_CREATED_DATE, depRec.getCreatedDate());
        assertEquals(DEFAULT_MODIFIED_DATE, depRec.getLastModified());

        assertPremisTransformed(depRec);
    }

    @Test
    public void transform_DepositRecord_WithDepositProperties() throws Exception {
        Model bxc3Model = createModelWithTypes(bxc3Pid, ContentModel.DEPOSIT_RECORD);
        Resource bxc3Resc = bxc3Model.getResource(toBxc3Uri(bxc3Pid));
        bxc3Resc.addLiteral(CDRProperty.depositedOnBehalfOf.getProperty(), "some depositor");
        bxc3Resc.addLiteral(CDRProperty.depositMethod.getProperty(), "dep method");
        bxc3Resc.addLiteral(CDRProperty.depositPackageType.getProperty(), "mets package");
        bxc3Resc.addLiteral(CDRProperty.depositPackageSubType.getProperty(), "package subtype");

        Document foxml = new FoxmlDocumentBuilder(bxc3Pid, "Migrated Deposit Record")
                .relsExtModel(bxc3Model)
                .build();
        serializeFoxml(bxc3Pid, foxml);

        addPremisLog(bxc3Pid);

        updatePathIndex();

        transformer.compute();

        DepositRecord depRec = repoObjLoader.getDepositRecord(bxc5Pid);
        System.out.println(depRec.getModel());
        Resource recResc = depRec.getResource();

        assertTrue("Did not have deposit record type", recResc.hasProperty(RDF.type, Cdr.DepositRecord));
        assertTrue(recResc.hasProperty(DC.title, "Migrated Deposit Record"));
        assertEquals(DEFAULT_CREATED_DATE, depRec.getCreatedDate());
        assertEquals(DEFAULT_MODIFIED_DATE, depRec.getLastModified());

        assertPremisTransformed(depRec);

        assertTrue(recResc.hasProperty(Cdr.depositedOnBehalfOf, "some depositor"));
        assertTrue(recResc.hasProperty(Cdr.depositMethod, "dep method"));
        assertTrue(recResc.hasProperty(Cdr.depositPackageType, "mets package"));
        assertTrue(recResc.hasProperty(Cdr.depositPackageProfile, "package subtype"));
    }

    @Test
    public void transform_DepositRecord_withManifests() throws Exception {
        Model bxc3Model = createModelWithTypes(bxc3Pid, ContentModel.DEPOSIT_RECORD);

        String manifest0Name = "DATA_MANIFEST0";
        String manifest0Content = "content for m1";
        writeManifestFile(bxc3Pid, manifest0Name, manifest0Content);
        DatastreamVersion manifest0 = new DatastreamVersion(null,
                manifest0Name, "0",
                FoxmlDocumentBuilder.DEFAULT_CREATED_DATE,
                Integer.toString(manifest0Content.length()),
                "text/xml",
                null);

        String manifest1Name = "DATA_MANIFEST1";
        String manifest1Content = "additional content";
        writeManifestFile(bxc3Pid, manifest1Name, manifest1Content);
        DatastreamVersion manifest1 = new DatastreamVersion(null,
                manifest1Name, "0",
                FoxmlDocumentBuilder.DEFAULT_LAST_MODIFIED,
                Integer.toString(manifest1Content.length()),
                "text/plain",
                null);

        Document foxml = new FoxmlDocumentBuilder(bxc3Pid, "Deposit Record with Manifests")
                .relsExtModel(bxc3Model)
                .withDatastreamVersion(manifest0)
                .withDatastreamVersion(manifest1)
                .build();
        serializeFoxml(bxc3Pid, foxml);

        addPremisLog(bxc3Pid);

        updatePathIndex();

        transformer.compute();

        DepositRecord depRec = repoObjLoader.getDepositRecord(bxc5Pid);
        Resource recResc = depRec.getResource();

        assertTrue("Did not have deposit record type", recResc.hasProperty(RDF.type, Cdr.DepositRecord));
        assertTrue(recResc.hasProperty(DC.title, "Deposit Record with Manifests"));
        assertEquals(DEFAULT_CREATED_DATE, depRec.getCreatedDate());
        assertEquals(DEFAULT_MODIFIED_DATE, depRec.getLastModified());

        assertPremisTransformed(depRec);

        List<BinaryObject> binList = depRec.listManifests().stream()
                .map(repoObjLoader::getBinaryObject)
                .collect(toList());

        BinaryObject manifest0Bin = getManifestByName(binList, manifest0Name);
        assertManifestDetails(DEFAULT_CREATED_DATE,
                "text/xml",
                manifest0Content,
                manifest0Bin);

        BinaryObject manifest1Bin = getManifestByName(binList, manifest1Name);
        assertManifestDetails(DEFAULT_MODIFIED_DATE,
                "text/plain",
                manifest1Content,
                manifest1Bin);
    }

    private BinaryObject getManifestByName(List<BinaryObject> binList, String dsName) {
        return binList.stream()
                .filter(b -> b.getResource().hasLiteral(DC.title, dsName))
                .findFirst()
                .get();
    }

    private void assertManifestDetails(Date timestamp, String mimetype,
            String content, BinaryObject manifestBin) throws IOException {
        assertEquals(DEFAULT_CREATED_DATE, manifestBin.getLastModified());
        assertEquals(DEFAULT_CREATED_DATE, manifestBin.getCreatedDate());
        assertEquals("text/xml", manifestBin.getMimetype());
        assertEquals(content, IOUtils.toString(manifestBin.getBinaryStream(), UTF_8));
    }

    private void addPremisLog(PID originalPid) throws IOException {
        Document premisDoc = createPremisDoc(originalPid);
        String detail = "virus scan";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", INITIATOR_ROLE, "virusscanner");

        String premisDsName = "uuid_" + originalPid.getId() + "+" + PREMIS_DS + "+" + PREMIS_DS + ".0";
        Path xmlPath = datastreamsPath.resolve(premisDsName);
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(premisDoc, outStream);
    }

    private void assertPremisTransformed(DepositRecord depRec) throws IOException {
        PremisLogger premisLog = depRec.getPremisLog();
        Model eventsModel = premisLog.getEventsModel();
        List<Resource> eventRescs = listEventResources(depRec.getPid(), eventsModel);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEquals("Event type did not match expected value",
                Premis.VirusCheck, eventResc.getPropertyResourceValue(RDF.type));
    }

    // non deposit record DONE
    // no foxml DONE
    // no manifests DONE
    // with manifests DONE
    // no properties DONE
    // with premis

    private Path serializeFoxml(PID pid, Document doc) throws IOException {
        Path xmlPath = objectsPath.resolve("uuid_" + pid.getId() + ".xml");
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(doc, outStream);
        return xmlPath;
    }

    private Path writeManifestFile(PID pid, String dsName, String content) throws IOException {
        Path dsPath = datastreamsPath.resolve("uuid_" + pid.getId() + "+" + dsName + "+" + dsName + ".0");
        FileUtils.writeStringToFile(dsPath.toFile(), content, UTF_8);
        return dsPath;
    }

    private Model createModelWithTypes(PID pid, ContentModel... models) {
        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(pid));
        for (ContentModel contentModel : models) {
            resc.addProperty(hasModel.getProperty(), contentModel.getResource());
        }
        return model;
    }

    private void updatePathIndex() {
        pathIndexingService.indexObjectsFromPath(objectsPath);
        pathIndexingService.indexDatastreamsFromPath(datastreamsPath);
    }
}
