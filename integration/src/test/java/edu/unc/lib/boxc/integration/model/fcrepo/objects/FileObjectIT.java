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
package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.PcdmUse;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants;

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

    @Before
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
        Path origPath = Files.createTempFile("original", ".txt");
        FileUtils.writeStringToFile(origPath.toFile(), origBodyString, "UTF-8");
        BinaryObject origObj = fileObj.addOriginalFile(origPath.toUri(), origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        verifyOriginalFile(origObj);

        BinaryObject retrievedOrig = fileObj.getOriginalFile();
        verifyOriginalFile(retrievedOrig);
    }

    @Test
    public void getMultipleBinariesTest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        // Add the original
        Path origPath = Files.createTempFile("original", ".txt");
        FileUtils.writeStringToFile(origPath.toFile(), origBodyString, "UTF-8");
        BinaryObject bObj2 = fileObj.addOriginalFile(origPath.toUri(), origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        // Construct the derivative objects
        String textBodyString = "Extracted text";
        String textFilename = "extracted.txt";
        String textMimetype = "text/plain";
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        Path fitsPath = Files.createTempFile("extracted", ".txt");
        FileUtils.writeStringToFile(fitsPath.toFile(), textBodyString, "UTF-8");
        BinaryObject bObj1 = fileObj.addBinary(fitsPid, fitsPath.toUri(), textFilename, textMimetype,
                null, RDF.type, PcdmUse.ExtractedText);
        assertNotNull(bObj1);

        // Retrieve the binary objects directly
        List<BinaryObject> binaries = fileObj.getBinaryObjects();

        assertEquals("Incorrect number of binaries added", 2, binaries.size());

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
        Path contentPath = Files.createTempFile("test", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), origBodyString, "UTF-8");
        fileObj.addBinary(binPid, contentPath.toUri(), origFilename, origMimetype, null, null, null);

        BinaryObject binObj = fileObj.getBinaryObject(DatastreamType.ORIGINAL_FILE.getId());
        verifyFile(binObj, origFilename, origMimetype, origBodyString);
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getNonFileObject() throws Exception {
        PID objPid = PIDs.get("uuid:" + UUID.randomUUID().toString());

        client.put(objPid.getRepositoryUri()).perform().close();

        repoObjLoader.getFileObject(objPid);
    }

    @Test
    public void testGetParent() throws Exception {
        WorkObject work = repoObjFactory.createWorkObject(null);

        Path origPath = Files.createTempFile("original", ".txt");
        FileUtils.writeStringToFile(origPath.toFile(), origBodyString, "UTF-8");
        FileObject fileObj = work.addDataFile(origPath.toUri(), origFilename, origMimetype, origSha1Checksum,
                    origMd5Checksum);

        treeIndexer.indexAll(baseAddress);

        RepositoryObject parent = fileObj.getParent();
        assertEquals("Parent of the file must match the work it was created in",
                parent.getPid(), work.getPid());
    }

    private void verifyOriginalFile(BinaryObject origObj) {
        verifyFile(origObj, origFilename, origMimetype, origBodyString);
    }

    private void verifyFile(BinaryObject bObj, String filename, String mimetype, String bodyString) {
        assertEquals(filename, bObj.getFilename());
        assertEquals(mimetype, bObj.getMimetype());

        String respString = new BufferedReader(new InputStreamReader(bObj.getBinaryStream())).lines()
                .collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", bodyString, respString);
    }

    private BinaryObject findBinaryByPid(List<BinaryObject> binaries, PID pid) {
        return binaries.stream().filter(p -> p.getPid().equals(pid)).findAny().get();
    }
}
