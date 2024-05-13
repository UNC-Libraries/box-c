package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
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

        PID filePid = pidMinter.mintContentPid();
        String bodyString = "Content";
        String filename = "file.txt";
        String mimetype = "text/plain";
        var fileUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(filePid));
        FileUtils.writeStringToFile(new File(fileUri), bodyString, "UTF-8");

        obj.addDataFile(filePid, fileUri, filename, mimetype, null, null, null);

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
        assertEquals(bodyString, respString, "Original content did not match submitted value");
    }

    @Test
    public void addPrimaryObjectAndSupplements() throws Exception {
        WorkObject obj = repoObjFactory.createWorkObject(null);

        // Create the primary object
        PID filePid1 = pidMinter.mintContentPid();
        var fileUri1 = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(filePid1));
        String bodyString = "Primary object";
        String filename = "primary.txt";
        FileUtils.writeStringToFile(new File(fileUri1), bodyString, "UTF-8");
        FileObject primaryObj = obj.addDataFile(filePid1, fileUri1, filename, null, null, null, null);
        // Set it as the primary object for our work
        obj.setPrimaryObject(primaryObj.getPid());

        // Create the supplemental object
        PID filePid2 = pidMinter.mintContentPid();
        var fileUri2 = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(filePid2));
        String bodyStringS = "Supplement1";
        String filenameS = "s1.txt";
        FileUtils.writeStringToFile(new File(fileUri2), bodyStringS, "UTF-8");
        FileObject supp = obj.addDataFile(filePid2, fileUri2, filenameS, null, null, null, null);

        treeIndexer.indexAll(baseAddress);

        // Retrieve the primary object and verify it
        FileObject primaryResult = obj.getPrimaryObject();
        assertEquals(primaryObj.getPid(), primaryResult.getPid());
        assertEquals(filename, primaryResult.getResource().getProperty(DC.title).getString());

        BinaryObject primaryBinary = primaryResult.getOriginalFile();
        assertEquals(filename, primaryBinary.getFilename());

        String respString = new BufferedReader(new InputStreamReader(primaryBinary.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals(bodyString, respString, "Original content did not match submitted value");

        // Get all members of this work and verify everything is there
        List<ContentObject> members = obj.getMembers();
        assertEquals(2, members.size(), "Incorrect number of members assigned to work");

        FileObject primaryMember = (FileObject) findContentObjectByPid(members, primaryObj.getPid());
        assertNotNull(primaryMember, "Primary object not found in members");

        FileObject suppMember = (FileObject) findContentObjectByPid(members, supp.getPid());
        BinaryObject suppFile = suppMember.getOriginalFile();
        assertEquals(filenameS, suppFile.getFilename());
    }

    @Test
    public void getParentTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);
        WorkObject child = obj.addWork();

        RepositoryObject parent = child.getParent();
        assertEquals(obj.getPid(), parent.getPid(),
                "Parent returned by the child must match the folder it was created in");
    }
}
