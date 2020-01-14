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
package edu.unc.lib.dl.persist.services.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageType;

/**
 * @author bbpennel
 *
 */
public class HashedFilesystemStorageLocationTest {

    private static final String TEST_UUID = "2e8f7551-ef3c-4387-8c3d-a38609927800";

    private HashedFilesystemStorageLocation loc;

    @Before
    public void setup() {
        loc = new HashedFilesystemStorageLocation();
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

    @Test(expected = IllegalArgumentException.class)
    public void baseFieldWithInvalidScheme() {
        String assignedBase = "http://example.com/to/my/stuff/";
        loc.setBase(assignedBase);
    }

    @Test
    public void getStorageUriForPidWithoutComponent() {
        String assignedBase = "file:/location/path/";

        loc.setId("loc1");
        loc.setBase(assignedBase);

        PID pid = PIDs.get(TEST_UUID);

        URI storageUri = loc.getStorageUri(pid);
        assertEquals(assignedBase + "2e/8f/75/51/" + TEST_UUID, storageUri.toString());
    }

    @Test
    public void getStorageUriForPidWithComponent() {
        String assignedBase = "file:/location/path/";
        String component = "/datafs/original_data";

        loc.setId("loc1");
        loc.setBase(assignedBase);

        PID pid = PIDs.get(TEST_UUID + component);

        URI storageUri = loc.getStorageUri(pid);
        assertEquals(assignedBase + "2e/8f/75/51/" + TEST_UUID + component, storageUri.toString());
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
