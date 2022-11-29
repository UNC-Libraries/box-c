package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;

/**
 *
 * @author bbpennel
 *
 */
public class DepositRecordIT extends AbstractFedoraIT {

    @Test
    public void createDepositRecordTest() throws Exception {

        Model model = getDepositRecordModel();

        DepositRecord record = repoObjFactory.createDepositRecord(model);

        assertNotNull(record);

        assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getInvalidDepositRecord() throws Exception {
        PID pid = pidMinter.mintContentPid();
        // Create a dummy non-depositRecord object
        client.put(pid.getRepositoryUri()).perform().close();

        // Try (and fail) to retrieve it as a deposit record
        repoObjLoader.getDepositRecord(pid);
    }

    @Test
    public void getDepositRecord() throws Exception {
        Model model = getDepositRecordModel();

        PID pid = repoObjFactory.createDepositRecord(model).getPid();

        DepositRecord record = repoObjLoader.getDepositRecord(pid);

        assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
    }

    @Test
    public void addManifestsTest() throws Exception {

        Model model = getDepositRecordModel();

        DepositRecord record = repoObjFactory.createDepositRecord(model);

        String bodyString1 = "Manifest info";
        String filename1 = "manifest1.txt";
        String mimetype1 = "text/plain";
        Path manifestPath = createTempFile("manifest", ".txt");
        writeStringToFile(manifestPath.toFile(), bodyString1, UTF_8);
        BinaryObject manifest1 = record.addManifest(manifestPath.toUri(), filename1, mimetype1, null, null);

        assertNotNull(manifest1);
        assertEquals(filename1, manifest1.getFilename());
        assertEquals(mimetype1, manifest1.getMimetype());

        String bodyString2 = "Second manifest";
        String mimetype2 = "text/plain";
        String filename2 = "manifest2";
        Path manifestPath2 = createTempFile("manifest", ".txt");
        writeStringToFile(manifestPath2.toFile(), bodyString2, UTF_8);
        BinaryObject manifest2 = record.addManifest(manifestPath2.toUri(), filename2, mimetype2, null, null);

        assertNotNull(manifest2);

        // Verify that listing returns all the expected manifests
        Collection<PID> manifestPids = record.listManifests();
        assertEquals("Incorrect number of manifests retrieved", 2, manifestPids.size());

        assertTrue("Manifest1 was not listed", manifestPids.contains(manifest1.getPid()));
        assertTrue("Manifest2 was not listed", manifestPids.contains(manifest2.getPid()));

        String respString1 = new BufferedReader(new InputStreamReader(manifest1.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Manifest content did not match submitted value", bodyString1, respString1);

        // Verify that retrieving the manifest returns the correct object
        BinaryObject gotManifest2 = record.getManifest(manifest2.getPid());
        assertNotNull("Get manifest did not return", gotManifest2);
        assertEquals(mimetype2, gotManifest2.getMimetype());

        String respString2 = new BufferedReader(new InputStreamReader(manifest2.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Manifest content did not match submitted value", bodyString2, respString2);
    }

    @Test
    public void addObjectsTest() throws Exception {
        Model model = getDepositRecordModel();
        DepositRecord record = repoObjFactory.createDepositRecord(model);

        repoInitializer.initializeRepository();
        ContentRootObject rootObj = repoObjLoader.getContentRootObject(getContentRootPid());
        AdminUnit adminUnit = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(adminUnit);

        CollectionObject coll = repoObjFactory.createCollectionObject(modelWithOriginalDeposit(record));
        adminUnit.addMember(coll);
        WorkObject work = repoObjFactory.createWorkObject(modelWithOriginalDeposit(record));
        coll.addMember(work);

        treeIndexer.indexAll(baseAddress);

        // Collection and work should have been part of this deposit
        List<PID> depositedObjects = record.listDepositedObjects();
        assertEquals(2, depositedObjects.size());
        assertTrue(depositedObjects.contains(coll.getPid()));
        assertTrue(depositedObjects.contains(work.getPid()));
        assertFalse(depositedObjects.contains(adminUnit.getPid()));
    }

    private Model modelWithOriginalDeposit(DepositRecord record) {
        Model refDepositModel = ModelFactory.createDefaultModel();
        Resource subj = refDepositModel.getResource("");
        subj.addProperty(Cdr.originalDeposit, record.getResource());

        return refDepositModel;
    }

    private Model getDepositRecordModel() {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(RDF.type, Cdr.DepositRecord);

        return model;
    }
}
