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
package edu.unc.lib.dl.persist.services.ingest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.MetsHeaderScanner;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 *  Deposit handler for METS submissions which follow the CDR profile
 *
 * @author bbpennel
 *
 */
public class CDRMETSDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(CDRMETSDepositHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a DCR METS deposit to {}", destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        File metsFile = writeStreamToDataDir(depositPID, deposit);
        deposit.setSourceUri(metsFile.toPath().normalize().toUri());

        // extract info from METS header
        MetsHeaderScanner scanner = new MetsHeaderScanner();
        try {
            scanner.scan(metsFile, deposit.getFilename());
        } catch (Exception e1) {
            throw new DepositException(
                    "Unable to parse your METS file: " + deposit.getFilename(), e1);
        }

        // METS specific fields
        Map<String, String> status = new HashMap<>();
        status.put(DepositField.packageProfile.name(), scanner.getProfile());
        status.put(DepositField.metsType.name(), scanner.getType());
        status.put(DepositField.createTime.name(), scanner.getCreateDate());
        status.put(DepositField.intSenderDescription.name(), StringUtils.join(scanner.getNames(), ','));

        registerDeposit(depositPID, destination, deposit, status);
        return depositPID;
    }
}