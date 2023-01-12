package edu.unc.lib.boxc.model.fcrepo.ids;

import static edu.unc.lib.boxc.model.api.ids.PIDConstants.DEPOSITS_QUALIFIER;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.REPOSITORY_ROOT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author bbpennel
 *
 */
public class PIDsTest {

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private static final String TEST_UUID = "95553b02-0256-4c73-b423-f12d070501e8";

    private static final String TEST_PATH = "/95/55/3b/02/";

    @BeforeEach
    public void init() {
        TestHelper.setContentBase(FEDORA_BASE);
    }

    public String fakeRepositoryPath(String qualifier, String component) {
        String path = FcrepoPaths.getBaseUri() + qualifier + TEST_PATH + TEST_UUID;
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
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(path, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Incorrect qualifier");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getPidFromRepositoryPathWithWhitespace() {
        String path = fakeRepositoryPath(CONTENT_BASE, null);
        String paddedPath = "  " + path + " ";

        PID pid = PIDs.get(path);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(path, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Incorrect qualifier");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getPidFromRepositoryComponentPath() {
        String component = "manifest/manifest_txt";
        String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID + "/" + component;
        String path = fakeRepositoryPath(DEPOSIT_RECORD_BASE, component);

        PID pid = PIDs.get(path);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(path, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(DEPOSIT_RECORD_BASE, pid.getQualifier(), "Incorrect qualifier");
        assertEquals(qualified, pid.getQualifiedId(), "Qualified id did not match");
        assertEquals(component, pid.getComponentPath(), "Incorrect component path");
    }

    @Test
    public void getInvalidRepositoryPath() {
        Assertions.assertThrows(InvalidPidException.class, () -> {
            String path = "http://notmyrepo.example.com/";

            PIDs.get(path);
        });
    }

    @Test
    public void getPidFromUUID() {
        String uuid = "uuid:" + TEST_UUID;
        String expectedPath = fakeRepositoryPath(CONTENT_BASE, null);

        PID pid = PIDs.get(uuid);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Blank qualifier was not set to default");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getPidFromIdentifier() {
        String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID;
        String expectedPath = fakeRepositoryPath(DEPOSIT_RECORD_BASE, null);

        PID pid = PIDs.get(qualified);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(DEPOSIT_RECORD_BASE, pid.getQualifier(), "Blank qualifier was not set to default");
        assertEquals(qualified, pid.getQualifiedId(), "Qualified id did not match");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getComponentFromIdentifier() {
        String component = "manifest/manifest_txt";
        String qualified = DEPOSIT_RECORD_BASE + "/" + TEST_UUID + "/" + component;
        String expectedPath = fakeRepositoryPath(DEPOSIT_RECORD_BASE, component);

        PID pid = PIDs.get(qualified);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(DEPOSIT_RECORD_BASE, pid.getQualifier(), "Blank qualifier was not set to default");
        assertEquals(qualified, pid.getQualifiedId(), "Qualified id did not match");
        assertEquals(component, pid.getComponentPath(), "Incorrect component path");
    }

    @Test
    public void getPidFromIDWithWhitespace() {
        String uuid = "  " + TEST_UUID + " ";
        String expectedPath = fakeRepositoryPath(CONTENT_BASE, null);

        PID pid = PIDs.get(uuid);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Blank qualifier was not set to default");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getInvalidFromIdentifier() {
        Assertions.assertThrows(InvalidPidException.class, () -> {
            String identifier = "/ab/c1/23/abcd123";

            PIDs.get(identifier);
        });
    }

    @Test
    public void getPidWithContainer() {
        String component = RepositoryPathConstants.DATA_FILE_FILESET;
        String expectedPath = fakeRepositoryPath(CONTENT_BASE, component);

        PID pid = PIDs.get(expectedPath);

        assertNotNull(pid);
        assertEquals(TEST_UUID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Incorrect qualifier");
        assertEquals(RepositoryPathConstants.DATA_FILE_FILESET, pid.getComponentPath(),
                "Component path should match fileset");
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
        assertEquals(CONTENT_ROOT_ID, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(CONTENT_BASE, pid.getQualifier(), "Blank qualifier was not set to default");
        assertEquals(qualified, pid.getQualifiedId(), "Qualified id did not match");
        if (component == null) {
            assertNull(pid.getComponentPath(), "Component path should not be set");
        } else {
            assertEquals(component, pid.getComponentPath(), "Component path not set correctly");
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
        Assertions.assertThrows(InvalidPidException.class, () -> {
            String identifier = "fakereserved";

            PIDs.get(identifier);
        });
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
        assertEquals(DEPOSITS_QUALIFIER, pid.getId(), "Identifier did not match provided value");
        assertEquals(path, pid.getRepositoryUri().toString(), "Repository path was incorrect");
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier(), "Incorrect qualifier");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void getPidFromDepositBaseId() {
        PID pid = PIDs.get(DEPOSITS_QUALIFIER);

        assertNotNull(pid);
        assertEquals(DEPOSITS_QUALIFIER, pid.getId(), "Identifier did not match provided value");
        assertEquals(RepositoryPaths.getDepositRecordBase(), pid.getRepositoryUri().toString(),
                "Repository path was incorrect");
        assertEquals(REPOSITORY_ROOT_ID, pid.getQualifier(), "Incorrect qualifier");
        assertNull(pid.getComponentPath(), "Component path should not be set");
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
        String path = FcrepoPaths.getBaseUri() + PIDConstants.AGENTS_QUALIFIER + "/" + id;

        PID pid = PIDs.get(path);

        assertNotNull(pid);
        assertEquals(id, pid.getId(), "Identifier did not match provided value");
        assertEquals(path, pid.getRepositoryPath(), "Repository path was incorrect");
        assertEquals(PIDConstants.AGENTS_QUALIFIER, pid.getQualifier(), "Incorrect qualifier");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void agentPidFromId() {
        String id = "person/subdomain/username";
        String qualifiedId = PIDConstants.AGENTS_QUALIFIER + "/" + id;

        String expectedPath = FcrepoPaths.getBaseUri() + qualifiedId;

        PID pid = PIDs.get(qualifiedId);

        assertNotNull(pid);
        assertEquals(id, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryPath(), "Repository path was incorrect");
        assertEquals(PIDConstants.AGENTS_QUALIFIER, pid.getQualifier(), "Incorrect qualifier");
        assertEquals(qualifiedId, pid.getQualifiedId(), "Incorrect qualified id");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void agentPidFromQualifierAndId() {
        String id = "person/subdomain/username";

        String expectedQualifiedId = PIDConstants.AGENTS_QUALIFIER + "/" + id;
        String expectedPath = FcrepoPaths.getBaseUri() + expectedQualifiedId;

        PID pid = PIDs.get(PIDConstants.AGENTS_QUALIFIER, id);

        assertNotNull(pid);
        assertEquals(id, pid.getId(), "Identifier did not match provided value");
        assertEquals(expectedPath, pid.getRepositoryPath(), "Repository path was incorrect");
        assertEquals(PIDConstants.AGENTS_QUALIFIER, pid.getQualifier(), "Incorrect qualifier");
        assertEquals(expectedQualifiedId, pid.getQualifiedId(), "Incorrect qualified id");
        assertNull(pid.getComponentPath(), "Component path should not be set");
    }

    @Test
    public void rejectsAgentPidWithoutQualifier() {
        Assertions.assertThrows(InvalidPidException.class, () -> {
            String id = "person/subdomain/username";

            PIDs.get(id);
        });
    }
}
