package edu.unc.lib.boxc.deposit.jms;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bbpennel
 */
public class DepositJobMessageFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DepositJobMessageFactory.class);
    private static final Set<String> VALID_DEPOSIT_JOBS = Stream.of(
            PackageIntegrityCheckJob.class,
            UnpackDepositJob.class,
            PreconstructedDepositJob.class,
            CDRMETS2N3BagJob.class,
            Simple2N3BagJob.class,
            BagIt2N3BagJob.class,
            DirectoryToBagJob.class,
            NormalizeFileObjectsJob.class,
            ValidateDestinationJob.class,
            ValidateContentModelJob.class,
            ValidateDescriptionJob.class,
            ValidateFileAvailabilityJob.class,
            VirusScanJob.class,
            FixityCheckJob.class,
            ExtractTechnicalMetadataJob.class,
            AssignStorageLocationsJob.class,
            TransferBinariesToStorageJob.class,
            StaffOnlyPermissionJob.class,
            IngestDepositRecordJob.class,
            IngestContentObjectsJob.class,
            CleanupDepositJob.class,
            WorkFormToBagJob.class
    ).map(Class::getName).collect(Collectors.toSet());

    private static final Map<PackagingType, Class<?>> PACKAGING_TYPE_TO_JOB_CLASS = new EnumMap(Map.of(
            PackagingType.BAG_WITH_N3, PreconstructedDepositJob.class,
            PackagingType.METS_CDR, CDRMETS2N3BagJob.class,
            PackagingType.SIMPLE_OBJECT, Simple2N3BagJob.class,
            PackagingType.BAGIT, BagIt2N3BagJob.class,
            PackagingType.DIRECTORY, DirectoryToBagJob.class,
            PackagingType.WORK_FORM_DEPOSIT, WorkFormToBagJob.class
    ));

    private JobStatusFactory jobStatusFactory;

    public DepositJobMessage createNextJobMessage(String depositId, Map<String, String> status) {
        DepositJobMessage jobMessage = new DepositJobMessage();
        jobMessage.setDepositId(depositId);

        var successfulJobs = new HashSet<>(this.jobStatusFactory.getSuccessfulJobNames(depositId));

        LOG.debug("Got completed job names: {}", successfulJobs);
        if (!VALID_DEPOSIT_JOBS.containsAll(successfulJobs)) {
            throw new DepositFailedException("Deposit " + depositId + " lists invalid 'successful jobs',"
                    + " it may be out of date and require updating: " + successfulJobs);
        }

        Class<?> nextJobClass = getNextJobClass(status, successfulJobs);
        jobMessage.setJobClassName(nextJobClass.getName());

        return jobMessage;
    }

    private Class<?> getNextJobClass(Map<String, String> status, Set<String> successfulJobs) {
        if (status.get(DepositField.depositMd5.name()) != null) {
            if (notComplete(successfulJobs, PackageIntegrityCheckJob.class)) {
                return PackageIntegrityCheckJob.class;
            }
        }

        // Package may be unpacked
        String filename = status.get(DepositField.fileName.name());
        String packagingValue = status.get(DepositField.packagingType.name());
        PackagingType packagingType = PackagingType.getPackagingType(packagingValue);
        if (packagingType == null) {
            String msg = MessageFormat.format("Cannot convert deposit package to N3 BagIt."
                    + " No converter for this packaging type(s): {0}", packagingValue);
            throw new DepositFailedException(msg);
        }

        if (filename != null && filename.toLowerCase().endsWith(".zip") &&
                !PackagingType.SIMPLE_OBJECT.equals(packagingType)) {
            if (notComplete(successfulJobs, UnpackDepositJob.class)) {
                return UnpackDepositJob.class;
            }
        }

        var conversionClass = PACKAGING_TYPE_TO_JOB_CLASS.get(packagingType);
        if (notComplete(successfulJobs, conversionClass)) {
            return conversionClass;
        }

        // Normalize all fileObjects into Works. Only applying to METS packaging, other types should already be valid
        if (notComplete(successfulJobs, NormalizeFileObjectsJob.class)
                && PackagingType.METS_CDR.equals(packagingType)) {
            return NormalizeFileObjectsJob.class;
        }

        // Verify that the destination can receive the deposit
        if (notComplete(successfulJobs, ValidateDestinationJob.class)) {
            return ValidateDestinationJob.class;
        }

        // Validate object structure and properties
        if (notComplete(successfulJobs, ValidateContentModelJob.class)) {
            return ValidateContentModelJob.class;
        }

        // MODS validation
        if (notComplete(successfulJobs, ValidateDescriptionJob.class)) {
            return ValidateDescriptionJob.class;
        }

        // Validate file availability
        if (notComplete(successfulJobs, ValidateFileAvailabilityJob.class)) {
            return ValidateFileAvailabilityJob.class;
        }

        // Virus Scan
        if (notComplete(successfulJobs, VirusScanJob.class)) {
            return VirusScanJob.class;
        }

        // Verify/calculate checksums
        if (notComplete(successfulJobs, FixityCheckJob.class)) {
            return FixityCheckJob.class;
        }

        // Extract technical metadata
        if (notComplete(successfulJobs, ExtractTechnicalMetadataJob.class)) {
            return ExtractTechnicalMetadataJob.class;
        }

        // Assign storage locations
        if (notComplete(successfulJobs, AssignStorageLocationsJob.class)) {
            return AssignStorageLocationsJob.class;
        }

        // Transfer binaries to storage locations
        if (notComplete(successfulJobs, TransferBinariesToStorageJob.class)) {
            return TransferBinariesToStorageJob.class;
        }

        // Mark objects staff only if flag is set
        boolean runStaffOnlyJob = Boolean.parseBoolean(status.get(DepositField.staffOnly.name()));
        if (runStaffOnlyJob && notComplete(successfulJobs, StaffOnlyPermissionJob.class)) {
            return StaffOnlyPermissionJob.class;
        }

        boolean excludeDepositRecord = Boolean.parseBoolean(status.get(DepositField.excludeDepositRecord.name()));
        // Ingest the deposit record
        if (!excludeDepositRecord && notComplete(successfulJobs, IngestDepositRecordJob.class)) {
            return IngestDepositRecordJob.class;
        }

        // Ingest all content objects to repository
        if (notComplete(successfulJobs, IngestContentObjectsJob.class)) {
            return IngestContentObjectsJob.class;
        }

        return CleanupDepositJob.class;
    }

    private boolean notComplete(Set<String> successfulJobs, Class<?> jobClass) {
        return !successfulJobs.contains(jobClass.getName());
    }
}
