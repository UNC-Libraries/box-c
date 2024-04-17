package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.deposit.DepositTestUtils;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.normalize.UnpackDepositJob;

public class UnpackDepositJobTest extends AbstractDepositJobTest {

    private UnpackDepositJob job;

    @BeforeEach
    public void setup() {
        job = new UnpackDepositJob(jobUUID, depositUUID);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositModelManager", depositModelManager);
        job.init();
    }

    @Test
    public void test() {
        String workDir = DepositTestUtils.makeTestDir(
                depositsDirectory,
                depositUUID, new File("src/test/resources/depositFileZipped.zip"));

        Map<String, String> status = new HashMap<>();
        status.put(DepositField.fileName.name(), "cdrMETS.zip");
        when(depositStatusFactory.get(anyString())).thenReturn(status);

        job.run();

        File metsFile = new File(workDir, "data/mets.xml");
        assertTrue(metsFile.exists(), "METS file must exist after unpacking");
    }
}
