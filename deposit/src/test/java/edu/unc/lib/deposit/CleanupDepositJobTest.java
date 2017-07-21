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
package edu.unc.lib.deposit;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.staging.StagingException;
import edu.unc.lib.deposit.staging.StagingPolicy.CleanupPolicy;
import edu.unc.lib.deposit.staging.StagingPolicyManager;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.URIUtil;

/**
 * 
 * @author bbpennel
 *
 */
public class CleanupDepositJobTest extends AbstractDepositJobTest {

    private File stagesDir;

    private File stagingFolder;
    private String stagingPath;

    private Map<String, String> depositStatus;

    @Mock
    private StagingPolicyManager stagingManager;

    private CleanupDepositJob job;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        depositStatus = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        stagesDir = tmpFolder.newFolder("stages");

        stagingFolder = new File(stagesDir, "staging_folder");
        stagingPath = stagingFolder.getAbsolutePath();
        populateStagingFolder(stagingFolder);

        createJob();
    }

    private void createJob() throws Exception {
        // Create a deposit directory with a manifest file in it
        File manifestFile = new File(depositDir, "manifest.txt");
        manifestFile.createNewFile();

        // Initialize the job
        job = new CleanupDepositJob(jobUUID, depositUUID);
        job.setStagingPolicyManager(stagingManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "dataset", dataset);

        job.init();
    }

    private void populateStagingFolder(File stagingFolder) throws Exception {
        File templateStageDirectory = new File("src/test/resources/cleanupStage");
        FileUtils.copyDirectory(templateStageDirectory, stagingFolder);
    }

    private void addIngestedFilesToModel() {
        addIngestedFilesToModel(stagingPath);
    }

    private void addIngestedFilesToModel(String basePath) {
        Model model = job.getWritableModel();

        Resource depositResc = model.createResource(depositPid.getRepositoryPath());
        depositResc.addProperty(CdrDeposit.stagingLocation,
                URIUtil.join(stagingPath, "project/folderA/ingested"));
        depositResc.addProperty(CdrDeposit.stagingLocation,
                URIUtil.join(stagingPath, "project/folderB/ingested"));
        depositResc.addProperty(CdrDeposit.stagingLocation,
                URIUtil.join(stagingPath, "project/folderB/also_ingested"));

        job.closeModel();
    }

    private void assertDepositCleanedUp() {
        assertFalse("Deposit directory not cleaned up", depositDir.exists());

        verify(depositStatusFactory).expireKeys(eq(depositUUID), anyInt());
        verify(depositStatusFactory).expireKeys(eq(depositUUID), anyInt());
    }

    /**
     * Verify that no ingest file cleanup takes place, as prescribed by policy
     * 
     * @throws Exception
     */
    @Test
    public void doNothingTest() throws Exception {
        addIngestedFilesToModel();

        doReturn(CleanupPolicy.DO_NOTHING).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        job.run();

        assertNothingDeleted(stagingFolder);

        assertDepositCleanedUp();
    }

    private void assertNothingDeleted(File stagingFolder) {
        assertTrue(new File(stagingFolder, "project/folderA/ingested").exists());
        assertTrue(new File(stagingFolder, "project/folderA/leftover").exists());
        assertTrue(new File(stagingFolder, "project/folderB/ingested").exists());
        assertTrue(new File(stagingFolder, "project/folderB/also_ingested").exists());
    }

    /**
     * Verify that deleted files and empty folders were deleted as prescribed by
     * policy
     * 
     * @throws Exception
     */
    @Test
    public void deleteIngestedFilesEmptyFoldersTest() throws Exception {
        addIngestedFilesToModel();

        doReturn(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        job.run();

        assertIngestedFilesEmptyFoldersDeleted(stagingFolder);

        assertDepositCleanedUp();
    }

    private void assertIngestedFilesEmptyFoldersDeleted(File stagingFolder) {
        assertFalse(new File(stagingFolder, "project/folderA/ingested").exists());
        assertTrue(new File(stagingFolder, "project/folderA/leftover").exists());
        assertFalse(new File(stagingFolder, "project/folderB/ingested").exists());
        assertFalse(new File(stagingFolder, "project/folderB/also_ingested").exists());
        assertFalse(new File(stagingFolder, "project/folderB").exists());
    }

    /**
     * Verify that only ingested files, not folders, were deleted as defined by
     * policy
     * 
     * @throws Exception
     */
    @Test
    public void deleteIngestedFilesTest() throws Exception {
        addIngestedFilesToModel();

        doReturn(CleanupPolicy.DELETE_INGESTED_FILES).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        job.run();

        assertIngestedFilesDeleted(stagingFolder);

        assertDepositCleanedUp();
    }

    private void assertIngestedFilesDeleted(File stagingFolder) {
        assertFalse(new File(stagingFolder, "project/folderA/ingested").exists());
        assertTrue(new File(stagingFolder, "project/folderA/leftover").exists());
        assertFalse(new File(stagingFolder, "project/folderB/ingested").exists());
        assertFalse(new File(stagingFolder, "project/folderB/also_ingested").exists());
        assertTrue(new File(stagingFolder, "project/folderB").exists());
    }

    /**
     * Test that the cleanup job does not fail if the staging folder specified
     * is not present at cleanup time
     * 
     * @throws Exception
     */
    @Test
    public void cleanupMissingStagingFolderTest() throws Exception {
        doReturn(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        job.run();

        // Mainly ensuring that no error occurs when there is nothing to delete
        assertDepositCleanedUp();
    }

    /**
     * Test that registering multiple staging locations with different policies
     * works correctly
     * 
     * @throws Exception
     */
    @Test
    public void multipleStagingPoliciesTest() throws Exception {
        addIngestedFilesToModel();
        File stagingFolder2 = new File(stagesDir, "staging_folder2");
        String stagingPath2 = stagingFolder2.getAbsolutePath();
        addIngestedFilesToModel(stagingPath2);
        populateStagingFolder(stagingFolder2);

        doReturn(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        doReturn(CleanupPolicy.DO_NOTHING).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder2)));

        job.run();

        assertIngestedFilesEmptyFoldersDeleted(stagingFolder);
        assertNothingDeleted(stagingFolder2);
    }

    @Test(expected = StagingException.class)
    public void noCleanupPolicyTest() throws Exception {
        when(stagingManager.getCleanupPolicy(any(URI.class)))
                .thenThrow(new StagingException("No staging location"));

        addIngestedFilesToModel();

        job.run();
    }

    @Test
    public void cleanupFilesCleanedUpTest() throws Exception {
        doReturn(CleanupPolicy.DELETE_INGESTED_FILES_EMPTY_FOLDERS).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        File cleanupFile = addCleanupFile();

        job.run();

        assertFalse("Cleanup file was not deleted", cleanupFile.exists());
    }

    @Test
    public void doNothingToCleanupFilesTest() throws Exception {
        doReturn(CleanupPolicy.DO_NOTHING).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        File cleanupFile = addCleanupFile();

        job.run();

        assertTrue("Cleanup file should not have been deleted", cleanupFile.exists());
    }

    private File addCleanupFile() throws Exception {
        Model model = job.getWritableModel();
        Resource depositResc = model.createResource(depositPid.getRepositoryPath());

        String cleanupFilename = "extra-manifest.txt";
        File cleanupFile = new File(stagingPath, cleanupFilename);
        cleanupFile.createNewFile();
        depositResc.addProperty(CdrDeposit.cleanupLocation,
                URIUtil.join(stagingPath, cleanupFilename));

        job.closeModel();

        return cleanupFile;
    }

    @Test
    public void cleanupFileAndCleanupStagedTest() throws Exception {
        doReturn(CleanupPolicy.DELETE_INGESTED_FILES).when(stagingManager)
                .getCleanupPolicy(argThat(new FileBeginsWithMatcher(stagingFolder)));

        File cleanupFile = addCleanupFile();
        addIngestedFilesToModel();

        job.run();

        assertFalse("Cleanup file was not deleted", cleanupFile.exists());
        assertIngestedFilesDeleted(stagingFolder);
    }

    private class FileBeginsWithMatcher extends ArgumentMatcher<URI> {
        private String basePath;

        public FileBeginsWithMatcher(File baseFile) {
            this.basePath = baseFile.getAbsolutePath();
        }

        @Override
        public boolean matches(Object argument) {
            URI compareUri = (URI) argument;
            String comparePath = compareUri.getPath();
            return comparePath.startsWith(basePath);
        }

    }
}
