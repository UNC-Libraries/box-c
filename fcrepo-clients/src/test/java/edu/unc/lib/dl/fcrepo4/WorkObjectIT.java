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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class WorkObjectIT extends AbstractFedoraIT {

    @Before
    public void init() throws Exception {
        createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        repoObjLoader.init();
    }

    @Test
    public void createWorkObjectTest() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(DcElements.title, "Title");

        WorkObject obj = repoObjFactory.createWorkObject(model);

        assertTrue(obj.getTypes().contains(Cdr.Work.getURI()));
        assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));

        assertEquals("Title", obj.getResource().getProperty(DcElements.title).getString());
    }

    @Test
    public void addDataFileTest() throws Exception {
        WorkObject obj = repoObjFactory.createWorkObject(null);

        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        obj.addDataFile(contentStream, filename, mimetype, null, null);

        List<ContentObject> members = obj.getMembers();
        assertEquals(1, members.size());

        assertTrue(members.get(0) instanceof FileObject);

        FileObject dataObj = (FileObject) members.get(0);
        BinaryObject bObj = dataObj.getOriginalFile();

        assertEquals(filename, bObj.getFilename());
        assertEquals(mimetype, bObj.getMimetype());

        String respString = new BufferedReader(new InputStreamReader(bObj.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", bodyString, respString);
    }

    @Test
    public void addPrimaryObjectAndSupplements() throws Exception {
        WorkObject obj = repoObjFactory.createWorkObject(null);

        // Create the primary object
        String bodyString = "Primary object";
        String filename = "primary.txt";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        FileObject primaryObj = obj.addDataFile(contentStream, filename, null, null, null);
        // Set it as the primary object for our work
        obj.setPrimaryObject(primaryObj.getPid());

        // Create the supplemental object
        String bodyStringS = "Supplement1";
        String filenameS = "s1.txt";
        InputStream contentStreamS = new ByteArrayInputStream(bodyStringS.getBytes());

        FileObject supp = obj.addDataFile(contentStreamS, filenameS, null, null, null);

        // Retrieve the primary object and verify it
        FileObject primaryResult = obj.getPrimaryObject();
        assertEquals(primaryObj.getPid(), primaryResult.getPid());
        assertEquals(filename, primaryResult.getResource().getProperty(DC.title).getString());

        BinaryObject primaryBinary = primaryResult.getOriginalFile();
        assertEquals(filename, primaryBinary.getFilename());

        String respString = new BufferedReader(new InputStreamReader(primaryBinary.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", bodyString, respString);

        // Get all members of this work and verify everything is there
        List<ContentObject> members = obj.getMembers();
        assertEquals("Incorrect number of members assigned to work", 2, members.size());

        FileObject primaryMember = (FileObject) findContentObjectByPid(members, primaryObj.getPid());
        assertNotNull("Primary object not found in members", primaryMember);

        FileObject suppMember = (FileObject) findContentObjectByPid(members, supp.getPid());
        BinaryObject suppFile = suppMember.getOriginalFile();
        assertEquals(filenameS, suppFile.getFilename());
    }

    @Test
    public void addModsTest() throws Exception {
        WorkObject obj = repoObjFactory.createWorkObject(null);
        String bodyString = "some MODS content";
        InputStream modsStream = new ByteArrayInputStream(bodyString.getBytes());
        FileObject fileObj = obj.setDescription(modsStream);

        assertObjectExists(obj.getMODS().getPid());
        List<BinaryObject> binObjs = fileObj.getBinaryObjects();
        assertEquals(1, binObjs.size());
        assertObjectExists(binObjs.get(0).getPid());
        // make sure content is added to MODS
        String respString = new BufferedReader(new InputStreamReader(binObjs.get(0).getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", bodyString, respString);
    }

    @Test
    public void addSourceMdTest() throws Exception {
        WorkObject anotherObj = repoObjFactory.createWorkObject(null);
        String sourceProfile = "some source md";
        String sourceMdBodyString = "source md content";
        String modsBodyString = "MODS content";
        InputStream sourceMdStream = new ByteArrayInputStream(sourceMdBodyString.getBytes());
        InputStream modsStream = new ByteArrayInputStream(modsBodyString.getBytes());
        FileObject fileObj = anotherObj.addDescription(sourceMdStream, sourceProfile, modsStream);
        // tests that getDescription returns FileObject containing source md and mods
        assertObjectExists(anotherObj.getDescription().getPid());
        assertNotNull(anotherObj.getMODS());
        List<BinaryObject> binObjs = fileObj.getBinaryObjects();
        assertEquals(2, binObjs.size());

        BinaryObject b0 = binObjs.get(0);
        BinaryObject b1 = binObjs.get(1);
        PID pid0 = b0.getPid();
        PID pid1 = b1.getPid();
        // tests that mods and source md binaries were created
        assertObjectExists(pid0);
        assertObjectExists(pid1);
        // make sure content is added to source md and mods binaries
        if (pid0.equals((anotherObj.getMODS().getPid()))) {
            verifyContent(b1, b0, sourceMdBodyString, modsBodyString);
        } else {
            verifyContent(b0, b1, sourceMdBodyString, modsBodyString);
        }
    }

    private void verifyContent(BinaryObject sourceMdBin, BinaryObject modsBin,
            String sourceMdBodyString, String modsBodyString) {
        String sourceMdRespString = new BufferedReader(new InputStreamReader(sourceMdBin.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", sourceMdBodyString, sourceMdRespString);

        String modsRespString = new BufferedReader(new InputStreamReader(modsBin.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Original content did not match submitted value", modsBodyString, modsRespString);
    }
}
