package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.PcdmUse;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class FileObjectIT extends AbstractFedoraIT {

    private static final String origBodyString = "Original data";
    private static final String origFilename = "original.txt";
    private static final String origMimetype = "text/plain";
    private static final String origSha1Checksum = DigestUtils.sha1Hex(origBodyString);
    private static final String origMd5Checksum = DigestUtils.md5Hex(origBodyString);

    @BeforeEach
    public void init() throws Exception {
        createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
    }

    @Test
    public void createFileObjectTest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        assertNotNull(fileObj);
        assertObjectExists(fileObj.getPid());
    }

    @Test
    public void addOriginalFileTest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        // Prep file and add
        var fileUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(fileObj.getPid()));
        FileUtils.writeStringToFile(new File(fileUri), origBodyString, "UTF-8");
        BinaryObject origObj = fileObj.addOriginalFile(fileUri, origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        verifyOriginalFile(origObj);

        BinaryObject retrievedOrig = fileObj.getOriginalFile();
        verifyOriginalFile(retrievedOrig);
    }

    @Test
    public void getMultipleBinariesTest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        // Add the original
        var origUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(fileObj.getPid()));
        FileUtils.writeStringToFile(new File(origUri), origBodyString, "UTF-8");
        BinaryObject bObj2 = fileObj.addOriginalFile(origUri, origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        // Construct the derivative objects
        String textBodyString = "Extracted text";
        String textFilename = "extracted.txt";
        String textMimetype = "text/plain";
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        var fitsUri = storageLocationTestHelper.makeTestStorageUri(fitsPid);
        FileUtils.writeStringToFile(new File(fitsUri), textBodyString, "UTF-8");
        BinaryObject bObj1 = fileObj.addBinary(fitsPid, fitsUri, textFilename, textMimetype,
                null, RDF.type, PcdmUse.ExtractedText);
        assertNotNull(bObj1);

        // Retrieve the binary objects directly
        List<BinaryObject> binaries = fileObj.getBinaryObjects();

        assertEquals(2, binaries.size(), "Incorrect number of binaries added");

        // Find each of the created binaries by pid
        BinaryObject rObj1 = findBinaryByPid(binaries, bObj1.getPid());
        BinaryObject rObj2 = findBinaryByPid(binaries, bObj2.getPid());

        assertNotNull(rObj1);
        assertNotNull(rObj2);

        // Verify that binaries had correct data added
        verifyFile(rObj1, textFilename, textMimetype, textBodyString);
        verifyOriginalFile(rObj2);
    }

    @Test
    public void testGetBinaryByName() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        PID binPid = getOriginalFilePid(fileObj.getPid());
        var binUri = storageLocationTestHelper.makeTestStorageUri(binPid);
        FileUtils.writeStringToFile(new File(binUri), origBodyString, "UTF-8");
        fileObj.addBinary(binPid, binUri, origFilename, origMimetype, null, null, null);

        BinaryObject binObj = fileObj.getBinaryObject(DatastreamType.ORIGINAL_FILE.getId());
        verifyFile(binObj, origFilename, origMimetype, origBodyString);
    }

    @Test
    public void getNonFileObject() throws Exception {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            PID objPid = PIDs.get("uuid:" + UUID.randomUUID().toString());

            client.put(objPid.getRepositoryUri()).perform().close();

            repoObjLoader.getFileObject(objPid);
        });
    }

    @Test
    public void testGetParent() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(null);

        var filePid = pidMinter.mintContentPid();
        var origUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(filePid));
        FileUtils.writeStringToFile(new File(origUri), origBodyString, "UTF-8");
        FileObject fileObj = work.addDataFile(filePid, origUri, origFilename, origMimetype, origSha1Checksum,
                    origMd5Checksum, null);

        treeIndexer.indexAll(baseAddress);

        RepositoryObject parent = fileObj.getParent();
        assertEquals(parent.getPid(), work.getPid(), "Parent of the file must match the work it was created in");
    }

    private void verifyOriginalFile(BinaryObject origObj) {
        verifyFile(origObj, origFilename, origMimetype, origBodyString);
    }

    private void verifyFile(BinaryObject bObj, String filename, String mimetype, String bodyString) {
        assertEquals(filename, bObj.getFilename());
        assertEquals(mimetype, bObj.getMimetype());

        String respString = new BufferedReader(new InputStreamReader(bObj.getBinaryStream())).lines()
                .collect(Collectors.joining("\n"));
        assertEquals(bodyString, respString, "Original content did not match submitted value");
    }

    private BinaryObject findBinaryByPid(List<BinaryObject> binaries, PID pid) {
        return binaries.stream().filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
