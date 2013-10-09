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

import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.ErrorURIRegistry;

public abstract class AbstractDepositHandler implements DepositHandler {
	@Autowired
	protected DepositReportingUtil depositReportingUtil;
	@Autowired
	protected DigitalObjectManager digitalObjectManager;

	public DepositReportingUtil getDepositReportingUtil() {
		return depositReportingUtil;
	}

	public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
		this.depositReportingUtil = depositReportingUtil;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	protected DepositReceipt buildReceipt(IngestResult ingestResult, SwordConfiguration config) throws SwordError {
		if (ingestResult == null || ingestResult.derivedPIDs == null || ingestResult.derivedPIDs.size() == 0) {
			throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400, "Add batch request "
					+ ingestResult.originalDepositID.getPid() + " did not return any derived results.");
		}

		DepositReceipt receipt = depositReportingUtil.retrieveDepositReceipt(ingestResult,
				(SwordConfigurationImpl) config);
		return receipt;
	}
}