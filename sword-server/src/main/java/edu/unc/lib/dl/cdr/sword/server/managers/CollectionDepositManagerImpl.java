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

import java.util.List;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.AccessControlRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.AtomPubEntrySIP;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PackagingType;

/**
 *
 * @author bbpennel
 *
 */
public class CollectionDepositManagerImpl extends AbstractFedoraManager implements CollectionDepositManager {
	private static Logger log = Logger.getLogger(CollectionDepositManagerImpl.class);

	private DigitalObjectManager digitalObjectManager;

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {
		
		log.debug("Preparing to do collection deposit to " + collectionURI);
		if (collectionURI == null)
			throw new SwordServerException("No collection URI was provided");

		Agent depositor = agentFactory.findPersonByOnyen(auth.getUsername(), false);
		if (depositor == null){
			throw new SwordAuthException("Unable to find a user matching the submitted username credentials, " + auth.getUsername());
		}
		Agent owner = null;
		if (auth.getOnBehalfOf() != null){
			owner = agentFactory.findPersonByOnyen(auth.getOnBehalfOf(), false);
			if (owner == null){
				throw new SwordAuthException("Unable to find a user matching OnBehalfOf, " + auth.getOnBehalfOf());
			}
		} else {
			owner = depositor;
		}
		
		SwordConfigurationImpl configImpl = (SwordConfigurationImpl)config;

		PID containerPID = extractPID(collectionURI, SwordConfigurationImpl.COLLECTION_PATH + "/");
		
		//Get the users group
		List<String> groupList = this.getGroups(auth, configImpl);
		
		if (!accessControlUtils.hasAccess(containerPID, groupList, AccessControlRole.curator.getUri().toString())){
			throw new SwordAuthException("Insufficient privileges to deposit to container " + containerPID.getPid());
		}
		
		
		if (deposit.getFile() == null){
			if (deposit.getSwordEntry() != null) {
				try {
					log.debug("Performing metadata only deposit to " + containerPID.getPid());
					return doMetadataOnlyDeposit(containerPID, deposit, auth, configImpl, depositor, owner);
				} catch (JDOMException e){
					log.warn("Failed to deposit", e);
					throw new SwordError("A problem occurred while attempting to perform your deposit: " + e.getMessage());
				} catch (Exception e) {
					throw new SwordServerException("Unexpected exception while attempting to perform a metadata only deposit", e);
				}
			}
		} else {
			PackagingType recognizedType = null;
			for(PackagingType t : PackagingType.values()) {
				if(t.equals(deposit.getPackaging())) {
					recognizedType = t;
					break;
				}
			}

		if (recognizedType != null){
			try {
				return doMETSDeposit(containerPID, deposit, auth, configImpl, agent, recognizedType);
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
	
	private DepositReceipt doMetadataOnlyDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent, Agent owner) throws Exception {
		
		log.debug("Preparing to perform an Atom Pub entry metadata only deposit to " + containerPID.getPid());
		
		if (deposit.getSwordEntry() == null || deposit.getSwordEntry().getEntry() == null)
			throw new SwordError("No AtomPub entry was included in the submission");
		
		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, deposit.getSwordEntry().getEntry());
		sip.setInProgress(deposit.isInProgress());
		sip.setSuggestedSlug(deposit.getSlug());
		
		DepositRecord record = new DepositRecord(agent, owner, DepositMethod.SWORD13);
		record.setMessage("Added through SWORD");
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);
		
		return buildReceipt(ingestResult, config);
	}

	private DepositReceipt doMETSDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent, PackagingType type) throws Exception {

		log.debug("Preparing to perform a CDR METS deposit to " + containerPID.getPid());

		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");

		if (log.isDebugEnabled()){
			log.debug("Working with temporary file: " + deposit.getFile().getAbsolutePath());
		}
		
		METSPackageSIP sip = new METSPackageSIP(containerPID, deposit.getFile(), depositor, owner, isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();
		
		DepositRecord record = new DepositRecord(agent, DepositMethod.SWORD13);
		record.setMessage("Added through SWORD");
		record.setPackagingType(type);
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);

		return buildReceipt(ingestResult, config);
	}
	
	private DepositReceipt buildReceipt(IngestResult ingestResult, SwordConfigurationImpl config) throws SwordServerException{
		DepositReceipt receipt = new DepositReceipt();
		//receipt.setOriginalDeposit("", deposit.getMimeType());
		
		if (ingestResult == null || ingestResult.derivedPIDs == null || ingestResult.derivedPIDs.size() == 0){
			throw new SwordServerException("Add batch request " + ingestResult.originalDepositID.getPid() + " did not return any derived results.");
		}

		PID representativePID = null;

		for (PID resultPID: ingestResult.derivedPIDs){
			if (representativePID == null){
				representativePID = resultPID;
			}
			receipt.addEditMediaIRI(new IRI(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + resultPID.getPid()));
		}

		IRI editIRI = new IRI(config.getSwordPath() + SwordConfigurationImpl.EDIT_PATH + "/" + representativePID.getPid());
		IRI swordEditIRI = new IRI(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/" + representativePID.getPid() + ".atom");

		receipt.setEditIRI(editIRI);
		receipt.setSwordEditIRI(swordEditIRI);

		receipt.setSplashUri(config.getBasePath() + "record?id=" + representativePID.getPid());

		receipt.setTreatment("Added to CDR through SWORD");
		
		log.info("Returning receipt " + receipt);
		return receipt;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}
}
