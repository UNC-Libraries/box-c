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
package edu.unc.lib.dcr.migration.content;

import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.AGGREGATE_WORK;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.CONTAINER;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.DEPOSIT_RECORD;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.PRESERVEDOBJECT;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.SIMPLE;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder.DEFAULT_CREATED_DATE;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder.DEFAULT_LAST_MODIFIED;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.DC_DS;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.MODS_DS;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.ORIGINAL_DS;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.createPremisDoc;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.deserializeLogFile;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.listEventResources;
import static edu.unc.lib.dcr.migration.premis.TestPremisEventHelpers.serializeXMLFile;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static java.nio.file.Files.newOutputStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dcr.migration.deposit.DepositDirectoryManager;
import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Bxc3UserRole;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;

/**
 * @author bbpennel
 */
public class ContentObjectTransformerTest {
    private static final String DATA_FILE_MD5 = "a4443ebd04da89c898a448a48f71e7b3";
    private static final String DATA_FILE_MIMETYPE = "text/plain";
    private static final String DATA_FILE_SIZE = "104";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private Path objectsPath;

    private Path datastreamsPath;

    private Path depositBasePath;

    private PID depositPid;

    private PID startingPid;

    private ContentObjectTransformerManager manager;

    private ContentTransformationService service;

    private DepositModelManager modelManager;

    private DepositDirectoryManager directoryManager;

    private RepositoryPIDMinter pidMinter;

    private PremisLoggerFactory premisLoggerFactory;

    @Mock
    private PathIndex pathIndex;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        datastreamsPath = tmpFolder.newFolder("datastreams").toPath();
        objectsPath = tmpFolder.newFolder("objects").toPath();
        File tdbDir = tmpFolder.newFolder("tdb");
        depositBasePath = tmpFolder.newFolder("deposits").toPath();

        pidMinter = new RepositoryPIDMinter();

        startingPid = pidMinter.mintContentPid();

        depositPid = pidMinter.mintDepositRecordPid();
        modelManager = new DepositModelManager(depositPid, tdbDir.toString());
        directoryManager = new DepositDirectoryManager(depositPid, depositBasePath, false);

        premisLoggerFactory = new PremisLoggerFactory();

        manager = new ContentObjectTransformerManager();
        manager.setPathIndex(pathIndex);
        manager.setModelManager(modelManager);
        manager.setPidMinter(pidMinter);
        manager.setTopLevelAsUnit(true);
        manager.setDirectoryManager(directoryManager);
        manager.setPremisLoggerFactory(premisLoggerFactory);

        service = new ContentTransformationService(depositPid, startingPid.getId(), true);
        service.setModelManager(modelManager);
        service.setTransformerManager(manager);
    }

    @Test
    public void transformFolderWithNoChildren() throws Exception {
        Model model = createContainerModel(startingPid);
        addPatronAccess(model, startingPid);
        addStaffRoles(model, startingPid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(resc.hasProperty(CdrDeposit.label, "folder"));

        assertHasPatronAccess(resc);
        // Cannot assign staff roles to folders
        assertNoStaffRoles(resc);
    }

    @Test
    public void transformFolderWithChildren() throws Exception {
        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "child1")
                .relsExtModel(createContainerModel(child1Pid))
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        PID child2Pid = makePid();
        Document foxml2 = new FoxmlDocumentBuilder(child2Pid, "child2")
                .relsExtModel(createContainerModel(child2Pid))
                .build();
        serializeFoxml(objectsPath, child2Pid, foxml2);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid);
        addContains(model, startingPid, child1Pid);
        addContains(model, startingPid, child2Pid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag parentBag = depModel.getBag(startingPid.getRepositoryPath());

        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(parentBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(parentBag.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(parentBag.hasProperty(CdrDeposit.label, "folder"));

        Resource child1Resc = depModel.getResource(child1Pid.getRepositoryPath());
        Resource child2Resc = depModel.getResource(child2Pid.getRepositoryPath());
        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(2, bagChildren.size());
        assertTrue(bagChildren.contains(child1Resc));
        assertTrue(bagChildren.contains(child2Resc));

        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(child1Resc.hasProperty(CdrDeposit.label, "child1"));
        assertTrue(child2Resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(child2Resc.hasProperty(CdrDeposit.label, "child2"));
    }

    @Test
    public void transformFolderWithChildGenerateIds() throws Exception {
        // Enable id generation
        manager.setGenerateIds(true);

        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "work child")
                .relsExtModel(createContainerModel(child1Pid, AGGREGATE_WORK))
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid);
        addContains(model, startingPid, child1Pid);
        addPatronAccess(model, startingPid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag depositBag = depModel.getBag(depositPid.getRepositoryPath());
        List<RDFNode> depChildren = depositBag.iterator().toList();
        assertEquals(1, depChildren.size());

        Bag parentBag = depModel.getBag(depChildren.get(0).asResource());
        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertNotEquals(startingPid.getRepositoryPath(), parentBag.getURI());
        assertHasPatronAccess(parentBag);

        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource child1Resc = (Resource) bagChildren.get(0);

        assertNotEquals(child1Pid.getRepositoryPath(), child1Resc.getURI());
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.Work));
    }

    @Test
    public void transformFolderWithDeletedChild() throws Exception {
        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "child1")
                .relsExtModel(createContainerModel(child1Pid))
                .state("Deleted")
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        PID child2Pid = makePid();
        Document foxml2 = new FoxmlDocumentBuilder(child2Pid, "child2")
                .relsExtModel(createContainerModel(child2Pid))
                .build();
        serializeFoxml(objectsPath, child2Pid, foxml2);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid);
        addContains(model, startingPid, child1Pid);
        addContains(model, startingPid, child2Pid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag parentBag = depModel.getBag(startingPid.getRepositoryPath());

        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(parentBag.hasProperty(CdrDeposit.label, "folder"));

        Resource child2Resc = depModel.getResource(child2Pid.getRepositoryPath());
        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        assertTrue(bagChildren.contains(child2Resc));

        assertTrue(child2Resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(child2Resc.hasProperty(CdrDeposit.label, "child2"));
    }

    @Test
    public void transformFolderWithPremis() throws Exception {
        addPremisLog(startingPid);

        Model model = createContainerModel(startingPid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(resc.hasProperty(CdrDeposit.label, "folder"));

        assertPremisTransformed(startingPid);
    }

    @Test
    public void transformWorkWithNoChildren() throws Exception {
        Model model = createContainerModel(startingPid, AGGREGATE_WORK);
        addPatronAccess(model, startingPid);
        addStaffRoles(model, startingPid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Work));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(resc.hasProperty(CdrDeposit.label, "work"));

        assertHasPatronAccess(resc);
        assertNoStaffRoles(resc);
    }

    @Test
    public void transformWorkWithMods() throws Exception {
        Model model = createContainerModel(startingPid, AGGREGATE_WORK);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "work")
                .relsExtModel(model)
                .withDatastreamVersion(makeModsDatastream("My Work"))
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Work));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(resc.hasProperty(CdrDeposit.label, "work"));

        assertMods(startingPid, "My Work");
    }

    @Test
    public void transformWorkWithFile() throws Exception {
        PID child1Pid = makePid();
        Path dataFilePath = mockDatastreamFile(child1Pid, ORIGINAL_DS, 0);
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "file1")
                .relsExtModel(createFileModel(child1Pid))
                .withDatastreamVersion(createDataFileVersion())
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        Model model = createContainerModel(startingPid, AGGREGATE_WORK);
        addContains(model, startingPid, child1Pid);
        addRelationship(model, startingPid, defaultWebObject.getProperty(), child1Pid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag workBag = depModel.getBag(startingPid.getRepositoryPath());
        Resource child1Resc = depModel.getResource(child1Pid.getRepositoryPath());

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(workBag.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(workBag.hasProperty(Cdr.primaryObject, child1Resc));
        assertTrue(workBag.hasProperty(CdrDeposit.label, "work"));

        // Verify file properties
        List<RDFNode> bagChildren = workBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        assertTrue(bagChildren.contains(child1Resc));
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(child1Resc.hasLiteral(CdrDeposit.md5sum, DATA_FILE_MD5));
        assertTrue(child1Resc.hasLiteral(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        // datastreams only have a created time
        assertTrue(child1Resc.hasLiteral(CdrDeposit.lastModifiedTime, DEFAULT_CREATED_DATE));
        assertTrue(child1Resc.hasLiteral(CdrDeposit.size, DATA_FILE_SIZE));
        assertTrue(child1Resc.hasLiteral(CdrDeposit.stagingLocation, dataFilePath.toUri().toString()));
        assertTrue(child1Resc.hasProperty(CdrDeposit.label, "file1"));
    }

    @Test
    public void transformWorkWithFileWithGeneratedIds() throws Exception {
        manager.setGenerateIds(true);

        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "file1")
                .relsExtModel(createFileModel(child1Pid))
                .withDatastreamVersion(createDataFileVersion())
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        addPremisLog(startingPid);

        Model model = createContainerModel(startingPid, AGGREGATE_WORK);
        addContains(model, startingPid, child1Pid);
        addRelationship(model, startingPid, defaultWebObject.getProperty(), child1Pid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag depositBag = depModel.getBag(depositPid.getRepositoryPath());
        List<RDFNode> depChildren = depositBag.iterator().toList();
        assertEquals(1, depChildren.size());

        Bag parentBag = depModel.getBag(depChildren.get(0).asResource());
        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Work));
        assertNotEquals(startingPid.getRepositoryPath(), parentBag.getURI());

        assertPremisTransformed(PIDs.get(parentBag.getURI()));

        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource child1Resc = (Resource) bagChildren.get(0);

        assertNotEquals(child1Pid.getRepositoryPath(), child1Resc.getURI());
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.FileObject));
    }

    @Test
    public void transformWorkWithFileNoDcTitle() throws Exception {
        addPremisLog(startingPid);

        PID child1Pid = makePid();
        Path dataFilePath = mockDatastreamFile(child1Pid, ORIGINAL_DS, 0);
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "label.txt")
                .relsExtModel(createFileModel(child1Pid))
                .withDatastreamVersion(createDataFileVersion())
                // remove dc datastream
                .withDatastreamVersions(DC_DS, null)
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        Model model = createContainerModel(startingPid, AGGREGATE_WORK);
        addContains(model, startingPid, child1Pid);
        addRelationship(model, startingPid, defaultWebObject.getProperty(), child1Pid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag workBag = depModel.getBag(startingPid.getRepositoryPath());
        Resource child1Resc = depModel.getResource(child1Pid.getRepositoryPath());

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.label, "work"));

        assertPremisTransformed(startingPid);

        // Verify file properties
        List<RDFNode> bagChildren = workBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        assertTrue(bagChildren.contains(child1Resc));
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(child1Resc.hasLiteral(CdrDeposit.stagingLocation, dataFilePath.toUri().toString()));
        assertTrue(child1Resc.hasProperty(CdrDeposit.label, "label.txt"));
    }

    @Test
    public void transformStandaloneFile() throws Exception {
        addPremisLog(startingPid);

        // Give the object a separate created time from its data file
        String objectCreated = "2011-10-01T11:11:11.111Z";
        Path dataFilePath = mockDatastreamFile(startingPid, ORIGINAL_DS, 0);
        Model bxc3Model = createFileModel(startingPid);
        addPatronAccess(bxc3Model, startingPid);
        Document foxml1 = new FoxmlDocumentBuilder(startingPid, "file1")
                .relsExtModel(bxc3Model)
                .withDatastreamVersion(createDataFileVersion())
                .createdDate(objectCreated)
                .withDatastreamVersion(makeModsDatastream("My File"))
                .build();
        serializeFoxml(objectsPath, startingPid, foxml1);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        // original pid should now refer to a newly generated work
        Bag workBag = depModel.getBag(startingPid.getRepositoryPath());
        List<RDFNode> bagChildren = workBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource fileResc = (Resource) bagChildren.get(0);

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue("Generated work should have inherited the object creation time from the file",
                workBag.hasProperty(CdrDeposit.createTime, objectCreated));
        assertTrue(workBag.hasProperty(Cdr.primaryObject, fileResc));
        assertTrue(workBag.hasProperty(CdrDeposit.label, "file1"));
        assertHasPatronAccess(workBag);

        // Verify file properties
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(fileResc.hasLiteral(CdrDeposit.md5sum, DATA_FILE_MD5));
        assertTrue("File should be using the creation time of the datastream",
                fileResc.hasLiteral(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        // datastreams only have a created time
        assertTrue(fileResc.hasLiteral(CdrDeposit.lastModifiedTime, DEFAULT_CREATED_DATE));
        assertTrue(fileResc.hasLiteral(CdrDeposit.size, DATA_FILE_SIZE));
        assertTrue(fileResc.hasLiteral(CdrDeposit.stagingLocation, dataFilePath.toUri().toString()));
        assertTrue(fileResc.hasProperty(CdrDeposit.label, "file1"));

        // Work should have the MODS
        assertMods(startingPid, "My File");

        // Original premis must stay with file
        assertPremisTransformed(PIDs.get(fileResc.getURI()));
        // ACLs should now be on the work
        assertNoPatronAccess(fileResc);
    }

    @Test
    public void transformStandaloneFileWithAltId() throws Exception {
        // Give the object a separate created time from its data file
        Path dataFilePath = mockDatastreamFile(startingPid, ORIGINAL_DS, 0);
        DatastreamVersion dataVersion = createDataFileVersion();
        dataVersion.setAltIds("irods://localhost:12345/test/path/to/test.txt");
        Document foxml1 = new FoxmlDocumentBuilder(startingPid, "")
                .relsExtModel(createFileModel(startingPid))
                .withDatastreamVersion(dataVersion)
                .withDatastreamVersions(DC_DS, null)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml1);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        // original pid should now refer to a newly generated work
        Bag workBag = depModel.getBag(startingPid.getRepositoryPath());
        List<RDFNode> bagChildren = workBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource fileResc = (Resource) bagChildren.get(0);

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.label, "test.txt"));

        // Verify file properties
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(fileResc.hasProperty(CdrDeposit.label, "test.txt"));
        assertTrue(fileResc.hasLiteral(CdrDeposit.stagingLocation, dataFilePath.toUri().toString()));
    }

    @Test
    public void transformNestedCollection() throws Exception {
        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "collection")
                .relsExtModel(createContainerModel(child1Pid, ContentModel.COLLECTION))
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid, ContentModel.COLLECTION);
        addContains(model, startingPid, child1Pid);
        addPatronAccess(model, startingPid);
        addStaffRoles(model, startingPid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "aspiring unit")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource unitResc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(unitResc.hasProperty(RDF.type, Cdr.AdminUnit));
        assertTrue(unitResc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(unitResc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(unitResc.hasProperty(CdrDeposit.label, "aspiring unit"));
        // patron ACLs should have migrated from unit to collection
        assertNoPatronAccess(unitResc);
        assertHasStaffRoles(unitResc);

        Resource collResc = depModel.getResource(child1Pid.getRepositoryPath());

        assertTrue(collResc.hasProperty(RDF.type, Cdr.Collection));
        assertTrue(collResc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(collResc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(collResc.hasProperty(CdrDeposit.label, "collection"));
        // Coll should now have the ACLs from the unit
        assertHasPatronAccess(collResc);
        assertNoStaffRoles(collResc);
    }

    @Test
    public void transformCollectionAtTopWithFlagFalse() throws Exception {
        manager.setTopLevelAsUnit(false);

        Model model = createContainerModel(startingPid, ContentModel.COLLECTION);
        addStaffRoles(model, startingPid);
        addPatronAccess(model, startingPid);

        Document foxml = new FoxmlDocumentBuilder(startingPid, "top collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        service.perform();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(startingPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Collection));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(resc.hasProperty(CdrDeposit.label, "top collection"));

        assertHasPatronAccess(resc);
        assertHasStaffRoles(resc);
    }

    @Test
    public void transformNonTransformableType() throws Exception {
        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(startingPid));
        resc.addProperty(hasModel.getProperty(), DEPOSIT_RECORD.getResource());

        Document foxml = new FoxmlDocumentBuilder(startingPid, "collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals("Transformation should contain failure", 1, result);

        Model depModel = modelManager.getReadModel();
        assertDoesNotContainSubject(depModel, startingPid);
    }

    @Test
    public void transformMarkedForDeletion() throws Exception {
        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "child1")
                .relsExtModel(createContainerModel(child1Pid))
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid);
        addContains(model, startingPid, child1Pid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .state("Deleted")
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        assertDoesNotContainSubject(depModel, startingPid);
    }

    @Test
    public void transformFolderWithMissingChild() throws Exception {
        // Missing child pid
        PID missingPid = makePid();

        PID child2Pid = makePid();
        Document foxml2 = new FoxmlDocumentBuilder(child2Pid, "child2")
                .relsExtModel(createContainerModel(child2Pid))
                .build();
        serializeFoxml(objectsPath, child2Pid, foxml2);

        // Create the parent's foxml
        Model model = createContainerModel(startingPid);
        addContains(model, startingPid, missingPid);
        addContains(model, startingPid, child2Pid);
        Document foxml = new FoxmlDocumentBuilder(startingPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, startingPid, foxml);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag parentBag = depModel.getBag(startingPid.getRepositoryPath());

        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(parentBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(parentBag.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));

        Resource missingResc = depModel.getResource(missingPid.getRepositoryPath());
        Resource child2Resc = depModel.getResource(child2Pid.getRepositoryPath());
        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        assertFalse(bagChildren.contains(missingResc));
        assertTrue(bagChildren.contains(child2Resc));

        assertTrue(child2Resc.hasProperty(RDF.type, Cdr.Folder));
    }

    private Model createContainerModel(PID pid, ContentModel... models) {
        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(pid));
        resc.addProperty(hasModel.getProperty(), CONTAINER.getResource());
        for (ContentModel contentModel : models) {
            resc.addProperty(hasModel.getProperty(), contentModel.getResource());
        }
        return model;
    }

    private Model createFileModel(PID pid, ContentModel... models) {
        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(pid));
        resc.addProperty(hasModel.getProperty(), SIMPLE.getResource());
        resc.addProperty(hasModel.getProperty(), PRESERVEDOBJECT.getResource());
        for (ContentModel contentModel : models) {
            resc.addProperty(hasModel.getProperty(), contentModel.getResource());
        }
        return model;
    }

    private void addContains(Model model, PID parent, PID child) {
        addRelationship(model, parent, contains.getProperty(), child);
    }

    private void addRelationship(Model model, PID parent, Property prop, PID child) {
        Resource parentResc = model.getResource(toBxc3Uri(parent));
        parentResc.addProperty(prop, model.getResource(toBxc3Uri(child)));
    }

    private DatastreamVersion createDataFileVersion() {
        return createDataFileVersion(DATA_FILE_MD5, 0, DEFAULT_CREATED_DATE, DATA_FILE_SIZE, DATA_FILE_MIMETYPE);
    }

    private DatastreamVersion createDataFileVersion(String md5, int version, String created,
            String size, String mimeType) {
        return new DatastreamVersion(md5, ORIGINAL_DS, ORIGINAL_DS + "." + version, created, size, mimeType, null);
    }

    private Path mockDatastreamFile(PID pid, String dsName, int version) {
        Path path = datastreamsPath.resolve("uuid:" + pid.getId() + "+" + dsName + "+" + dsName + "." + version);
        when(pathIndex.getPath(pid, PathIndex.getFileType(dsName))).thenReturn(path);
        return path;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private DatastreamVersion makeModsDatastream(String title) {
        Document doc = new Document();
        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS)
                                .setText(title))));
        DatastreamVersion modsDs = new DatastreamVersion(null, MODS_DS, MODS_DS + ".0", DEFAULT_CREATED_DATE,
                "100", "text/xml", null);
        modsDs.setBodyEl(doc.getRootElement());
        return modsDs;
    }

    private Path serializeFoxml(Path destDir, PID pid, Document doc) throws IOException {
        Path xmlPath = destDir.resolve("uuid_" + pid.getId() + ".xml");
        OutputStream outStream = newOutputStream(xmlPath);
        new XMLOutputter().output(doc, outStream);
        when(pathIndex.getPath(pid)).thenReturn(xmlPath);
        return xmlPath;
    }

    private void assertMods(PID pid, String expectedTitle) throws Exception {
        Path modsPath = directoryManager.getDescriptionDir().resolve(pid.getId() + ".xml");
        assertTrue("MODS did not exist", Files.exists(modsPath));
        Document modsDoc = createSAXBuilder().build(modsPath.toFile());
        String resultTitle = modsDoc.getRootElement().getChild("titleInfo", MODS_V3_NS)
                .getChild("title", MODS_V3_NS)
                .getText();
        assertEquals("MODS title did not match", expectedTitle, resultTitle);
    }

    private void assertDoesNotContainSubject(Model model, PID pid) {
        assertFalse(model.listSubjects().toSet().contains(createResource(pid.getRepositoryPath())));
    }

    private void addPremisLog(PID originalPid) throws IOException {
        Document premisDoc = createPremisDoc(originalPid);
        String detail = "virus scan";
        Element eventEl = addEvent(premisDoc, VALIDATION_TYPE, detail, EVENT_DATE);
        addAgent(eventEl, "Name", INITIATOR_ROLE, "virusscanner");
        Path originalPremisPath = serializeXMLFile(tmpFolder.getRoot().toPath(), startingPid, premisDoc);

        when(pathIndex.getPath(originalPid, PathIndex.PREMIS_TYPE)).thenReturn(originalPremisPath);
    }

    private void assertPremisTransformed(PID originalPid) throws IOException {
        Path expectedTransformedPath = directoryManager.getPremisPath(originalPid);
        assertTrue(Files.exists(expectedTransformedPath));

        Model eventsModel = deserializeLogFile(expectedTransformedPath.toFile());
        List<Resource> eventRescs = listEventResources(originalPid, eventsModel);
        assertEquals(1, eventRescs.size());

        Resource eventResc = eventRescs.get(0);
        assertEquals("Event type did not match expected value",
                Premis.VirusCheck, eventResc.getPropertyResourceValue(RDF.type));
    }

    private void addPatronAccess(Model bxc3Model, PID startingPid) {
        Resource bxc3Resc = bxc3Model.getResource(toBxc3Uri(startingPid));
        bxc3Resc.addLiteral(Bxc3UserRole.metadataPatron.getProperty(), ACLTransformationHelpers.BXC3_PUBLIC_GROUP);
        bxc3Resc.addLiteral(Bxc3UserRole.patron.getProperty(), ACLTransformationHelpers.BXC3_AUTHENTICATED_GROUP);
    }

    private void assertHasPatronAccess(Resource bxc5Resc) {
        assertTrue(bxc5Resc.hasLiteral(CdrAcl.canViewMetadata, AccessPrincipalConstants.PUBLIC_PRINC));
        assertTrue(bxc5Resc.hasLiteral(CdrAcl.canViewOriginals, AccessPrincipalConstants.AUTHENTICATED_PRINC));
    }

    private void assertNoPatronAccess(Resource bxc5Resc) {
        List<Statement> publicRoles = bxc5Resc.getModel().listStatements(
                bxc5Resc, null, AccessPrincipalConstants.PUBLIC_PRINC).toList();
        assertTrue("Expected no roles for everyone group on " + bxc5Resc.getURI(), publicRoles.isEmpty());
        List<Statement> authRoles = bxc5Resc.getModel().listStatements(
                bxc5Resc, null, AccessPrincipalConstants.AUTHENTICATED_PRINC).toList();
        assertTrue("Expected no roles for authenticated group on " + bxc5Resc.getURI(), authRoles.isEmpty());
    }

    private void addStaffRoles(Model bxc3Model, PID startingPid) {
        Resource bxc3Resc = bxc3Model.getResource(toBxc3Uri(startingPid));
        bxc3Resc.addLiteral(Bxc3UserRole.curator.getProperty(), "admin_grp");
        bxc3Resc.addLiteral(Bxc3UserRole.observer.getProperty(), "viewer_grp");
    }

    private void assertHasStaffRoles(Resource bxc5Resc) {
        assertTrue(bxc5Resc.hasLiteral(CdrAcl.canManage, "admin_grp"));
        assertTrue(bxc5Resc.hasLiteral(CdrAcl.canAccess, "viewer_grp"));
    }

    private void assertNoStaffRoles(Resource bxc5Resc) {
        assertFalse(bxc5Resc.hasProperty(CdrAcl.canAccess));
        assertFalse(bxc5Resc.hasProperty(CdrAcl.canIngest));
        assertFalse(bxc5Resc.hasProperty(CdrAcl.canDescribe));
        assertFalse(bxc5Resc.hasProperty(CdrAcl.canManage));
        assertFalse(bxc5Resc.hasProperty(CdrAcl.canProcess));
        assertFalse(bxc5Resc.hasProperty(CdrAcl.unitOwner));
    }
}
