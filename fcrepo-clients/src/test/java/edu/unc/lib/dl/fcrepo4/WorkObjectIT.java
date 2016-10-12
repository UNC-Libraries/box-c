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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
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
public class WorkObjectIT extends AbstractFedoraIT {
	@Autowired
	private Repository repository;

	private PID pid;

	@Before
	public void init() {
		pid = repository.mintContentPid();
	}

	@Test
	public void createWorkObjectTest() throws Exception {
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(DcElements.title, "Title");

		WorkObject obj = repository.createWorkObject(pid, model);

		assertTrue(obj.getTypes().contains(Cdr.Work.getURI()));
		assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));

		assertEquals("Title", obj.getResource().getProperty(DcElements.title).getString());
	}

	@Test
	public void addDataFileTest() throws Exception {
		WorkObject obj = repository.createWorkObject(pid);

		String bodyString = "Content";
		String filename = "file.txt";
		String mimetype = "text/plain";
		InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

		obj.addDataFile(contentStream, filename, mimetype, null);

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
		WorkObject obj = repository.createWorkObject(pid);

		// Create the primary object
		String bodyString = "Primary object";
		String filename = "primary.txt";
		InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

		FileObject primaryObj = obj.addDataFile(contentStream, filename, null, null);
		// Set it as the primary object for our work
		obj.setPrimaryObject(primaryObj.getPid());

		// Create the supplemental object
		String bodyStringS = "Supplement1";
		String filenameS = "s1.txt";
		InputStream contentStreamS = new ByteArrayInputStream(bodyStringS.getBytes());

		FileObject supp = obj.addDataFile(contentStreamS, filenameS, null, null);

		// Retrieve the primary object and verify it
		FileObject primaryResult = obj.getPrimaryObject();
		assertEquals(primaryObj.getPid(), primaryResult.getPid());

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
}
