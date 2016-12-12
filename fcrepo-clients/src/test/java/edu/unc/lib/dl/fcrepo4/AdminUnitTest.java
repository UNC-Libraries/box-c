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
 */package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

public class AdminUnitTest extends AbstractFedoraTest {
	
	private PID pid;
	private AdminUnit unit;
	
	private PID collectionChildPid;
	private PID workChildPid;

	private CollectionObject collectionChildObj;
	private WorkObject workChildObj;

	@Before
	public void init() {
		initMocks(this);

		pid = PIDs.get(UUID.randomUUID().toString());

		unit = new AdminUnit(pid, repository, dataLoader);

		collectionChildPid = PIDs.get(UUID.randomUUID().toString());
		workChildPid = PIDs.get(UUID.randomUUID().toString());
		when(repository.mintContentPid()).thenReturn(collectionChildPid).thenReturn(workChildPid);
		
		collectionChildObj = new CollectionObject(collectionChildPid, repository, dataLoader);
		when(repository.createCollectionObject(any(PID.class), any(Model.class)))
				.thenReturn(collectionChildObj);
		
		workChildObj = new WorkObject(workChildPid, repository, dataLoader);
		when(repository.createWorkObject(any(PID.class), any(Model.class)))
				.thenReturn(workChildObj);
	}

	@Test
	public void isValidTypeTest() {
		// Return the correct RDF types
		List<String> types = Arrays.asList(PcdmModels.Collection.getURI(), Cdr.AdminUnit.getURI());
		when(dataLoader.loadTypes(eq(unit))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
			@Override
			public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
				unit.setTypes(types);
				return dataLoader;
			}
		});

		unit.validateType();
	}
	
	@Test(expected = ObjectTypeMismatchException.class)
	public void invalidTypeTest() {
		List<String> types = Arrays.asList(PcdmModels.Object.getURI(), Cdr.Work.getURI());
		when(dataLoader.loadTypes(eq(unit))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
			@Override
			public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
				unit.setTypes(types);
				return dataLoader;
			}
		});

		unit.validateType();
	}
	
	// should not be able to add a Work object as a member
	@Test
	public void addWorkMemberTest() {
		unit.addMember(workChildObj);

		ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
		verify(repository).addMember(eq(unit), captor.capture());

		ContentObject child = captor.getValue();
		assertFalse("Should not be able to add Work to AdminUnit", child instanceof WorkObject);
	}

	@Test
	public void addCollectionObjectMemberTest() {
		unit.addMember(collectionChildObj);

		ArgumentCaptor<ContentObject> captor = ArgumentCaptor.forClass(ContentObject.class);
		verify(repository).addMember(eq(unit), captor.capture());

		ContentObject child = captor.getValue();
		assertTrue("Incorrect type of child added", child instanceof CollectionObject);
		assertEquals("Child did not have the expected pid", collectionChildPid, child.getPid());
	}

}
