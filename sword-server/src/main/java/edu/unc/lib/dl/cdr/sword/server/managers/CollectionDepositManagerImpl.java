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

import java.util.ArrayList;
import java.util.List;

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
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
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
		if (collectionURI == null)
			throw new SwordServerException("No collection URI was provided");

		String agentName = null;
		if (auth.getOnBehalfOf() != null){
			agentName = auth.getOnBehalfOf();
		} else {
			agentName = auth.getUsername();
		};
		Agent agent = agentFactory.findPersonByOnyen(agentName, false);
		if (agent == null){
			throw new SwordAuthException("Unable to find a user matching the provided credentials, " + agentName);
		}
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
		
		//Get the users group
		List<String> groupList = new ArrayList<String>();
		groupList.add(configImpl.getDepositorNamespace() + auth.getUsername());
		groupList.add("public");
		
		if (!accessControlUtils.hasAccess(containerPID, groupList, "http://cdr.unc.edu/definitions/roles#curator")){
			throw new SwordAuthException("Insufficient privileges to deposit to container " + containerPID.getPid());
		}

		if (PackagingType.METS_CDR.equals(deposit.getPackaging()) || PackagingType.METS_DSPACE_SIP_2.equals(deposit.getPackaging())
				|| PackagingType.METS_DSPACE_SIP_1.equals(deposit.getPackaging())){
			try {
				return doMETSDeposit(containerPID, deposit, auth, configImpl, agent);
			} catch (FilesDoNotMatchManifestException e){
				LOG.warn("Files in the package " + deposit.getFilename() + " did not match the provided METS manifest of package type " + deposit.getPackaging(), e);
				throw new SwordError("Files in the package " + deposit.getFilename() + " did not match the provided METS manifest.", e);
			} catch (IngestException e){
				LOG.warn("Files in the package " + deposit.getFilename() + " did not match the provided METS manifest.", e);
				throw new SwordError("An exception occurred while attempting to ingest package " + deposit.getFilename() + " of type " + deposit.getPackaging(), e);
			} catch (Exception e) {
				throw new SwordServerException(e);
			}
		}
		return null;
	}

	private DepositReceipt doMETSDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent) throws Exception {

		LOG.debug("Preparing to perform a CDR METS deposit to " + containerPID.getPid());

		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");

		if (LOG.isDebugEnabled()){
			LOG.debug("Working with temporary file: " + deposit.getFile().getAbsolutePath());
		}
		
		METSPackageSIP sip = new METSPackageSIP(containerPID, deposit.getFile(), agent, isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, agent, "Added through SWORD");

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
			receipt.addEditMediaIRI(new IRI(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + resultPID.getPid()));
		}

		IRI editIRI = new IRI(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + representativePID.getPid() + ".atom");

		receipt.setEditIRI(editIRI);
		receipt.setSwordEditIRI(editIRI);

		receipt.setSplashUri(config.getBasePath() + "record?id=" + representativePID.getPid());

		receipt.setTreatment("Added to CDR through SWORD");

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
