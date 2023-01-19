package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.api.rdf.PcdmModels.memberOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.objects.ContentObject;
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
public class FolderObjectIT extends AbstractFedoraIT {

    @Test
    public void createFolderObjectTest() {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(DcElements.title, "Folder Title");

        FolderObject obj = repoObjFactory.createFolderObject(model);

        assertTrue(obj.getTypes().contains(Cdr.Folder.getURI()));
        assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));

        assertEquals("Folder Title", obj.getResource()
                .getProperty(DcElements.title).getString());
    }

    @Test
    public void addFolderTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);

        FolderObject child = obj.addFolder();

        assertNotNull(child);
        assertObjectExists(child.getPid());
        assertTrue(child.getTypes().contains(Cdr.Folder.getURI()));

        assertTrue(child.getResource().hasProperty(memberOf, obj.getResource()),
                "Added child must be a member of the folder");
    }

    @Test
    public void addWorkTest() throws Exception {
        Model childModel = ModelFactory.createDefaultModel();
        Resource childResc = childModel.createResource("");
        childResc.addProperty(DcElements.title, "Work Title");

        FolderObject obj = repoObjFactory.createFolderObject(null);
        WorkObject work = obj.addWork(childModel);

        assertNotNull(work);
        assertObjectExists(work.getPid());
        assertTrue(work.getTypes().contains(Cdr.Work.getURI()));
        assertEquals("Work Title", work.getResource()
                .getProperty(DcElements.title).getString());

        assertTrue(work.getResource().hasProperty(memberOf, obj.getResource()),
                "Added child must be a member of the folder");
    }

    @Test
    public void getMembersTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);

        WorkObject child1 = obj.addWork();
        FolderObject child2 = obj.addFolder();

        treeIndexer.indexAll(baseAddress);

        List<ContentObject> members = obj.getMembers();
        assertEquals(2, members.size(), "Incorrect number of members");

        WorkObject member1 = (WorkObject) findContentObjectByPid(members, child1.getPid());
        FolderObject member2 = (FolderObject) findContentObjectByPid(members, child2.getPid());

        assertNotNull(member1);
        assertNotNull(member2);
    }

    @Test
    public void addChildToTwoFoldersTest() throws Exception {
        FolderObject folder1 = repoObjFactory.createFolderObject(null);
        FolderObject folder2 = repoObjFactory.createFolderObject(null);

        FolderObject child = folder1.addFolder();

        // Add the child to the second folder, effectively moving it
        folder2.addMember(child);

        treeIndexer.indexAll(baseAddress);

        List<ContentObject> members1 = folder1.getMembers();
        assertEquals(0, members1.size(), "Incorrect number of members");

        List<ContentObject> members2 = folder2.getMembers();
        assertEquals(1, members2.size(), "Incorrect number of members");

        assertEquals(child.getPid(), members2.get(0).getPid());
    }

    @Test
    public void getParentTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);
        FolderObject child = obj.addFolder();

        RepositoryObject parent = child.getParent();
        assertEquals(parent.getPid(), obj.getPid(),
                "Parent returned by the child must match the folder it was created in");
    }
}
