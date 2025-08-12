package edu.unc.lib.boxc.deposit.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.IngestContentObjectsJob;
import edu.unc.lib.boxc.deposit.fcrepo4.IngestDepositRecordJob;
import edu.unc.lib.boxc.deposit.fcrepo4.StaffOnlyPermissionJob;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.normalize.AssignStorageLocationsJob;
import edu.unc.lib.boxc.deposit.normalize.BagIt2N3BagJob;
import edu.unc.lib.boxc.deposit.normalize.CDRMETS2N3BagJob;
import edu.unc.lib.boxc.deposit.normalize.DirectoryToBagJob;
import edu.unc.lib.boxc.deposit.normalize.NormalizeFileObjectsJob;
import edu.unc.lib.boxc.deposit.normalize.PreconstructedDepositJob;
import edu.unc.lib.boxc.deposit.normalize.Simple2N3BagJob;
import edu.unc.lib.boxc.deposit.normalize.UnpackDepositJob;
import edu.unc.lib.boxc.deposit.normalize.WorkFormToBagJob;
import edu.unc.lib.boxc.deposit.transfer.TransferBinariesToStorageJob;
import edu.unc.lib.boxc.deposit.validate.ExtractTechnicalMetadataJob;
import edu.unc.lib.boxc.deposit.validate.FixityCheckJob;
import edu.unc.lib.boxc.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.boxc.deposit.validate.ValidateContentModelJob;
import edu.unc.lib.boxc.deposit.validate.ValidateDescriptionJob;
import edu.unc.lib.boxc.deposit.validate.ValidateDestinationJob;
import edu.unc.lib.boxc.deposit.validate.ValidateFileAvailabilityJob;
import edu.unc.lib.boxc.deposit.validate.VirusScanJob;
import edu.unc.lib.boxc.deposit.work.DepositFailedException;
import edu.unc.lib.boxc.persist.api.PackagingType;

@ExtendWith(MockitoExtension.class)
public class DepositJobMessageFactoryTest {

    private DepositJobMessageFactory factory;

    @Mock
    private JobStatusFactory jobStatusFactory;

    private final String DEPOSIT_ID = "deposit123";
    private Map<String, String> depositStatus;
    private List<String> successfulJobs;

    @BeforeEach
    public void setup() {
        factory = new DepositJobMessageFactory();
        factory.setJobStatusFactory(jobStatusFactory);

        depositStatus = new HashMap<>();
        successfulJobs = new ArrayList<>();
        when(jobStatusFactory.getSuccessfulJobNames(DEPOSIT_ID)).thenReturn(successfulJobs);
    }

    @Test
    public void testPackageIntegrityCheckJobFirst() {
        depositStatus.put(DepositField.depositMd5.name(), "abc123");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(DEPOSIT_ID, result.getDepositId());
        assertEquals(PackageIntegrityCheckJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testUnpackDepositJobForZipFile() {
        depositStatus.put(DepositField.fileName.name(), "test.zip");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.name());
        successfulJobs.add(PackageIntegrityCheckJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(UnpackDepositJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSkipUnpackForSimpleObjectZip() {
        depositStatus.put(DepositField.fileName.name(), "test.zip");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(Simple2N3BagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSkipUnpackForNonZipFile() {
        depositStatus.put(DepositField.fileName.name(), "test.tar");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(BagIt2N3BagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testPreconstructedDepositJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAG_WITH_N3.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(PreconstructedDepositJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testCDRMETS2N3BagJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.METS_CDR.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(CDRMETS2N3BagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSimple2N3BagJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(Simple2N3BagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testBagIt2N3BagJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(BagIt2N3BagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testDirectoryToBagJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.DIRECTORY.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(DirectoryToBagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testWorkFormToBagJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.WORK_FORM_DEPOSIT.name());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(WorkFormToBagJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testNormalizeFileObjectsJobForMETSCDR() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.METS_CDR.name());
        successfulJobs.add(CDRMETS2N3BagJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(NormalizeFileObjectsJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSkipNormalizeFileObjectsJobForNonMETSCDR() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        successfulJobs.add(Simple2N3BagJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(ValidateDestinationJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testValidationJobSequence() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        successfulJobs.add(Simple2N3BagJob.class.getName());

        // Test ValidateDestinationJob
        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(ValidateDestinationJob.class.getName(), result.getJobClassName());

        successfulJobs.add(ValidateDestinationJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(ValidateContentModelJob.class.getName(), result.getJobClassName());

        successfulJobs.add(ValidateContentModelJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(ValidateDescriptionJob.class.getName(), result.getJobClassName());

        successfulJobs.add(ValidateDescriptionJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(ValidateFileAvailabilityJob.class.getName(), result.getJobClassName());

        successfulJobs.add(ValidateFileAvailabilityJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(VirusScanJob.class.getName(), result.getJobClassName());

        successfulJobs.add(VirusScanJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(FixityCheckJob.class.getName(), result.getJobClassName());

        successfulJobs.add(FixityCheckJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(ExtractTechnicalMetadataJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testIngestJobSequence() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        addAllValidationJobs();

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(AssignStorageLocationsJob.class.getName(), result.getJobClassName());

        successfulJobs.add(AssignStorageLocationsJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(TransferBinariesToStorageJob.class.getName(), result.getJobClassName());

        successfulJobs.add(TransferBinariesToStorageJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(IngestDepositRecordJob.class.getName(), result.getJobClassName());

        successfulJobs.add(IngestDepositRecordJob.class.getName());
        result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        assertEquals(IngestContentObjectsJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testStaffOnlyPermissionJob() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        depositStatus.put(DepositField.staffOnly.name(), "true");
        addAllValidationJobs();
        successfulJobs.add(AssignStorageLocationsJob.class.getName());
        successfulJobs.add(TransferBinariesToStorageJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(StaffOnlyPermissionJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSkipStaffOnlyPermissionJobWhenFalse() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        depositStatus.put(DepositField.staffOnly.name(), "false");
        addAllValidationJobs();
        successfulJobs.add(AssignStorageLocationsJob.class.getName());
        successfulJobs.add(TransferBinariesToStorageJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(IngestDepositRecordJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testSkipIngestDepositRecordWhenExcluded() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        depositStatus.put(DepositField.excludeDepositRecord.name(), "true");
        addAllValidationJobs();
        successfulJobs.add(AssignStorageLocationsJob.class.getName());
        successfulJobs.add(TransferBinariesToStorageJob.class.getName());

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(IngestContentObjectsJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testCleanupJobLast() {
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());
        addAllJobs();

        DepositJobMessage result = factory.createNextJobMessage(DEPOSIT_ID, depositStatus);

        assertEquals(CleanupDepositJob.class.getName(), result.getJobClassName());
    }

    @Test
    public void testInvalidPackagingType() {
        depositStatus.put(DepositField.packagingType.name(), "INVALID_TYPE");

        DepositFailedException exception = assertThrows(DepositFailedException.class, () -> {
            factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        });

        assertEquals("Cannot convert deposit package to N3 BagIt. No converter for this packaging type(s): INVALID_TYPE",
                exception.getMessage());
    }

    @Test
    public void testInvalidSuccessfulJobs() {
        successfulJobs.add("InvalidJobClass");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.SIMPLE_OBJECT.name());

        DepositFailedException exception = assertThrows(DepositFailedException.class, () -> {
            factory.createNextJobMessage(DEPOSIT_ID, depositStatus);
        });

        assertEquals("Deposit " + DEPOSIT_ID + " lists invalid 'successful jobs', it may be out of date and require updating: [InvalidJobClass]",
                exception.getMessage());
    }

    private void addAllValidationJobs() {
        successfulJobs.add(Simple2N3BagJob.class.getName());
        successfulJobs.add(ValidateDestinationJob.class.getName());
        successfulJobs.add(ValidateContentModelJob.class.getName());
        successfulJobs.add(ValidateDescriptionJob.class.getName());
        successfulJobs.add(ValidateFileAvailabilityJob.class.getName());
        successfulJobs.add(VirusScanJob.class.getName());
        successfulJobs.add(FixityCheckJob.class.getName());
        successfulJobs.add(ExtractTechnicalMetadataJob.class.getName());
    }

    private void addAllJobs() {
        addAllValidationJobs();
        successfulJobs.add(AssignStorageLocationsJob.class.getName());
        successfulJobs.add(TransferBinariesToStorageJob.class.getName());
        successfulJobs.add(IngestDepositRecordJob.class.getName());
        successfulJobs.add(IngestContentObjectsJob.class.getName());
    }
}