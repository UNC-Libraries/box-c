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
package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmUse;

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
        InputStream contentStream = new ByteArrayInputStream(origBodyString.getBytes());
        BinaryObject origObj = fileObj.addOriginalFile(contentStream, origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        verifyOriginalFile(origObj);

        BinaryObject retrievedOrig = fileObj.getOriginalFile();
        verifyOriginalFile(retrievedOrig);
    }

    @Test
    public void getMultipleBinariesTest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        // Add the original
        InputStream contentStream = new ByteArrayInputStream(origBodyString.getBytes());
        BinaryObject bObj3 = fileObj.addOriginalFile(contentStream, origFilename, origMimetype, origSha1Checksum,
                origMd5Checksum);

        // Construct the derivative objects
        String textBodyString = "Extracted text";
        String textFilename = "extracted.txt";
        String textMimetype = "text/plain";
        InputStream textContentStream = new ByteArrayInputStream(textBodyString.getBytes());
        BinaryObject bObj1 = fileObj.addDerivative("text", textContentStream, textFilename, textMimetype,
                PcdmUse.ExtractedText);
        assertNotNull(bObj1);

        String thumbBodyString = "";
        String thumbFilename = "thumb.png";
        String thumbMimetype = "image/png";
        InputStream thumbContentStream = new ByteArrayInputStream(thumbBodyString.getBytes());
        BinaryObject bObj2 = fileObj.addDerivative("thumb", thumbContentStream, thumbFilename, thumbMimetype,
                PcdmUse.ThumbnailImage);
        assertNotNull(bObj1);

        // Retrieve the binary objects directly
        List<BinaryObject> binaries = fileObj.getBinaryObjects();

        assertEquals("Incorrect number of binaries added", 3, binaries.size());

        // Find each of the created binaries by pid
        BinaryObject rObj1 = findBinaryByPid(binaries, bObj1.getPid());
        BinaryObject rObj2 = findBinaryByPid(binaries, bObj2.getPid());
        BinaryObject rObj3 = findBinaryByPid(binaries, bObj3.getPid());

        assertNotNull(rObj1);
        assertNotNull(rObj2);
        assertNotNull(rObj3);

        // Verify that binaries had correct data added
        verifyFile(rObj1, textFilename, textMimetype, textBodyString);
        verifyFile(rObj2, thumbFilename, thumbMimetype, thumbBodyString);
        verifyOriginalFile(rObj3);
    }

    @Test
    public void testGetBinaryByName() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        InputStream contentStream = new ByteArrayInputStream(origBodyString.getBytes());
        fileObj.addBinary("some_binary", contentStream, origFilename, origMimetype, null, null, null);

        BinaryObject binObj = fileObj.getBinaryObject("some_binary");
        verifyFile(binObj, origFilename, origMimetype, origBodyString);
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getNonFileObject() throws Exception {
        PID objPid = PIDs.get("uuid:" + UUID.randomUUID().toString());

        client.put(objPid.getRepositoryUri()).perform().close();

        repoObjLoader.getFileObject(objPid);
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
