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

import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 * Interface for deposit handlers
 * 
 * @author bbpennel
 * 
 */
public interface DepositHandler {
    /**
     * Ingests the provided Deposit object into the container destination
     * 
     * @param destination
     *           PID of the container which will become the parent of the deposit
     * @param deposit
     * @param type
     *           packaging type of the deposit
     * @param priority
     *           priority level for the submitted deposit
     * @param config
     * @param depositor
     *           username of the depositor
     * @param owner
     *           username of the owner to assign to the deposit
     * @return
     * @throws Exception
     */
    public DepositReceipt doDeposit(PID destination, Deposit deposit, PackagingType type, Priority priority,
            SwordConfiguration config, String depositor, String owner) throws Exception;
}
