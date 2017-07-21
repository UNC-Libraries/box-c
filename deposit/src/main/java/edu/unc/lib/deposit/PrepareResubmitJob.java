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

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;

/**
 * 
 * @author mdaines
 *
 */
public class PrepareResubmitJob extends AbstractDepositJob {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupDepositJob.class);

    public PrepareResubmitJob() {
    }

    public PrepareResubmitJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        String uuid = getDepositUUID();
        Map<String, String> status = getDepositStatus();

        File resubmitDir = new File(getDepositsDirectory(), status.get(DepositField.resubmitDirName.name()));
        File backupDir = new File(resubmitDir, DepositConstants.RESUBMIT_BACKUP_DIR);
        File depositDirectory = getDepositDirectory();

        // Swap directories: existing deposit directory becomes backup directory inside resubmit directory,
        // resubmit directory becomes former deposit directory.
        depositDirectory.renameTo(backupDir);
        resubmitDir.renameTo(depositDirectory);

        // Set isResubmit flag, update the fileName, and remove resubmit fields.
        DepositStatusFactory depositStatusFactory = getDepositStatusFactory();
        depositStatusFactory.set(uuid, DepositField.isResubmit, "true");
        depositStatusFactory.set(uuid, DepositField.fileName, status.get(DepositField.resubmitFileName.name()));
        depositStatusFactory.deleteField(uuid, DepositField.resubmitDirName);
        depositStatusFactory.deleteField(uuid, DepositField.resubmitFileName);

        // Destroy our model, since we'll recreate it in later steps
        this.destroyModel();

    }

}
