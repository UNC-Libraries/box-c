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
package edu.unc.lib.boxc.deposit.impl.submit;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * @author bbpennel
 */
public class PreconstructedDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(PreconstructedDepositHandler.class);

    private PID depositPID;

    public PreconstructedDepositHandler(PID depositPID) {
        this.depositPID = depositPID;
    }

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a Preconstructed deposit to {}",
                destination.getQualifiedId());

        Map<String, String> options = new HashMap<>();
        options.put(DepositField.excludeDepositRecord.name(), "true");

        registerDeposit(depositPID, destination, deposit, options);

        return depositPID;
    }

}
