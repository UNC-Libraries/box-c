package edu.unc.lib.boxc.persist.impl.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.storage.StorageType;
import edu.unc.lib.boxc.persist.impl.storage.HashedFilesystemStorageLocation;

/**
 * @author bbpennel
 *
 */
public class HashedFilesystemStorageLocationTest {

    private static final String TEST_UUID = "2e8f7551-ef3c-4387-8c3d-a38609927800";

    @TempDir
    public Path tmpFolder;
    protected Path storagePath;

    protected HashedFilesystemStorageLocation loc;

    @BeforeEach
    public void setup() throws Exception {
        storagePath = tmpFolder.resolve("storage");
        Files.createDirectory(storagePath);

        loc = new HashedFilesystemStorageLocation();
        loc.setBase(storagePath.toString());
    }

    @Test
    public void getStorageType() {
        assertEquals(StorageType.FILESYSTEM, loc.getStorageType());
    }

    @Test
    public void baseFieldWithFileScheme() {
        String assignedBase = "file:/path/to/my/stuff/";
        loc.setBase(assignedBase);

        assertEquals(assignedBase, loc.getBase());
        assertEquals(URI.create(assignedBase), loc.getBaseUri());
    }

    @Test
    public void baseFieldWithTripleSlashes() {
        String assignedBase = "file:///path/to/my/stuff/";
        String expectedBase = "file:/path/to/my/stuff/";
        loc.setBase(assignedBase);

        assertEquals(expectedBase, loc.getBase());
        assertEquals(URI.create(expectedBase), loc.getBaseUri());
    }

    @Test
    public void baseFieldWithFileSchemeWithoutTrailingSlash() {
        String assignedBase = "file:/path/to/my/stuff";
        loc.setBase(assignedBase);

        assertEquals(assignedBase + "/", loc.getBase());
        assertEquals(URI.create(assignedBase + "/"), loc.getBaseUri());
    }

    @Test
    public void baseFieldWithNoScheme() {
        String expectedBase = "file:/path/to/my/stuff/";
        String assignedBase = "/path/to/my/stuff/";
        loc.setBase(assignedBase);

        assertEquals(expectedBase, loc.getBase());
        assertEquals(URI.create(expectedBase), loc.getBaseUri());
    }

    @Test
    public void baseFieldWithInvalidScheme() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String assignedBase = "http://example.com/to/my/stuff/";
            loc.setBase(assignedBase);
        });
    }

    @Test
    public void getNewStorageUriForPidWithoutComponent() {
        String assignedBase = "file:/location/path/";

        loc.setId("loc1");
        loc.setBase(assignedBase);

        PID pid = PIDs.get(TEST_UUID);

        URI storageUri = loc.getNewStorageUri(pid);
        assertTrue(storageUri.toString().startsWith(assignedBase + "2e/8f/75/51/" + TEST_UUID));

    }

    @Test
    public void getNewStorageUriForPidWithComponent() {
        String assignedBase = "file:/location/path/";
        String component = "/datafs/original_data";

        loc.setId("loc1");
        loc.setBase(assignedBase);

        PID pid = PIDs.get(TEST_UUID + component);

        URI storageUri = loc.getNewStorageUri(pid);
        assertTrue(storageUri.toString().startsWith(assignedBase + "2e/8f/75/51/" + TEST_UUID + component));
    }

    @Test
    public void getNewStorageUriNoConflicts() {
        PID pid = PIDs.get(TEST_UUID);
        URI storageUri1 = loc.getNewStorageUri(pid);
        URI storageUri2 = loc.getNewStorageUri(pid);
        assertNotNull(storageUri1);
        assertNotNull(storageUri2);
        assertNotEquals(storageUri1, storageUri2);
    }

    @Test
    public void getCurrentStorageUriNoExisting() {
        PID pid = PIDs.get(TEST_UUID);
        assertNull(loc.getCurrentStorageUri(pid));
    }

    @Test
    public void getCurrentStorageUriOneExisting() throws Exception {
        PID pid = PIDs.get(TEST_UUID);
        URI expectedUri = createNewStorageUri(pid);

        URI currentUri = loc.getCurrentStorageUri(pid);
        assertEquals(expectedUri, currentUri);
        assertTrue(currentUri.toString().startsWith(storagePath.toFile().toURI().toString()));
    }

    @Test
    public void getCurrentStorageUriMultipleExisting() throws Exception {
        PID pid = PIDs.get(TEST_UUID);
        createNewStorageUri(pid);
        createNewStorageUri(pid);
        createNewStorageUri(pid);
        URI expectedUri = createNewStorageUri(pid);

        assertEquals(expectedUri, loc.getCurrentStorageUri(pid));
    }

    @Test
    public void getAllStorageUrisExisting() throws Exception {
        PID pid = PIDs.get(TEST_UUID);
        List<URI> expected = Arrays.asList(
                createNewStorageUri(pid),
                createNewStorageUri(pid),
                createNewStorageUri(pid),
                createNewStorageUri(pid));

        List<URI> uris = loc.getAllStorageUris(pid);
        assertEquals(expected.size(), uris.size());
        assertTrue(uris.containsAll(expected));
    }

    @Test
    public void getAllStorageUrisNoneExisting() throws Exception {
        PID pid = PIDs.get(TEST_UUID);

        List<URI> uris = loc.getAllStorageUris(pid);
        assertEquals(0, uris.size());
    }

    private URI createNewStorageUri(PID pid) throws IOException {
        URI uri = loc.getNewStorageUri(pid);
        Path destPath = Paths.get(uri);
        Files.createDirectories(destPath.getParent());
        FileUtils.writeStringToFile(destPath.toFile(), "content", "UTF-8");
        return uri;
    }

    @Test
    public void isValidUriInLocation() {
        String assignedBase = "file:/location/path/";
        String subpath = "ab/cd/ef/gh/abcdefgh";
        URI targetUri = URI.create(assignedBase + subpath);

        loc.setId("loc1");
        loc.setBase(assignedBase);

        assertTrue(loc.isValidUri(targetUri));
    }

    @Test
    public void isValidUriNotInLocation() {
        String assignedBase = "file:///location/path/";
        URI targetUri = URI.create("file:///some/other/path/to/my/file.txt");

        loc.setId("loc1");
        loc.setBase(assignedBase);

        assertFalse(loc.isValidUri(targetUri));
    }

    @Test
    public void nameField() {
        loc.setName("My name");
        assertEquals("My name", loc.getName());
    }

    @Test
    public void idField() {
        loc.setId("some_id");
        assertEquals("some_id", loc.getId());
    }
}
