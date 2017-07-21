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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.SpringJobFactory;

public class PackageIntegrityCheckJobTest extends AbstractNormalizationJobTest {

    private PackageIntegrityCheckJob job;
    
    @Before
    public void setup() {
        job = new PackageIntegrityCheckJob(jobUUID, depositUUID);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "dataset", dataset);
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
    
    @Test(expected = JobFailedException.class)
    public void testFileCorrupted() {
        DepositTestUtils.makeTestDir(
                depositsDirectory,
                depositUUID, new File("src/test/resources/depositFileZipped.zip"));
        
        Map<String, String> status = new HashMap<>();
        status.put(DepositField.depositMd5.name(), "a949138500f67e8617ac9968d2632d4e");
        status.put(DepositField.fileName.name(), "cdrMETS.zip");
        when(depositStatusFactory.get(anyString())).thenReturn(status);
        
        job.run();
    }
}
