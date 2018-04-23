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
package edu.unc.lib.dl.cdr.sword.server.deposit;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 *
 * @author bbpennel
 *
 */
public class SimpleObjectDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory
            .getLogger(SimpleObjectDepositHandler.class);

    @Override
    public DepositReceipt doDeposit(PID destination, Deposit deposit,
            PackagingType type, Priority priority, SwordConfiguration config,
            String depositor, String owner) throws Exception {
        log.debug("Preparing to perform a Simple Object deposit to "
                + destination.getPid());

        PID depositPID = null;
        UUID depositUUID = UUID.randomUUID();
        depositPID = new PID("uuid:" + depositUUID.toString());
        File dir = makeNewDepositDirectory(depositPID.getUUID());
        dir.mkdir();

        // write deposit file to data directory
        if (deposit.getFile() != null) {
            File dataDir = new File(dir, "data");
            dataDir.mkdir();
            File depositFile = new File(dataDir, URLDecoder.decode(deposit.getFilename(), "UTF-8"));
            try {
                FileUtils.moveFile(deposit.getFile(), depositFile);
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        // Skip deposit record for this tiny ingest
        Map<String, String> options = new HashMap<String, String>();
        options.put(DepositField.excludeDepositRecord.name(), "true");

        registerDeposit(depositPID, destination, deposit,
                type, Priority.high, depositor, owner, options);
        return buildReceipt(depositPID, config);
    }
}
