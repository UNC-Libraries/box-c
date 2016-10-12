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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * 
 * @author bbpennel
 *
 */
public class FolderObjectTest extends AbstractFedoraTest{

	private PID pid;

	private PID childPid;

	private FolderObject folder;

	@Before
	public void init() {
		initMocks(this);

		pid = PIDs.get(UUID.randomUUID().toString());

		folder = new FolderObject(pid, repository, dataLoader);

		childPid = PIDs.get(UUID.randomUUID().toString());
		when(repository.mintContentPid()).thenReturn(childPid);
	}

	@Test
	public void isValidTypeTest() {
		// Return the correct RDF types
		List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Folder.getURI());
		when(dataLoader.loadTypes(eq(folder))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
			@Override
			public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
				folder.setTypes(types);
				return dataLoader;
			}
		});

		folder.validateType();
	}

	@Test(expected = ObjectTypeMismatchException.class)
	public void invalidTypeTest() {
		when(dataLoader.loadTypes(eq(folder))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
			@Override
			public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
				folder.setTypes(Arrays.asList());
				return dataLoader;
			}
		});

		folder.validateType();
	}

	@Test
	public void addFolderTest() {
		FolderObject childFolder = new FolderObject(childPid, repository, dataLoader);

		when(repository.createFolderObject(any(PID.class), any(Model.class)))
				.thenReturn(childFolder);

		folder.addFolder();

		verify(repository).createFolderObject(eq(childPid), (Model) isNull());

		ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
		verify(repository).addMember(eq(folder), captor.capture());

		ContentObject child = captor.getValue();
		assertTrue("Incorrect type of child added", child instanceof FolderObject);
		assertEquals("Child did not have the expected pid", childPid, child.getPid());
	}

	@Test
	public void addWorkTest() {
		WorkObject childObj = new WorkObject(childPid, repository, dataLoader);

		when(repository.createWorkObject(any(PID.class), any(Model.class)))
				.thenReturn(childObj);

		folder.addWork();

		verify(repository).createWorkObject(eq(childPid), (Model) isNull());

		ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
		verify(repository).addMember(eq(folder), captor.capture());

		ContentObject child = captor.getValue();
		assertTrue("Incorrect type of child added", child instanceof WorkObject);
		assertEquals("Child did not have the expected pid", childPid, child.getPid());
	}
}
