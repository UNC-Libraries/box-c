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
package edu.unc.lib.dl.cdr.services.sword.managers;

import java.io.IOException;

import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.PreIngestEventLogger;
import edu.unc.lib.dl.services.DigitalObjectManager;

/**
 * 
 * @author bbpennel
 * 
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {

	private DigitalObjectManager digitalObjectManager;
	private AgentManager agentManager;

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");

		try {
			Agent agent = agentManager.findPersonByOnyen("bbpennel", true);

			PID pid = new PID(collectionURI);
			String containerPath = this.tripleStoreQueryService.lookupRepositoryPath(pid);
			METSPackageSIP sip = new METSPackageSIP("/Collections/", deposit.getFile(), agent, isZip);
			//PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

			digitalObjectManager.add(sip, agent, "Added through SWORD", false);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IngestException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}
}
