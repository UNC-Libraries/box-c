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

import static edu.unc.lib.boxc.model.api.rdf.PcdmModels.memberOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Test;

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

        assertTrue("Added child must be a member of the folder",
                child.getResource().hasProperty(memberOf, obj.getResource()));
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

        assertTrue("Added child must be a member of the folder",
                work.getResource().hasProperty(memberOf, obj.getResource()));
    }

    @Test
    public void getMembersTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);

        WorkObject child1 = obj.addWork();
        FolderObject child2 = obj.addFolder();

        treeIndexer.indexAll(baseAddress);

        List<ContentObject> members = obj.getMembers();
        assertEquals("Incorrect number of members", 2, members.size());

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
        assertEquals("Incorrect number of members", 0, members1.size());

        List<ContentObject> members2 = folder2.getMembers();
        assertEquals("Incorrect number of members", 1, members2.size());

        assertEquals(child.getPid(), members2.get(0).getPid());
    }

    @Test
    public void getParentTest() throws Exception {
        FolderObject obj = repoObjFactory.createFolderObject(null);
        FolderObject child = obj.addFolder();

        RepositoryObject parent = child.getParent();
        assertEquals("Parent returned by the child must match the folder it was created in",
                parent.getPid(), obj.getPid());
    }
}
