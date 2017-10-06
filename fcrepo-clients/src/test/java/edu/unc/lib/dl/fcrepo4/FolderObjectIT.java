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

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Test;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;

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

        obj.getResource().hasProperty(PcdmModels.hasMember, child.getResource());
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

        obj.getResource().hasProperty(PcdmModels.hasMember, work.getResource());
    }

    @Test
    public void getMembersTest() {
        FolderObject obj = repoObjFactory.createFolderObject(null);

        WorkObject child1 = obj.addWork();
        FolderObject child2 = obj.addFolder();

        List<ContentObject> members = obj.getMembers();
        assertEquals("Incorrect number of members", 2, members.size());

        WorkObject member1 = (WorkObject) findContentObjectByPid(members, child1.getPid());
        FolderObject member2 = (FolderObject) findContentObjectByPid(members, child2.getPid());

        assertNotNull(member1);
        assertNotNull(member2);
    }
}
