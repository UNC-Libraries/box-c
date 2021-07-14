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

import java.util.Map;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.api.ingest.DepositHandler;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.UnsupportedPackagingTypeException;

/**
 * Service which submits deposits to the deposit pipeline based on packaging type
 *
 * @author bbpennel
 *
 */
public class DepositSubmissionService {
    private Map<PackagingType, DepositHandler> packageHandlers;
    private AccessControlService aclService;

    /**
     * Submits deposit to the deposit pipeline based on the packaging type provided
     * in the DepositData object
     *
     * @param destPid PID of the container the deposit will go to
     * @param deposit details of the deposit
     * @return PID of the deposit
     * @throws DepositException
     */
    public PID submitDeposit(PID destPid, DepositData deposit) throws DepositException {
        aclService.assertHasAccess("Insufficient permissions to deposit to " + destPid.getRepositoryPath(),
                destPid, deposit.getDepositingAgent().getPrincipals(), Permission.ingest);

        PackagingType type = deposit.getPackagingType();

        DepositHandler depositHandler = packageHandlers.get(type);
        if (type == null || depositHandler == null) {
            throw new UnsupportedPackagingTypeException("Cannot perform deposit of type " + type);
        }

        return depositHandler.doDeposit(destPid, deposit);
    }

    /**
     * @param packageHandlers the packageHandlers to set
     */
    public void setPackageHandlers(Map<PackagingType, DepositHandler> packageHandlers) {
        this.packageHandlers = packageHandlers;
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }
}
