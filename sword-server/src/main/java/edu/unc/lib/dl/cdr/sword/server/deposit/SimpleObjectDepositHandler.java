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

import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.SingleFileSIP;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PackagingType;

public class SimpleObjectDepositHandler extends AbstractDepositHandler {
	private static Logger log = Logger.getLogger(SimpleObjectDepositHandler.class);

	@Override
	public DepositReceipt doDeposit(PID destination, Deposit deposit, PackagingType type, SwordConfiguration config,
			Agent agent, Agent owner) throws Exception {
		log.debug("Preparing to perform a Simple Object deposit to " + destination.getPid());
		String label = deposit.getSlug();
		if (label == null || label.trim().length() == 0)
			label = deposit.getFilename();
		SingleFileSIP sip = new SingleFileSIP(destination, deposit.getFile(), deposit.getMimeType(), label,
				deposit.getMd5());

		DepositRecord record = new DepositRecord(agent, owner, DepositMethod.SWORD13);
		record.setMessage("Added through SWORD");
		record.setPackagingType(type);
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);

		return buildReceipt(ingestResult, config);
	}

}
