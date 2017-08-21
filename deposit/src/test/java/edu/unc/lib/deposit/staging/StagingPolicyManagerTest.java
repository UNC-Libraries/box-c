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
package edu.unc.lib.deposit.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.deposit.staging.StagingPolicy.CleanupPolicy;

/**
 *
 * @author bbpennel
 *
 */
public class StagingPolicyManagerTest {

    private File depositsDirectory;
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private File stagingLoc1;
    private File stagingLoc2;
    private File unregisteredLoc;

    private URI validUri;
    private URI unregisteredUri;

    private StagingPolicyManager manager;

    @Before
    public void init() throws Exception {
        depositsDirectory = tmpFolder.newFolder("deposits");

        manager = new StagingPolicyManager();
        manager.setBasePath(depositsDirectory.getAbsolutePath());
        manager.setConfigPath("src/test/resources/staging_config.json");

        stagingLoc1 = new File(depositsDirectory, "staging_location1");
        stagingLoc1.mkdir();
        stagingLoc2 = new File(depositsDirectory, "staging_location2");
        stagingLoc2.mkdir();
        unregisteredLoc = new File(depositsDirectory, "unregistered");
        unregisteredLoc.mkdir();

        validUri = Paths.get(stagingLoc1.getAbsolutePath(), "file.txt").toUri();
        unregisteredUri = Paths.get(unregisteredLoc.getAbsolutePath(), "gone.txt").toUri();
    }

    @Test(expected = StagingException.class)
    public void missingLocationInitTest() {
        stagingLoc2.delete();

        manager.init();
    }

    @Test
    public void getStagingPolicyTest() {
        manager.init();

        StagingPolicy policy = manager.getStagingPolicy(validUri);

        assertNotNull(policy);
        assertEquals("Stage1", policy.getName());
        assertEquals(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS, policy.getCleanupPolicy());
        assertNotNull(policy.getPath());
    }

    @Test
    public void getStagingPolicyNonFileUriTest() {
        manager.init();

        URI uri = URI.create(validUri.getPath());
        StagingPolicy policy = manager.getStagingPolicy(uri);

        assertNotNull(policy);
        assertEquals("Stage1", policy.getName());
    }

    @Test
    public void getStagingPolicyRelativeTest() {
        manager.init();

        URI relative = URI.create("staging_location1/file.txt");

        StagingPolicy policy = manager.getStagingPolicy(relative);

        assertNotNull(policy);
        assertEquals("Stage1", policy.getName());
    }

    @Test(expected = StagingException.class)
    public void getStagingPolicyUnregisteredPathTest() {
        manager.init();

        manager.getStagingPolicy(
                Paths.get(unregisteredLoc.getAbsolutePath(), "file.txt").toUri());
    }

    @Test
    public void getCleanupPolicyTest() {
        manager.init();

        CleanupPolicy policy1 = manager.getCleanupPolicy(validUri);
        assertEquals(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS, policy1);

        URI validUri2 = Paths.get(stagingLoc2.getAbsolutePath(), "image.png").toUri();
        CleanupPolicy policy2 = manager.getCleanupPolicy(validUri2);
        assertEquals(CleanupPolicy.DO_NOTHING, policy2);
    }

    @Test(expected = StagingException.class)
    public void getCleanupPolicyForUnregistedPathTest() {
        manager.init();

        manager.getCleanupPolicy(unregisteredUri);
    }

    @Test
    public void isValidStagingLocationTest() {
        manager.init();

        assertTrue(manager.isValidStagingLocation(validUri));

        assertFalse(manager.isValidStagingLocation(unregisteredUri));
    }
}
