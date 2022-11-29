package edu.unc.lib.boxc.integration.model.fcrepo.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

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
        assertEquals("Incorrect number of members", 2, members.size());

        WorkObject workMember = (WorkObject) findContentObjectByPid(members, work.getPid());
        FolderObject folderMember = (FolderObject) findContentObjectByPid(members, folder.getPid());

        assertNotNull("Must return the created work as a member", workMember);
        assertNotNull("Must return the created folder as a member", folderMember);
    }

    @Test
    public void testGetParent() throws Exception {
        AdminUnit adminObj = repoObjFactory.createAdminUnit(null);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);

        adminObj.addMember(collObj);

        RepositoryObject parent = collObj.getParent();
        assertEquals("Parent for collection must be the created admin unit",
                adminObj.getPid(), parent.getPid());
    }
}
