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

import java.io.File;
import java.io.IOException;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.ZipFileUtil;

/**
 * Unpacks the submission package into the deposit directory.
 * @author count0
 *
 */
public class UnpackDepositJob extends AbstractDepositJob {

    public UnpackDepositJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    public void runJob() {
        // unzip deposit file to directory
        String filename = getDepositStatus().get(DepositField.fileName.name());
        if (filename.toLowerCase().endsWith(".zip")) {
            File depositFile = new File(getDataDirectory(), filename);
            try {
                ZipFileUtil.unzipToDir(depositFile, getDataDirectory());
            } catch (IOException e) {
                throw new Error("Unable to unpack your deposit: " + getDepositPID().getUUID(), e);
            }
        }
    }

}
