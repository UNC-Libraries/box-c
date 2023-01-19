package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

/**
*
* @author bbpennel
*
*/
public class CollectionObjectIT extends AbstractFedoraIT {

    @Test
    public void testAddAndGetMembers() throws Exception {
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);

        FolderObject folder = repoObjFactory.createFolderObject(null);
        WorkObject work = repoObjFactory.createWorkObject(null);

        collObj.addMember(folder);
        collObj.addMember(work);

        treeIndexer.indexAll(baseAddress);

        List<ContentObject> members = collObj.getMembers();
        assertEquals(2, members.size(), "Incorrect number of members");

        WorkObject workMember = (WorkObject) findContentObjectByPid(members, work.getPid());
        FolderObject folderMember = (FolderObject) findContentObjectByPid(members, folder.getPid());

        assertNotNull(workMember, "Must return the created work as a member");
        assertNotNull(folderMember, "Must return the created folder as a member");
    }

    @Test
    public void testGetParent() throws Exception {
        AdminUnit adminObj = repoObjFactory.createAdminUnit(null);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);

        adminObj.addMember(collObj);

        RepositoryObject parent = collObj.getParent();
        assertEquals(adminObj.getPid(), parent.getPid(), "Parent for collection must be the created admin unit");
    }
}
