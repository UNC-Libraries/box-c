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
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.MODS_DS;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.ORIGINAL_DS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static java.nio.file.Files.newOutputStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
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
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;

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

    private ContentObjectTransformerManager factory;

    private DepositModelManager modelManager;

    private DepositDirectoryManager directoryManager;

    private RepositoryPIDMinter pidMinter;

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

        depositPid = pidMinter.mintDepositRecordPid();
        modelManager = new DepositModelManager(depositPid, tdbDir.toString());
        directoryManager = new DepositDirectoryManager(depositPid, depositBasePath, false);

        factory = new ContentObjectTransformerManager();
        factory.setPathIndex(pathIndex);
        factory.setModelManager(modelManager);
        factory.setPidMinter(pidMinter);
        factory.setTopLevelAsUnit(true);
        factory.setDirectoryManager(directoryManager);
    }

    @Test
    public void transformFolderWithNoChildren() throws Exception {
        PID folderPid = makePid();

        Model model = createContainerModel(folderPid);

        Document foxml = new FoxmlDocumentBuilder(folderPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, folderPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(folderPid, folderPid, Cdr.Folder);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(folderPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
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
        PID folderPid = makePid();
        Model model = createContainerModel(folderPid);
        addContains(model, folderPid, child1Pid);
        addContains(model, folderPid, child2Pid);
        Document foxml = new FoxmlDocumentBuilder(folderPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, folderPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(folderPid, folderPid, Cdr.Folder);
        transformer.fork();

        int result = factory.awaitTransformers();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag parentBag = depModel.getBag(folderPid.getRepositoryPath());

        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(parentBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(parentBag.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));

        Resource child1Resc = depModel.getResource(child1Pid.getRepositoryPath());
        Resource child2Resc = depModel.getResource(child2Pid.getRepositoryPath());
        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(2, bagChildren.size());
        assertTrue(bagChildren.contains(child1Resc));
        assertTrue(bagChildren.contains(child2Resc));

        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.Folder));
        assertTrue(child2Resc.hasProperty(RDF.type, Cdr.Folder));
    }

    @Test
    public void transformFolderWithChildGenerateIds() throws Exception {
        // Enable id generation
        factory.setGenerateIds(true);

        // Create the children objects' foxml
        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "work child")
                .relsExtModel(createContainerModel(child1Pid, AGGREGATE_WORK))
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        // Create the parent's foxml
        PID folderPid = makePid();
        Model model = createContainerModel(folderPid);
        addContains(model, folderPid, child1Pid);
        Document foxml = new FoxmlDocumentBuilder(folderPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, folderPid, foxml);

        ContentTransformationService service = new ContentTransformationService(depositPid, folderPid.getId(), false);
        service.setModelManager(modelManager);
        service.setTransformerManager(factory);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag depositBag = depModel.getBag(depositPid.getRepositoryPath());
        List<RDFNode> depChildren = depositBag.iterator().toList();
        assertEquals(1, depChildren.size());

        Bag parentBag = depModel.getBag(depChildren.get(0).asResource());
        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Folder));
        assertNotEquals(folderPid.getRepositoryPath(), parentBag.getURI());

        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource child1Resc = (Resource) bagChildren.get(0);

        assertNotEquals(child1Pid.getRepositoryPath(), child1Resc.getURI());
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.Work));
    }

    @Test
    public void transformWorkWithNoChildren() throws Exception {
        PID workPid = makePid();

        Model model = createContainerModel(workPid, AGGREGATE_WORK);

        Document foxml = new FoxmlDocumentBuilder(workPid, "work").relsExtModel(model).build();
        serializeFoxml(objectsPath, workPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(workPid, workPid, Cdr.Folder);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(workPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Work));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
    }

    @Test
    public void transformWorkWithMods() throws Exception {
        PID workPid = makePid();

        Model model = createContainerModel(workPid, AGGREGATE_WORK);

        Document foxml = new FoxmlDocumentBuilder(workPid, "work")
                .relsExtModel(model)
                .withDatastreamVersion(makeModsDatastream("My Work"))
                .build();
        serializeFoxml(objectsPath, workPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(workPid, Cdr.Folder);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(workPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Work));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));

        assertMods(workPid, "My Work");
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

        PID workPid = makePid();
        Model model = createContainerModel(workPid, AGGREGATE_WORK);
        addContains(model, workPid, child1Pid);
        addRelationship(model, workPid, defaultWebObject.getProperty(), child1Pid);

        Document foxml = new FoxmlDocumentBuilder(workPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, workPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(workPid, workPid, Cdr.Folder);
        transformer.fork();

        int result = factory.awaitTransformers();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag workBag = depModel.getBag(workPid.getRepositoryPath());
        Resource child1Resc = depModel.getResource(child1Pid.getRepositoryPath());

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(workBag.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        assertTrue(workBag.hasProperty(Cdr.primaryObject, child1Resc));

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
    }

    @Test
    public void transformWorkWithFileWithGeneratedIds() throws Exception {
        factory.setGenerateIds(true);

        PID child1Pid = makePid();
        Document foxml1 = new FoxmlDocumentBuilder(child1Pid, "file1")
                .relsExtModel(createFileModel(child1Pid))
                .withDatastreamVersion(createDataFileVersion())
                .build();
        serializeFoxml(objectsPath, child1Pid, foxml1);

        PID workPid = makePid();
        Model model = createContainerModel(workPid, AGGREGATE_WORK);
        addContains(model, workPid, child1Pid);
        addRelationship(model, workPid, defaultWebObject.getProperty(), child1Pid);

        Document foxml = new FoxmlDocumentBuilder(workPid, "work")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, workPid, foxml);

        ContentTransformationService service = new ContentTransformationService(depositPid, workPid.getId(), false);
        service.setModelManager(modelManager);
        service.setTransformerManager(factory);

        int result = service.perform();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag depositBag = depModel.getBag(depositPid.getRepositoryPath());
        List<RDFNode> depChildren = depositBag.iterator().toList();
        assertEquals(1, depChildren.size());

        Bag parentBag = depModel.getBag(depChildren.get(0).asResource());
        assertTrue(parentBag.hasProperty(RDF.type, Cdr.Work));
        assertNotEquals(workPid.getRepositoryPath(), parentBag.getURI());

        List<RDFNode> bagChildren = parentBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource child1Resc = (Resource) bagChildren.get(0);

        assertNotEquals(child1Pid.getRepositoryPath(), child1Resc.getURI());
        assertTrue(child1Resc.hasProperty(RDF.type, Cdr.FileObject));
    }

    @Test
    public void transformStandaloneFile() throws Exception {
        // Give the object a separate created time from its data file
        String objectCreated = "2011-10-01T11:11:11.111Z";
        PID originalPid = makePid();
        Path dataFilePath = mockDatastreamFile(originalPid, ORIGINAL_DS, 0);
        Document foxml1 = new FoxmlDocumentBuilder(originalPid, "file1")
                .relsExtModel(createFileModel(originalPid))
                .withDatastreamVersion(createDataFileVersion())
                .createdDate(objectCreated)
                .withDatastreamVersion(makeModsDatastream("My File"))
                .build();
        serializeFoxml(objectsPath, originalPid, foxml1);

        ContentObjectTransformer transformer = factory.createTransformer(originalPid, originalPid, Cdr.Folder);
        transformer.fork();

        int result = factory.awaitTransformers();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        // original pid should now refer to a newly generated work
        Bag workBag = depModel.getBag(originalPid.getRepositoryPath());
        List<RDFNode> bagChildren = workBag.iterator().toList();
        assertEquals(1, bagChildren.size());
        Resource fileResc = (Resource) bagChildren.get(0);

        // Verify work properties
        assertTrue(workBag.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workBag.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue("Generated work should have inherited the object creation time from the file",
                workBag.hasProperty(CdrDeposit.createTime, objectCreated));
        assertTrue(workBag.hasProperty(Cdr.primaryObject, fileResc));

        // Verify file properties
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(fileResc.hasLiteral(CdrDeposit.md5sum, DATA_FILE_MD5));
        assertTrue("File should be using the creation time of the datastream",
                fileResc.hasLiteral(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
        // datastreams only have a created time
        assertTrue(fileResc.hasLiteral(CdrDeposit.lastModifiedTime, DEFAULT_CREATED_DATE));
        assertTrue(fileResc.hasLiteral(CdrDeposit.size, DATA_FILE_SIZE));
        assertTrue(fileResc.hasLiteral(CdrDeposit.stagingLocation, dataFilePath.toUri().toString()));

        // Work should have the MODS
        assertMods(originalPid, "My File");
    }

    @Test
    public void transformCollectionInUnit() throws Exception {
        PID collPid = makePid();

        Model model = createContainerModel(collPid, ContentModel.COLLECTION);

        Document foxml = new FoxmlDocumentBuilder(collPid, "collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, collPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(collPid, collPid, Cdr.AdminUnit);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(collPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Collection));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
    }

    @Test
    public void transformCollectionAtTopWithFlag() throws Exception {
        factory.setTopLevelAsUnit(true);

        PID collPid = makePid();

        Model model = createContainerModel(collPid, ContentModel.COLLECTION);

        Document foxml = new FoxmlDocumentBuilder(collPid, "top collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, collPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(collPid, collPid, null);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(collPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.AdminUnit));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
    }

    @Test
    public void transformCollectionAtTopWithFlagFalse() throws Exception {
        factory.setTopLevelAsUnit(false);

        PID collPid = makePid();

        Model model = createContainerModel(collPid, ContentModel.COLLECTION);

        Document foxml = new FoxmlDocumentBuilder(collPid, "top collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, collPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(collPid, collPid, null);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        Resource resc = depModel.getResource(collPid.getRepositoryPath());

        assertTrue(resc.hasProperty(RDF.type, Cdr.Collection));
        assertTrue(resc.hasProperty(CdrDeposit.lastModifiedTime, DEFAULT_LAST_MODIFIED));
        assertTrue(resc.hasProperty(CdrDeposit.createTime, DEFAULT_CREATED_DATE));
    }

    @Test
    public void transformNonTransformableType() throws Exception {
        PID pid = makePid();

        Model model = createDefaultModel();
        Resource resc = model.getResource(toBxc3Uri(pid));
        resc.addProperty(hasModel.getProperty(), DEPOSIT_RECORD.getResource());

        Document foxml = new FoxmlDocumentBuilder(pid, "collection")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, pid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(pid, pid, Cdr.AdminUnit);
        transformer.invoke();

        Model depModel = modelManager.getReadModel();
        assertTrue(depModel.isEmpty());
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
        PID folderPid = makePid();
        Model model = createContainerModel(folderPid);
        addContains(model, folderPid, child1Pid);
        Document foxml = new FoxmlDocumentBuilder(folderPid, "folder")
                .relsExtModel(model)
                .state("Deleted")
                .build();
        serializeFoxml(objectsPath, folderPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(folderPid, folderPid, Cdr.Folder);
        transformer.fork();

        int result = factory.awaitTransformers();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        assertTrue("No properties should be present, for either the parent or child",
                depModel.isEmpty());
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
        PID folderPid = makePid();
        Model model = createContainerModel(folderPid);
        addContains(model, folderPid, missingPid);
        addContains(model, folderPid, child2Pid);
        Document foxml = new FoxmlDocumentBuilder(folderPid, "folder")
                .relsExtModel(model)
                .build();
        serializeFoxml(objectsPath, folderPid, foxml);

        ContentObjectTransformer transformer = factory.createTransformer(folderPid, folderPid, Cdr.Folder);
        transformer.fork();

        int result = factory.awaitTransformers();
        assertEquals(0, result);

        Model depModel = modelManager.getReadModel();
        Bag parentBag = depModel.getBag(folderPid.getRepositoryPath());

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

    private DatastreamVersion createDataFileVersion(String md5, int version, String created, String size, String mimeType) {
        return new DatastreamVersion(md5, ORIGINAL_DS, ORIGINAL_DS + "." + version, created, size, mimeType);
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
        DatastreamVersion modsDs = new DatastreamVersion(null, MODS_DS, MODS_DS + ".0", DEFAULT_CREATED_DATE, "100", "text/xml");
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
}
