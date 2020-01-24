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
package edu.unc.lib.dl.persist.api.ingest;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositException;

/**
 * Interface for a deposit handler used to submit deposits to the deposit pipeline
 *
 * @author bbpennel
 *
 */
public interface DepositHandler {
    /**
     * Perform this deposit handler, submitting a deposit request to the pipeline
     *
     * @param destination PID of the object to deposit into
     * @param deposit details about the deposit
     * @return PID of the deposit
     * @throws DepositException
     */
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException;
}
