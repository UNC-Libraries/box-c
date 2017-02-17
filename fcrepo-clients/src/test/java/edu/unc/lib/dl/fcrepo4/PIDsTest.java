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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class PIDsTest {

	@Mock
	private Repository mockRepo;

	private static final String FEDORA_BASE = "http://myrepo.example.com/fcrepo/rest/";

	private static final String TEST_UUID = "95553b02-0256-4c73-b423-f12d070501e8";

	private static final String TEST_PATH = "/95/55/3b/02/";

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		when(mockRepo.getBaseUri()).thenReturn(FEDORA_BASE);

		PIDs.setRepository(mockRepo);
	}

	public String fakeRepositoryPath(String qualifier, String component) {
		String path = FEDORA_BASE + qualifier + TEST_PATH + TEST_UUID;
		if (component != null) {
			path += "/" + component;
		}
		return path;
	}

	@Test
	public void getPidFromRepositoryPath() {
		String path = fakeRepositoryPath(CONTENT_BASE, null);

		PID pid = PIDs.get(path);

		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
		assertEquals("Repository path was incorrect", path, pid.getRepositoryUri().toString());
		assertEquals("Incorrect qualifier", CONTENT_BASE, pid.getQualifier());
		assertEquals("Content PID should follow uuid:<> format", "uuid:" + TEST_UUID, pid.getPid());
		assertNull("Component path should not be set", pid.getComponentPath());
	}

	@Test
	public void getPidFromRepositoryComponentPath() {
		String component = "manifest/manifest_txt";
		String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID + "/" + component;
		String path = fakeRepositoryPath(DEPOSIT_RECORD_BASE, component);

		PID pid = PIDs.get(path);

		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
		assertEquals("Repository path was incorrect", path, pid.getRepositoryUri().toString());
		assertEquals("Incorrect qualifier", DEPOSIT_RECORD_BASE, pid.getQualifier());
		assertEquals("Qualified id did not match", qualified, pid.getQualifiedId());
		assertEquals("PID did not match qualified id", qualified, pid.getPid());
		assertEquals("Incorrect component path", component, pid.getComponentPath());
	}

	@Test
	public void getInvalidRepositoryPath() {
		String path = "http://notmyrepo.example.com/";

		PID pid = PIDs.get(path);
		assertNull(pid);
	}

	@Test
	public void getPidFromUUID() {
		String uuid = "uuid:" + TEST_UUID;
		String expectedPath = fakeRepositoryPath(CONTENT_BASE, null);

		PID pid = PIDs.get(uuid);

		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
		assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryUri().toString());
		assertEquals("Blank qualifier was not set to default", CONTENT_BASE, pid.getQualifier());
		assertEquals("Content PID should follow uuid:<> format", uuid, pid.getPid());
		assertNull("Component path should not be set", pid.getComponentPath());
	}

	@Test
	public void getPidFromIdentifier() {
		String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID;
		String expectedPath = fakeRepositoryPath(DEPOSIT_RECORD_BASE, null);

		PID pid = PIDs.get(qualified);

		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
		assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryUri().toString());
		assertEquals("Blank qualifier was not set to default", DEPOSIT_RECORD_BASE, pid.getQualifier());
		assertEquals("Qualified id did not match", qualified, pid.getQualifiedId());
		assertEquals("PID did not match qualified id", qualified, pid.getPid());
		assertNull("Component path should not be set", pid.getComponentPath());
	}

	@Test
	public void getComponentFromIdentifier() {
		String component = "manifest/manifest_txt";
		String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID + "/" + component;
		String expectedPath = fakeRepositoryPath(DEPOSIT_RECORD_BASE, component);

		PID pid = PIDs.get(qualified);

		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
		assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryUri().toString());
		assertEquals("Blank qualifier was not set to default", DEPOSIT_RECORD_BASE, pid.getQualifier());
		assertEquals("Qualified id did not match", qualified, pid.getQualifiedId());
		assertEquals("PID did not match qualified id", qualified, pid.getPid());
		assertEquals("Incorrect component path", component, pid.getComponentPath());
	}

	@Test
	public void getInvalidFromIdentifier() {
		String identifier = "/ab/c1/23/abcd123";

		PID pid = PIDs.get(identifier);

		assertNull(pid);
	}

	@Test
	public void getReservedPidFromIdentifierTest() {
		String qualified = CONTENT_BASE + "/" + CONTENT_ROOT_ID;
		String expectedPath = FEDORA_BASE + qualified;

		PID pid = PIDs.get(qualified);

		verifyReservedPid(pid, qualified, expectedPath, null);
	}

	@Test
	public void getReservedPidFromPathTest() {
		String qualified = CONTENT_BASE + "/" + CONTENT_ROOT_ID;
		String expectedPath = FEDORA_BASE + qualified;

		PID pid = PIDs.get(expectedPath);

		verifyReservedPid(pid, qualified, expectedPath, null);
	}

	@Test
	public void getReservedPidFromIdOnlyTest() {
		String qualified = CONTENT_BASE + "/" + CONTENT_ROOT_ID;
		String expectedPath = FEDORA_BASE + qualified;

		PID pid = PIDs.get(CONTENT_ROOT_ID);

		verifyReservedPid(pid, qualified, expectedPath, null);
	}

	private void verifyReservedPid(PID pid, String qualified, String expectedPath, String component) {
		assertNotNull(pid);
		assertEquals("Identifer did not match provided value", CONTENT_ROOT_ID, pid.getId());
		assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryUri().toString());
		assertEquals("Blank qualifier was not set to default", CONTENT_BASE, pid.getQualifier());
		assertEquals("Qualified id did not match", qualified, pid.getQualifiedId());
		if (component == null) {
			assertEquals("PID did not match qualified id", "uuid:" + CONTENT_ROOT_ID, pid.getPid());
			assertNull("Component path should not be set", pid.getComponentPath());
		} else {
			assertEquals("PID did not match qualified id", "uuid:" + CONTENT_ROOT_ID + "/" + component, pid.getPid());
			assertEquals("Component path not set correctly", component, pid.getComponentPath());
		}
	}

	@Test
	public void getReservedPidFromPathWithComponentTest() {
		String component = "events/event1";
		String qualified = CONTENT_BASE + "/" + CONTENT_ROOT_ID + "/" + component;
		String expectedPath = FEDORA_BASE + qualified;

		PID pid = PIDs.get(expectedPath);

		verifyReservedPid(pid, qualified, expectedPath, component);
	}

	@Test
	public void getInvalidReservedPidTest() {
		String identifier = "fakereserved";

		PID pid = PIDs.get(identifier);

		assertNull(pid);
	}

	@Test
	public void getInvalidUUIDPathWithoutHashedContainersTest() {
		String path = FEDORA_BASE + CONTENT_BASE + "/" + TEST_UUID;

		PID pid = PIDs.get(path);

		assertNull(pid);
	}

	@Test
	public void getPidWithTxid() {
		String txid = "tx:99b58d30-06f5-477b-a44c-d614a9049d38";
		String path = FEDORA_BASE + txid + "/" + CONTENT_BASE + TEST_PATH + TEST_UUID;
		
		PID pid = PIDs.get(path);
		
		assertNotNull(pid);
		assertEquals(FEDORA_BASE + CONTENT_BASE + TEST_PATH + TEST_UUID, pid.getRepositoryPath());
	}
}
