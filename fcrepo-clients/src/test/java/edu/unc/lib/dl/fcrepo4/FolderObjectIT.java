/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * 
 * @author bbpennel
 *
 */
public class FolderObjectIT extends AbstractFedoraIT {

	@Autowired
	private Repository repository;

	private PID pid;

	@Before
	public void init() {
		pid = repository.mintContentPid();
	}

	@Test
	public void createFolderObjectTest() {
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(DcElements.title, "Folder Title");

		FolderObject obj = repository.createFolderObject(pid, model);

		assertTrue(obj.getTypes().contains(Cdr.Folder.getURI()));
		assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));

		assertEquals("Folder Title", obj.getResource()
				.getProperty(DcElements.title).getString());
	}

	@Test
	public void addFolderTest() throws Exception {
		FolderObject obj = repository.createFolderObject(pid);

		FolderObject child = obj.addFolder();

		assertNotNull(child);
		assertObjectExists(child.getPid());
		assertTrue(child.getTypes().contains(Cdr.Folder.getURI()));

		obj.getResource().hasProperty(PcdmModels.hasMember, child.getResource());
	}

	@Test
	public void addWorkTest() throws Exception {
		FolderObject obj = repository.createFolderObject(pid);

		PID childPid = repository.mintContentPid();

		Model childModel = ModelFactory.createDefaultModel();
		Resource childResc = childModel.createResource(childPid.getRepositoryPath());
		childResc.addProperty(DcElements.title, "Work Title");

		obj.addWork(childPid, childModel);
		WorkObject child = repository.getWorkObject(childPid);

		assertNotNull(child);
		assertObjectExists(childPid);
		assertTrue(child.getTypes().contains(Cdr.Work.getURI()));
		assertEquals("Work Title", child.getResource()
				.getProperty(DcElements.title).getString());

		obj.getResource().hasProperty(PcdmModels.hasMember, child.getResource());
	}

	@Test
	public void getMembersTest() {
		FolderObject obj = repository.createFolderObject(pid);

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
