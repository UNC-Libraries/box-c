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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

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
import edu.unc.lib.dl.services.IngestResult;
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

		LOG.debug("Preparing to do collection deposit to " + collectionURI);
		LOG.debug("Root pid is: " + collectionsPidObject);
		if (collectionURI == null)
			throw new SwordServerException("No collection URI was provided");
		
		try {
			Agent agent = agentFactory.findPersonByOnyen("bbpennel", true);
			SwordConfigurationImpl configImpl = (SwordConfigurationImpl)config;
			
			String pidString = null;
			String collectionPath = SwordConfigurationImpl.COLLECTION_PATH + "/";
			int pidIndex = collectionURI.indexOf(collectionPath);
			if (pidIndex > -1){
				pidString = collectionURI.substring(pidIndex + collectionPath.length());
			}
			
			LOG.debug("Collection URI pid is " + pidString);

			PID containerPID = null;
			if (pidString.trim().length() == 0){
				containerPID = collectionsPidObject;
			} else {
				containerPID = new PID(pidString);
			}
			
			if (PackagingType.METS_CDR.equals(deposit.getPackaging()) || PackagingType.METS_DSPACE_SIP.equals(deposit.getPackaging())){
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
		
		LOG.debug("Preparing to perform a CDR METS deposit to " + containerPID.getPid());
		
		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");
		
		METSPackageSIP sip = new METSPackageSIP(containerPID, deposit.getFile(), agent, isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

		IngestResult ingestResult = digitalObjectManager.addBatch(sip, agent, "Added through SWORD");
		
		DepositReceipt receipt = new DepositReceipt();
		receipt.setOriginalDeposit("", deposit.getMimeType());
		
		if (ingestResult == null || ingestResult.derivedPIDs == null || ingestResult.derivedPIDs.size() == 0){
			throw new SwordServerException("Add batch request to " + containerPID.getPid() + " did not return any derived results.");
		}
		
		PID representativePID = null;
		
		for (PID resultPID: ingestResult.derivedPIDs){
			if (representativePID == null){
				representativePID = resultPID;
			}
			receipt.addEditMediaIRI(new IRI(config.getBasePath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + resultPID.getPid()));
		}

		IRI editIRI = new IRI(config.getBasePath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + representativePID.getPid() + ".atom");
		
		receipt.setEditIRI(editIRI);
		receipt.setSwordEditIRI(editIRI);

		LOG.info("Returning receipt " + receipt);
		return receipt;
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
