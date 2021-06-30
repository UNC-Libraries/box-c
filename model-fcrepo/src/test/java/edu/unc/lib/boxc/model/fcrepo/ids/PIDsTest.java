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
package edu.unc.lib.boxc.model.fcrepo.ids;

import static edu.unc.lib.boxc.model.api.ids.PIDConstants.DEPOSITS_QUALIFIER;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.REPOSITORY_ROOT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
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

    @Test(expected = InvalidPidException.class)
    public void getInvalidRepositoryPath() {
        String path = "http://notmyrepo.example.com/";

        PIDs.get(path);
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

    @Test(expected = InvalidPidException.class)
    public void getInvalidFromIdentifier() {
        String identifier = "/ab/c1/23/abcd123";

        PIDs.get(identifier);
    }

    @Test
    public void getPidWithContainer() {
        String component = RepositoryPathConstants.DATA_FILE_FILESET;
        String expectedPath = fakeRepositoryPath(CONTENT_BASE, component);

        PID pid = PIDs.get(expectedPath);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", TEST_UUID, pid.getId());
        assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryUri().toString());
        assertEquals("Incorrect qualifier", CONTENT_BASE, pid.getQualifier());
        assertEquals("Component path should match fileset",
                RepositoryPathConstants.DATA_FILE_FILESET, pid.getComponentPath());
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

    @Test(expected = InvalidPidException.class)
    public void getInvalidReservedPidTest() {
        String identifier = "fakereserved";

        PIDs.get(identifier);
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
    public void getPidFromDepositBaseUri() {
        String path = RepositoryPaths.getDepositRecordBase();

        PID pid = PIDs.get(path);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", DEPOSITS_QUALIFIER, pid.getId());
        assertEquals("Repository path was incorrect", path, pid.getRepositoryUri().toString());
        assertEquals("Incorrect qualifier", REPOSITORY_ROOT_ID, pid.getQualifier());
        assertNull("Component path should not be set", pid.getComponentPath());
    }

    @Test
    public void getPidFromDepositBaseId() {
        PID pid = PIDs.get(DEPOSITS_QUALIFIER);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", DEPOSITS_QUALIFIER, pid.getId());
        assertEquals("Repository path was incorrect",
                RepositoryPaths.getDepositRecordBase(), pid.getRepositoryUri().toString());
        assertEquals("Incorrect qualifier", REPOSITORY_ROOT_ID, pid.getQualifier());
        assertNull("Component path should not be set", pid.getComponentPath());
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
    public void getContentBaseFromQualifiedIdTest() {
        PID pid = PIDs.get(CONTENT_BASE);
        PID fromQualified = PIDs.get(pid.getQualifiedId());

        assertEquals(FEDORA_BASE + CONTENT_BASE, fromQualified.getRepositoryPath());
        assertEquals(CONTENT_BASE, fromQualified.getId());
        assertEquals(REPOSITORY_ROOT_ID, fromQualified.getQualifier());
    }

    @Test
    public void getRootFromIdTest() {
        PID pid = PIDs.get(REPOSITORY_ROOT_ID);

        assertEquals(FEDORA_BASE, pid.getRepositoryPath());
        assertEquals(REPOSITORY_ROOT_ID, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
        assertEquals(REPOSITORY_ROOT_ID + "/" + REPOSITORY_ROOT_ID, pid.getQualifiedId());
    }

    @Test
    public void getRootFromQualifiedIdTest() {
        PID pid = PIDs.get(RepositoryPaths.getRootPid().getQualifiedId());

        assertEquals(FEDORA_BASE, pid.getRepositoryPath());
        assertEquals(REPOSITORY_ROOT_ID, pid.getId());
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier());
        assertEquals(REPOSITORY_ROOT_ID + "/" + REPOSITORY_ROOT_ID, pid.getQualifiedId());
    }

    @Test
    public void agentPidFromPath() {
        String id = "person/subdomain/username";
        String path = RepositoryPaths.getBaseUri() + PIDConstants.AGENTS_QUALIFIER + "/" + id;

        PID pid = PIDs.get(path);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", id, pid.getId());
        assertEquals("Repository path was incorrect", path, pid.getRepositoryPath());
        assertEquals("Incorrect qualifier", PIDConstants.AGENTS_QUALIFIER, pid.getQualifier());
        assertNull("Component path should not be set", pid.getComponentPath());
    }

    @Test
    public void agentPidFromId() {
        String id = "person/subdomain/username";
        String qualifiedId = PIDConstants.AGENTS_QUALIFIER + "/" + id;

        String expectedPath = RepositoryPaths.getBaseUri() + qualifiedId;

        PID pid = PIDs.get(qualifiedId);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", id, pid.getId());
        assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryPath());
        assertEquals("Incorrect qualifier", PIDConstants.AGENTS_QUALIFIER, pid.getQualifier());
        assertEquals("Incorrect qualified id", qualifiedId, pid.getQualifiedId());
        assertNull("Component path should not be set", pid.getComponentPath());
    }

    @Test
    public void agentPidFromQualifierAndId() {
        String id = "person/subdomain/username";

        String expectedQualifiedId = PIDConstants.AGENTS_QUALIFIER + "/" + id;
        String expectedPath = RepositoryPaths.getBaseUri() + expectedQualifiedId;

        PID pid = PIDs.get(PIDConstants.AGENTS_QUALIFIER, id);

        assertNotNull(pid);
        assertEquals("Identifer did not match provided value", id, pid.getId());
        assertEquals("Repository path was incorrect", expectedPath, pid.getRepositoryPath());
        assertEquals("Incorrect qualifier", PIDConstants.AGENTS_QUALIFIER, pid.getQualifier());
        assertEquals("Incorrect qualified id", expectedQualifiedId, pid.getQualifiedId());
        assertNull("Component path should not be set", pid.getComponentPath());
    }

    @Test(expected = InvalidPidException.class)
    public void rejectsAgentPidWithoutQualifier() {
        String id = "person/subdomain/username";

        PIDs.get(id);
    }
}
