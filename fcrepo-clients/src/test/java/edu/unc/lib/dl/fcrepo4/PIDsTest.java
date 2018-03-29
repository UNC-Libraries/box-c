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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.REPOSITORY_ROOT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author bbpennel
 *
 */
public class PIDsTest {

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String TEST_UUID = "95553b02-0256-4c73-b423-f12d070501e8";

    private static final String TEST_PATH = "/95/55/3b/02/";

    @Before
    public void init() {
        TestHelper.setContentBase(FEDORA_BASE);
    }

    public String fakeRepositoryPath(String qualifier, String component) {
        String path = RepositoryPaths.getBaseUri() + qualifier + TEST_PATH + TEST_UUID;
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
            assertNull("Component path should not be set", pid.getComponentPath());
        } else {
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

    @Test
    public void getContentBaseTest() {
        String path = FEDORA_BASE + CONTENT_BASE;

        PID pid = PIDs.get(path);

        assertEquals(path, pid.getRepositoryPath());
        assertEquals(CONTENT_BASE, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
    }

    @Test
    public void getRootTest() {
        String path = FEDORA_BASE;

        PID pid = PIDs.get(path);

        assertEquals(path, pid.getRepositoryPath());
        assertEquals(REPOSITORY_ROOT_ID, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
    }

    @Test
    public void getContentBaseFromIdTest() {
        PID pid = PIDs.get(CONTENT_BASE);

        assertEquals(FEDORA_BASE + CONTENT_BASE, pid.getRepositoryPath());
        assertEquals(CONTENT_BASE, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
    }

    @Test
    public void getRootFromIdTest() {
        PID pid = PIDs.get(REPOSITORY_ROOT_ID);

        assertEquals(FEDORA_BASE, pid.getRepositoryPath());
        assertEquals(REPOSITORY_ROOT_ID, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
    }
}
