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
package edu.unc.lib.deposit.validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 *
 * @author count0
 *
 */
public class PackageIntegrityCheckJob extends AbstractDepositJob {

    /**
     * Verifies the integrity of the deposit file received from SWORD.
     * @param uuid
     * @param depositUUID
     */
    public PackageIntegrityCheckJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    public PackageIntegrityCheckJob() {
        super();
    }

    @Override
    public void runJob() {
        Map<String, String> status = getDepositStatus();
        String md5 = status.get(DepositField.depositMd5.name());
        String file = status.get(DepositField.fileName.name());
        if (md5 != null && file != null) {
            File payload = new File(getSubdir("data"), file);
            String computed = null;
            try (InputStream is = new FileInputStream(payload)) {
                computed = DigestUtils.md5Hex(is);
            } catch (IOException e) {
                failJob(e, "Unable to read deposit file while verifying package");
            }

            if (!md5.equals(computed)) {
                String msg = MessageFormat.format(
                        "The computed checksum ({1}) did not match the one provided ({0}) for \"{2}\".", md5, computed,
                        file);
                failJob("The deposit file \"" + file + "\" was corrupted during submission or upload.", msg);
            }
        }
    }

}
