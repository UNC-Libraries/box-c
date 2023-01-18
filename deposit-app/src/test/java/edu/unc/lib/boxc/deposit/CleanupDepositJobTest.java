package edu.unc.lib.boxc.deposit;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

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
    private IngestSourceManager sourceManager;
    @Mock
    private IngestSource ingestSource;

    private CleanupDepositJob job;

    @BeforeEach
    public void setup() throws Exception {
        depositStatus = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        stagesDir = tmpFolder.resolve("stages").toFile();
        Files.createDirectory(tmpFolder.resolve("stages"));

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
        job.setIngestSourceManager(sourceManager);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "depositModelManager", depositModelManager);

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
        assertFalse(depositDir.exists(), "Deposit directory not cleaned up");

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

        when(ingestSource.isReadOnly()).thenReturn(true);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

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

        when(ingestSource.isReadOnly()).thenReturn(false);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

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
     * Test that the cleanup job does not fail if the staging folder specified
     * is not present at cleanup time
     *
     * @throws Exception
     */
    @Test
    public void cleanupMissingStagingFolderTest() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(false);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

        job.run();

        // Mainly ensuring that no error occurs when there is nothing to delete
        assertDepositCleanedUp();
    }

    @Test
    public void cleanupFileAlreadyRemovedTest() throws Exception {
        addIngestedFilesToModel();
        Files.delete(Paths.get(stagingPath, "project/folderA/ingested"));

        when(ingestSource.isReadOnly()).thenReturn(false);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

        job.run();

        assertIngestedFilesEmptyFoldersDeleted(stagingFolder);

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
        IngestSource ingestSource2 = mock(IngestSource.class);
        addIngestedFilesToModel(stagingPath2);
        populateStagingFolder(stagingFolder2);

        when(ingestSource.isReadOnly()).thenReturn(false);
        when(ingestSource2.isReadOnly()).thenReturn(true);
        doReturn(ingestSource).when(sourceManager)
                .getIngestSourceForUri(argThat(new FileBeginsWithMatcher(stagingFolder)));
        doReturn(ingestSource2).when(sourceManager)
                .getIngestSourceForUri(argThat(new FileBeginsWithMatcher(stagingFolder2)));

        job.run();

        assertIngestedFilesEmptyFoldersDeleted(stagingFolder);
        assertNothingDeleted(stagingFolder2);
    }

    @Test
    public void noCleanupPolicyTest() throws Exception {
        Assertions.assertThrows(UnknownIngestSourceException.class, () -> {
            when(sourceManager.getIngestSourceForUri(any(URI.class)))
                    .thenThrow(new UnknownIngestSourceException("Nope"));

            addIngestedFilesToModel();

            job.run();
        });
    }

    @Test
    public void cleanupFilesCleanedUpTest() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(false);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

        File cleanupFile = addCleanupFile();

        job.run();

        assertFalse(cleanupFile.exists(), "Cleanup file was not deleted");
    }

    @Test
    public void doNothingToCleanupFilesTest() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(true);
        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);

        File cleanupFile = addCleanupFile();

        job.run();

        assertTrue(cleanupFile.exists(), "Cleanup file should not have been deleted");
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

    private class FileBeginsWithMatcher implements ArgumentMatcher<URI> {
        private String basePath;

        public FileBeginsWithMatcher(File baseFile) {
            this.basePath = baseFile.getAbsolutePath();
        }

        @Override
        public boolean matches(URI compareUri) {
            String comparePath = compareUri.getPath();
            return comparePath.startsWith(basePath);
        }
    }
}
