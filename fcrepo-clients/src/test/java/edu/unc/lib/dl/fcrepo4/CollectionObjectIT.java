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

import java.util.List;

import org.junit.Test;

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
