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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.MetsHeaderScanner;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 *
 * @author bbpennel
 *
 */
public class CDRMETSDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(CDRMETSDepositHandler.class);

    @Override
    public DepositReceipt doDeposit(PID destination, Deposit deposit, PackagingType type, Priority priority,
            SwordConfiguration config, String depositor, String owner) throws SwordError {
        if (log.isDebugEnabled()) {
            log.debug("Preparing to perform a CDR METS deposit to " + destination.getPid());
            log.debug("Working with temporary file: " + deposit.getFile().getAbsolutePath());
        }

        // extract info from METS header
        MetsHeaderScanner scanner = new MetsHeaderScanner();
        try {
            scanner.scan(deposit.getFile(), deposit.getFilename());
        } catch (Exception e1) {
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400,
                    "Unable to parse your METS file: " + deposit.getFilename(), e1);
        }

        PID depositPID = scanner.getObjID();
        if (depositPID == null) {
            UUID depositUUID = UUID.randomUUID();
            depositPID = new PID("uuid:" + depositUUID.toString());
        }
        File dir = makeNewDepositDirectory(depositPID.getUUID());

        // drop upload in data directory
        try {
            File data = new File(dir, "data");
            data.mkdir();
            FileUtils.moveFile(deposit.getFile(), new File(data, deposit.getFilename()));
        } catch (IOException e) {
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500,
                    "Unable to create your deposit bag: " + depositPID.getPid(), e);
        }

        // METS specific fields
        Map<String, String> status = new HashMap<String, String>();
        status.put(DepositField.packageProfile.name(), scanner.getProfile());
        status.put(DepositField.metsType.name(), scanner.getType());
        status.put(DepositField.createTime.name(), scanner.getCreateDate());
        status.put(DepositField.intSenderDescription.name(), StringUtils.join(scanner.getNames(), ','));

        registerDeposit(depositPID, destination, deposit, type, priority, depositor, owner, status);
        return buildReceipt(depositPID, config);
    }
}