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
package edu.unc.lib.dl.persist.services.transfer;

import static edu.unc.lib.dl.model.DatastreamPids.getOriginalFilePid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.persist.services.storage.HashedPosixStorageLocation;

/**
 * @author bbpennel
 */
public class FSToPosixTransferClientTest extends FSToFSTransferClientTest {

    @Mock
    private HashedPosixStorageLocation storageLoc;

    private FSToPosixTransferClient posixClient;

    @Override
    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourcePath = tmpFolder.newFolder("source").toPath();
        storagePath = tmpFolder.newFolder("storage").toPath();

        this.posixClient = new FSToPosixTransferClient(ingestSource, storageLoc);
        this.client = posixClient;

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
        binDestPath = storagePath.resolve(binPid.getComponentId());

        when(storageLoc.getStorageUri(binPid)).thenReturn(binDestPath.toUri());
        when(storageLoc.getPermissions()).thenReturn(null);
    }

    @Test
    public void transfer_NewFile_WithPermissions() throws Exception {
        when(storageLoc.getPermissions()).thenReturn(new HashSet<>(asList(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE)));

        Path sourceFile = createSourceFile();

        URI binUri = client.transfer(binPid, sourceFile.toUri());
        Path binPath = Paths.get(binUri);

        assertIsSourceFile(binPath);

        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(binPath);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
    }
}
