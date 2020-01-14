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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.util.DepositException;

/**
 * Handler for deposit packages located in place on a file server.
 *
 * @author bbpennel
 *
 */
public class FileServerDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(FileServerDepositHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a deposit from file server of type {} to {}",
                deposit.getPackagingType(), destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        registerDeposit(depositPID, destination, deposit, null);
        return depositPID;
    }

}
