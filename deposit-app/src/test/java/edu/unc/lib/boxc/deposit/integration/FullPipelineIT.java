package edu.unc.lib.boxc.deposit.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.deposit.api.DepositMethod;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.fcrepo4.IngestContentObjectsJob;
import edu.unc.lib.boxc.deposit.fcrepo4.IngestDepositRecordJob;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.impl.submit.FileServerDepositHandler;
import edu.unc.lib.boxc.deposit.normalize.DirectoryToBagJob;
import edu.unc.lib.boxc.deposit.pipeline.DepositCoordinator;
import edu.unc.lib.boxc.deposit.pipeline.JobCoordinator;
import edu.unc.lib.boxc.deposit.transfer.TransferBinariesToStorageJob;
import edu.unc.lib.boxc.deposit.validate.ExtractTechnicalMetadataJob;
import edu.unc.lib.boxc.deposit.validate.FixityCheckJob;
import edu.unc.lib.boxc.deposit.validate.ValidateContentModelJob;
import edu.unc.lib.boxc.deposit.validate.ValidateDestinationJob;
import edu.unc.lib.boxc.deposit.validate.ValidateFileAvailabilityJob;
import edu.unc.lib.boxc.deposit.validate.VirusScanJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.persist.api.PackagingType;
import fi.solita.clamav.ClamAVClient;
import fi.solita.clamav.ScanResult;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Properties;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/service-context.xml",
        "file:src/main/webapp/WEB-INF/fcrepo-clients-context.xml",
        "file:src/main/webapp/WEB-INF/deposit-jobs-context.xml",
        "classpath:spring-test/full-pipeline-it-context.xml"
})
public class FullPipelineIT {
    @TempDir
    static Path tempDir;
    private static Path depositsDir;
    private static Path fitsHomePath;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private DepositOperationMessageService messageService;
    private FileServerDepositHandler depositHandler;
    @Autowired
    private DepositStatusFactory depositStatusFactory;
    @Autowired
    private JobStatusFactory jobStatusFactory;
    @Autowired
    private PIDMinter pidminter;
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private DepositCoordinator depositCoordinator;
    @Autowired
    private JobCoordinator jobCoordinator;
    @Autowired
    private ClamAVClient clamavClient;

    @BeforeAll
    static void setupTestProperties() throws IOException {
        depositsDir = tempDir.resolve("deposits");
        Path depositsTdbDir = tempDir.resolve("deposits-tdb-dataset");
        Path derivativeDir = tempDir.resolve("derivatives");
        Path ingestUploadDir = tempDir.resolve("ingest_upload_staging");
        Path ingestSources = tempDir.resolve("ingest_sources.json");
        Path ingestSourceMappings = tempDir.resolve("ingest_source_mappings.json");
        Path storageLocations = tempDir.resolve("storage_locations.json");
        Path storageLocationMappings = tempDir.resolve("storage_location_mappings.json");
        Path aclProperties = tempDir.resolve("acl.properties");
        fitsHomePath = tempDir.resolve("fits");

        Files.createDirectories(depositsDir);
        Files.createDirectories(depositsTdbDir);
        Files.createDirectories(derivativeDir);
        Files.createDirectories(ingestUploadDir);
        Files.createDirectories(Paths.get("/tmp/boxc_test_storage"));
        Files.createDirectories(fitsHomePath);

        Files.writeString(ingestSources, """
                [{
                    "id": "deposits_dir",
                    "name": "Deposits Directory",
                    "base": "%s",
                    "patterns": ["*"],
                    "readOnly": false,
                    "internal": true,
                    "type": "filesystem"}]
                """.formatted(depositsDir.toAbsolutePath().toUri()));
        Files.writeString(ingestSourceMappings,
                "[{\"id\": \"collections\", \"sources\": [\"deposits_dir\"]}]");
        Files.writeString(storageLocations, """
                        [{"id": "primary_storage",
                        "name": "Primary repository storage",
                        "type": "hashed_posix",
                        "permissions": "0664",
                        "base": "/tmp/boxc_test_storage"}]
                        """);
        Files.writeString(storageLocationMappings,
                "[{\"id\": \"collections\", \"defaultLocation\": \"primary_storage\"}]");
        Files.writeString(aclProperties, "cdr.acl.globalRoles.administrator=admingroup");

        // Create test properties file
        Properties testProps = new Properties();
        testProps.load(FullPipelineIT.class.getResourceAsStream("/application.properties"));

        // Override directory paths
        testProps.setProperty("deposits.dir", depositsDir.toString());
        testProps.setProperty("deposits.tdb.dir", depositsTdbDir.toString());
        testProps.setProperty("derivative.dir", derivativeDir.toString());
        testProps.setProperty("ingest.upload.staging.path", ingestUploadDir.toAbsolutePath().toString());
        testProps.setProperty("ingestSources.path", ingestSources.toString());
        testProps.setProperty("ingestSourceMappings.path", ingestSourceMappings.toString());
        testProps.setProperty("storageLocations.path", storageLocations.toString());
        testProps.setProperty("storageLocationMappings.path", storageLocationMappings.toString());
        testProps.setProperty("fits.homePath", fitsHomePath.toString());

        Path testPropsFile = Paths.get("target/test-deposit.properties");
        Files.createDirectories(testPropsFile.getParent());
        try (OutputStream out = Files.newOutputStream(testPropsFile)) {
            testProps.store(out, "Test properties");
        }
        System.setProperty("deposit.properties.uri", testPropsFile.toAbsolutePath().toUri().toString());
        System.setProperty("acl.properties.uri", aclProperties.toAbsolutePath().toUri().toString());
        System.setProperty("fcrepo.baseUri", "http://localhost:48085/fcrepo/rest");
    }

    @BeforeEach
    public void setup() throws Exception {
        // Clear out redis so we don't run into previous state
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
            jedis.flushDB();
        }
        depositCoordinator.init();
        depositHandler = new FileServerDepositHandler();
        depositHandler.setDepositsDirectory(depositsDir.toFile());
        depositHandler.setDepositStatusFactory(depositStatusFactory);
        depositHandler.setDepositOperationMessageService(messageService);
        depositHandler.setPidMinter(pidminter);
        ScanResult defaultScanResult = Mockito.mock(ScanResult.class);
        when(defaultScanResult.getStatus()).thenReturn(ScanResult.Status.PASSED);
        when(clamavClient.scanWithResult(any(Path.class))).thenReturn(defaultScanResult);

        setupFitsCommand("src/test/resources/fitsReports/textReport.xml");
    }

    @Test
    public void testFullPipeline() throws Exception {
        var agent = new AgentPrincipalsImpl("testUser", new AccessGroupSetImpl("admingroup"));
        var rootObject = repositoryObjectLoader.getContentRootObject(RepositoryPaths.getContentRootPid());
        var adminUnit = repositoryObjectFactory.createAdminUnit(null);
        var collection = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collection);
        rootObject.addMember(adminUnit);

        Path sampleDeposit = depositsDir.resolve("deposit_" + System.currentTimeMillis());
        Files.createDirectories(sampleDeposit);
        Files.writeString(sampleDeposit.resolve("test.txt"), "This is a test deposit file.");

        var depositData = new DepositData(sampleDeposit.toUri(),
                null,
                PackagingType.DIRECTORY,
                DepositMethod.CDRAPI1.getLabel(),
                agent);
        PID depositPid = depositHandler.doDeposit(collection.getPid(), depositData);
        String depositId = depositPid.getId();

        awaitDepositState(depositId, DepositState.running);
        awaitNoActiveJobs(depositId);

        awaitJobSuccessful(depositId, DirectoryToBagJob.class);
        awaitJobSuccessful(depositId, ValidateDestinationJob.class);
        awaitJobSuccessful(depositId, ValidateContentModelJob.class);
        awaitJobSuccessful(depositId, ValidateFileAvailabilityJob.class);
        awaitJobSuccessful(depositId, VirusScanJob.class);
        awaitJobSuccessful(depositId, FixityCheckJob.class);
        awaitJobSuccessful(depositId, ExtractTechnicalMetadataJob.class);
        awaitJobSuccessful(depositId, TransferBinariesToStorageJob.class);
        awaitJobSuccessful(depositId, IngestDepositRecordJob.class);
        awaitJobSuccessful(depositId, IngestContentObjectsJob.class, 10);

        awaitDepositState(depositId, DepositState.finished);
    }

    @Test
    public void testFullPipelinePauseAndResume() throws Exception {
        var agent = new AgentPrincipalsImpl("testUser", new AccessGroupSetImpl("admingroup"));
        var rootObject = repositoryObjectLoader.getContentRootObject(RepositoryPaths.getContentRootPid());
        var adminUnit = repositoryObjectFactory.createAdminUnit(null);
        var collection = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collection);
        rootObject.addMember(adminUnit);

        Path sampleDeposit = depositsDir.resolve("deposit_" + System.currentTimeMillis());
        Files.createDirectories(sampleDeposit);
        Files.writeString(sampleDeposit.resolve("test.txt"), "This is a test deposit file.");

        var depositData = new DepositData(sampleDeposit.toUri(),
                null,
                PackagingType.DIRECTORY,
                DepositMethod.CDRAPI1.getLabel(),
                agent);
        PID depositPid = depositHandler.doDeposit(collection.getPid(), depositData);
        String depositId = depositPid.getId();

        awaitDepositState(depositId, DepositState.running);
        awaitJobSuccessful(depositId, DirectoryToBagJob.class);

        var pauseMessage = new DepositOperationMessage(DepositOperation.PAUSE, depositId, agent.getUsername());
        messageService.sendDepositOperationMessage(pauseMessage);

        awaitDepositState(depositId, DepositState.paused);
        // Wait for any active jobs to complete
        awaitNoActiveJobs(depositId);
        // Pausing should have happened before the last job completed
        assertFalse(isJobSuccessful(depositId, IngestContentObjectsJob.class));

        var resumeMessage = new DepositOperationMessage(DepositOperation.RESUME, depositId, agent.getUsername());
        messageService.sendDepositOperationMessage(resumeMessage);

        awaitDepositState(depositId, DepositState.running);
        awaitNoActiveJobs(depositId);

        awaitJobSuccessful(depositId, ValidateDestinationJob.class);
        awaitJobSuccessful(depositId, ValidateContentModelJob.class);
        awaitJobSuccessful(depositId, ValidateFileAvailabilityJob.class);
        awaitJobSuccessful(depositId, VirusScanJob.class);
        awaitJobSuccessful(depositId, FixityCheckJob.class);
        awaitJobSuccessful(depositId, ExtractTechnicalMetadataJob.class);
        awaitJobSuccessful(depositId, TransferBinariesToStorageJob.class);
        awaitJobSuccessful(depositId, IngestDepositRecordJob.class);
        awaitJobSuccessful(depositId, IngestContentObjectsJob.class, 10);

        awaitDepositState(depositId, DepositState.finished);
    }

    private void awaitDepositState(String depositId, DepositState expectedState) {
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> expectedState.equals(depositStatusFactory.getState(depositId)));
    }

    private void awaitJobSuccessful(String depositId, Class<?> jobClass) {
        awaitJobSuccessful(depositId, jobClass, 5);
    }

    private void awaitJobSuccessful(String depositId, Class<?> jobClass, long timeoutSeconds) {
        Awaitility.await().atMost(Duration.ofSeconds(timeoutSeconds))
                .until(() -> isJobSuccessful(depositId, jobClass));
    }

    private void awaitNoActiveJobs(String depositId) {
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> !jobCoordinator.hasActiveJobs());
    }

    private boolean isJobSuccessful(String depositId, Class<?> jobClass) {
        return jobStatusFactory.getSuccessfulJobNames(depositId).contains(jobClass.getName());
    }

    private void setupFitsCommand(String docPath) throws IOException {
        Path fitsCommand = fitsHomePath.resolve("fits.sh");
        Files.deleteIfExists(fitsCommand);
        Files.createFile(fitsCommand);
        Files.writeString(fitsCommand, "#!/usr/bin/env bash\n"
                + "cat " + Paths.get(docPath).toAbsolutePath() + "\n"
                + "exit 0", StandardCharsets.US_ASCII);
        Files.setPosixFilePermissions(fitsCommand, PosixFilePermissions.fromString("rwxr--r--"));
    }
}
