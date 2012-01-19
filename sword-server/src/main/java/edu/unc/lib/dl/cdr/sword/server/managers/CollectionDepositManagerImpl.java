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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.log4j.Logger;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.PackagingType;

/**
 * 
 * @author bbpennel
 * 
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {
	private static Logger LOG = Logger.getLogger(CollectionDepositManagerImpl.class);

	private DigitalObjectManager digitalObjectManager;
	private AgentFactory agentFactory;

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

		try {
			Agent agent = agentFactory.findPersonByOnyen("bbpennel", true);
			SwordConfigurationImpl configImpl = (SwordConfigurationImpl)config;

			PID containerPID = null;
			if (collectionURI == null || collectionURI.trim().length() == 0){
				containerPID = collectionsPidObject;
			} else {
				containerPID = new PID(collectionURI);
			}
			
			if (PackagingType.METS_CDR.equals(deposit.getPackaging())){
				return doMETSCDRDeposit(containerPID, deposit, auth, configImpl, agent);
			}
		} catch (Exception e) {
			LOG.error("Exception while attempting to deposit", e);
			System.out.println("Exception while attempting to deposit ");
			e.printStackTrace();
		}
		return null;
	}
	
	private DepositReceipt doMETSCDRDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent) throws Exception {
		
		LOG.debug("Preparing to perform a CDR METS deposit");
		
		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");
		
		this.depositInputStreamToFile(deposit);
		METSPackageSIP sip = new METSPackageSIP(containerPID, deposit.getFile(), agent, isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

		digitalObjectManager.addBatch(sip, agent, "Added through SWORD");

		DepositReceipt receipt = new DepositReceipt();
		IRI editMediaIRI = new IRI(config.getBasePath() + SwordConfigurationImpl.COLLECTION_PATH);

		receipt.addEditMediaIRI(editMediaIRI);
		receipt.setSwordEditIRI(editMediaIRI);
		receipt.setEditMediaIRI(editMediaIRI);

		LOG.info("Returning receipt " + receipt);
		return receipt;
	}
	
	private DepositReceipt doMETSDSpaceDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent) throws Exception {
		
		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");
		
		this.depositInputStreamToFile(deposit);
		
		DepositReceipt receipt = new DepositReceipt();
		return receipt;
	}
	
	private void depositInputStreamToFile(Deposit deposit) throws Exception {
		File depositFile = File.createTempFile("input-", ".zip");
		OutputStream os = new BufferedOutputStream(new FileOutputStream(depositFile));
		try {
			byte[] buf = new byte[4096];
			int len;
			while ((len = deposit.getInputStream().read(buf)) != -1) {
				os.write(buf, 0, len);
			}
			deposit.setFile(depositFile);
		} finally {
			deposit.getInputStream().close();
			os.flush();
			os.close();
		}
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public AgentFactory getAgentFactory() {
		return agentFactory;
	}

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
}
