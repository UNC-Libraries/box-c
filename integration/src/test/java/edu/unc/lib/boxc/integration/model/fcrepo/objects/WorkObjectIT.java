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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;

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
        Path contentPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), bodyString, "UTF-8");

        obj.addDataFile(contentPath.toUri(), filename, mimetype, null, null);

        treeIndexer.indexAll(baseAddress);

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
        Path contentPath = Files.createTempFile("primary", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), bodyString, "UTF-8");

        FileObject primaryObj = obj.addDataFile(contentPath.toUri(), filename, null, null, null);
        // Set it as the primary object for our work
        obj.setPrimaryObject(primaryObj.getPid());

        // Create the supplemental object
        String bodyStringS = "Supplement1";
        String filenameS = "s1.txt";
        Path contentPath2 = Files.createTempFile("s1", ".txt");
        FileUtils.writeStringToFile(contentPath2.toFile(), bodyStringS, "UTF-8");

        FileObject supp = obj.addDataFile(contentPath2.toUri(), filenameS, null, null, null);

        treeIndexer.indexAll(baseAddress);

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
    public void getParentTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);
        WorkObject child = obj.addWork();

        RepositoryObject parent = child.getParent();
        assertEquals("Parent returned by the child must match the folder it was created in",
                obj.getPid(), parent.getPid());
    }
}
