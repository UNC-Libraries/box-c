package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.normalize.NormalizeFileObjectsJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;

/**
 *
 * @author bbpennel
 *
 */
public class NormalizeFileObjectsJobTest extends AbstractDepositJobTest {

    private static final String FILENAME = "file.txt";

    private NormalizeFileObjectsJob job;

    private Model model;

    private Bag depBag;

    @Mock
    private PremisLogger mockPremisLogger;
    @Mock
    private PremisLoggerFactory mockPremisLoggerFactory;
    private PremisEventBuilder mockPremisEventBuilder;

    @Mock
    private PIDMinter pidMinter;

    @BeforeEach
    public void init() {
        job = new NormalizeFileObjectsJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "premisLoggerFactory", mockPremisLoggerFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "pidMinter", pidMinter);
        job.init();

        mockPremisEventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(mockPremisLoggerFactory.createPremisLogger(any(PID.class), any(File.class)))
                .thenReturn(mockPremisLogger);
        when(mockPremisLogger.buildEvent(any(Resource.class))).thenReturn(mockPremisEventBuilder);

        model = job.getWritableModel();
        depBag = model.createBag(depositPid.getRepositoryPath());

        when(pidMinter.mintContentPid()).thenAnswer(new Answer<PID>() {
            @Override
            public PID answer(InvocationOnMock invocation) throws Throwable {
                return makePid(CONTENT_BASE);
            }
        });
    }

    @Test
    public void fileObjectInWorkTest() throws Exception {
        Bag workBag = addContainer(Cdr.Work);

        Resource childResc = addFileObject(workBag);

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();

        // Check that no additional objects were added
        Map<String, Resource> map = getResourceMap(model);
        assertEquals(3, map.size(), "Number of objects in model must not change");

        assertNotNull(map.get(workBag.getURI()));
        assertNotNull(map.get(childResc.getURI()));
        var resultWorkBag = model.getBag(workBag.getURI());
        var resultChildResc = model.getResource(childResc.getURI());
        assertTrue(resultWorkBag.contains(resultChildResc));
        var resultWorkResc = model.getResource(workBag.getURI());
        assertFalse(resultWorkResc.hasProperty(Cdr.primaryObject));
    }

    @Test
    public void fileObjectInWorkWithPrimaryObjectTest() throws Exception {
        Bag workBag = addContainer(Cdr.Work);
        var workResc = workBag.asResource();
        Resource childResc = addFileObject(workBag);
        workResc.addProperty(Cdr.primaryObject, childResc);

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();

        // Check that no additional objects were added
        Map<String, Resource> map = getResourceMap(model);
        assertEquals(3, map.size(), "Number of objects in model must not change");

        assertNotNull(map.get(workBag.getURI()));
        assertNotNull(map.get(childResc.getURI()));
        var resultWorkResc = model.getResource(workResc.getURI());
        var resultWorkBag = model.getBag(workResc);
        var resultChildResc = model.getResource(childResc.getURI());
        assertTrue(resultWorkBag.contains(resultChildResc));
        assertTrue(resultWorkResc.hasProperty(Cdr.primaryObject));
    }

    @Test
    public void fileObjectInFolderTest() throws Exception {
        Bag folderBag = addContainer(Cdr.Folder);
        String folderUri = folderBag.getURI();

        Resource childResc = addFileObject(folderBag);
        childResc.addLiteral(CdrAcl.embargoUntil,
                model.createTypedLiteral(Calendar.getInstance()));

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();

        // Check that no additional objects were added
        Map<String, Resource> map = getResourceMap(model);
        assertEquals(4, map.size(), "Must be one more resource than originally added");

        // Verify that work was created
        Resource workResc = model.listResourcesWithProperty(RDF.type, Cdr.Work).next();
        assertNotNull(workResc);

        // Verify that the label was set
        assertEquals(FILENAME, workResc.getProperty(CdrDeposit.label).getString());

        folderBag = model.getBag(folderUri);
        // Verify that the folder contains the work, not the file
        assertEquals(folderBag.iterator().next().asResource(), workResc, "Folder must contain work");

        childResc = model.getResource(childResc.getURI());
        // Verify that ACL properties are transfered to work
        assertFalse(childResc.hasProperty(CdrAcl.embargoUntil));
        assertTrue(workResc.hasProperty(CdrAcl.embargoUntil));

        // Check that the work contains the fileObject
        assertEquals(model.getBag(workResc).iterator().next().asResource(), childResc, "Folder must contain work");

        // Work should not have primaryObject assigned
        assertFalse(workResc.hasProperty(Cdr.primaryObject));

        // Label still present on file object
        assertEquals(FILENAME, childResc.getProperty(CdrDeposit.label).getString());

        // Other properties still present on file object
        Resource origResc = DepositModelHelpers.getDatastream(childResc);
        assertTrue(origResc.hasProperty(CdrDeposit.stagingLocation));

        // Check that premis event was added
        verify(mockPremisLogger).buildEvent(eq(Premis.Creation));
    }

    @Test
    public void fileObjectWithDescriptionTest() throws Exception {
        Bag folderBag = addContainer(Cdr.Folder);

        Resource childResc = addFileObject(folderBag);
        PID childPid = PIDs.get(childResc.getURI());

        File childModsFile = job.getModsPath(childPid, true).toFile();
        childModsFile.createNewFile();

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();

        Resource workResc = model.listResourcesWithProperty(RDF.type, Cdr.Work).next();

        // Verify that the MODS file was renamed to match work uuid
        PID workPid = PIDs.get(workResc.getURI());
        File workModsFile = job.getModsPath(workPid).toFile();

        assertTrue(workModsFile.exists());
        assertFalse(childModsFile.exists());
    }

    @Test
    public void noFileObjectTest() throws Exception {
        Bag workBag = addContainer(Cdr.Work);

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();

        // Check that no additional objects were added
        Map<String, Resource> map = getResourceMap(model);
        assertEquals(2, map.size(), "Number of objects in model must not change");

        // Check that no objects changed
        Resource workResc = map.get(workBag.getURI());
        assertNotNull(workResc);
        assertTrue(workResc.hasProperty(RDF.type, Cdr.Work));
    }

    private Bag addContainer(Resource type) {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, type);

        depBag.add(objBag);

        return objBag;
    }

    private Resource addFileObject(Bag parent) {
        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        childResc.addProperty(CdrDeposit.label, FILENAME);
        URI stagingUri = Paths.get(depositDir.getAbsolutePath(), "path", FILENAME).toUri();

        Resource origResc = DepositModelHelpers.addDatastream(childResc);
        origResc.addProperty(CdrDeposit.stagingLocation, stagingUri.toString());

        parent.add(childResc);

        return childResc;
    }

    private Map<String, Resource> getResourceMap(Model model) {
        Map<String, Resource> map = new HashMap<>();

        ResIterator rescIt = model.listResourcesWithProperty(RDF.type);
        while (rescIt.hasNext()) {
            Resource resc = rescIt.next();
            map.put(resc.getURI(), resc);
        }

        return map;
    }
}
