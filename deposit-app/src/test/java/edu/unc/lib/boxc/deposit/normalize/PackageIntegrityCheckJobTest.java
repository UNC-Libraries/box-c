package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.deposit.DepositTestUtils;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.utils.SpringJobFactory;
import edu.unc.lib.boxc.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;

public class PackageIntegrityCheckJobTest extends AbstractNormalizationJobTest {

    private PackageIntegrityCheckJob job;

    @BeforeEach
    public void setup() {
        job = new PackageIntegrityCheckJob(jobUUID, depositUUID);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositModelManager", depositModelManager);
        job.init();
    }

    @Autowired
    SpringJobFactory springJobFactory = null;

    @Test
    public void test() {
        DepositTestUtils.makeTestDir(
                depositsDirectory,
                depositUUID, new File("src/test/resources/depositFileZipped.zip"));

        Map<String, String> status = new HashMap<>();
        status.put(DepositField.depositMd5.name(), "c949138500f67e8617ac9968d2632d4e");
        status.put(DepositField.fileName.name(), "cdrMETS.zip");
        when(depositStatusFactory.get(anyString())).thenReturn(status);

        job.run();
    }

    @Test
    public void testFileCorrupted() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            DepositTestUtils.makeTestDir(
                    depositsDirectory,
                    depositUUID, new File("src/test/resources/depositFileZipped.zip"));

            Map<String, String> status = new HashMap<>();
            status.put(DepositField.depositMd5.name(), "a949138500f67e8617ac9968d2632d4e");
            status.put(DepositField.fileName.name(), "cdrMETS.zip");
            when(depositStatusFactory.get(anyString())).thenReturn(status);

            job.run();
        });
    }
}
