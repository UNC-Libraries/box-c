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

import org.apache.abdera.Abdera;
import org.apache.abdera.writer.Writer;
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
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.AtomPubEntrySIP;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.ContentModelHelper;
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
	private DepositReportingUtil depositReportingUtil;

	@Override
	public DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth,
			SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException {

		log.debug("Preparing to do collection deposit to " + collectionURI);
		if (collectionURI == null)
			throw new SwordServerException("No collection URI was provided");

		Agent depositor = agentFactory.findPersonByOnyen(auth.getUsername(), false);
		Agent owner = null;
		if (auth.getOnBehalfOf() != null) {
			owner = agentFactory.findPersonByOnyen(auth.getOnBehalfOf(), false);
		} else {
			owner = depositor;
		}

		SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

		PID containerPID = extractPID(collectionURI, SwordConfigurationImpl.COLLECTION_PATH + "/");

		if (!hasAccess(auth, containerPID, ContentModelHelper.Permission.addRemoveContents, configImpl)) {
			throw new SwordAuthException("Insufficient privileges to deposit to container " + containerPID.getPid());
		}

		if (deposit.getPackaging() == null || deposit.getPackaging().trim().length() == 0) {
			// Ingest of non-packaged objects
			if (deposit.getSwordEntry() == null) {
				// Single file, no metadata ingest, which isn't supported at the moment
				throw new SwordError("Could not ingest, no metadata was provided.");
			} else {
				try {
					log.debug("Performing Atom Pub Entry deposit to " + containerPID.getPid());
					return doAtomPubEntryDeposit(containerPID, deposit, auth, configImpl, depositor, owner);
				} catch (JDOMException e) {
					log.warn("Failed to deposit", e);
					throw new SwordError("A problem occurred while attempting to perform your deposit: " + e.getMessage());
				} catch (Exception e) {
					throw new SwordServerException(
							"Unexpected exception while attempting to perform an Atom Pub entry deposit", e);
				}
			}
		} else {
			if (deposit.getFile() == null) {
				// Invalid to have a package but no file
				throw new SwordError("Could not ingest, a package type was provided but no content.");
			} else {
				PackagingType recognizedType = null;
				for (PackagingType t : PackagingType.values()) {
					if (t.equals(deposit.getPackaging())) {
						recognizedType = t;
						break;
					}
				}

				if (recognizedType != null) {
					try {
						// METS subclasses are the only supported packaging types at the moment
						return doMETSDeposit(containerPID, deposit, auth, configImpl, depositor, recognizedType, owner);
					} catch (FilesDoNotMatchManifestException e) {
						log.warn("Files in the package " + deposit.getFilename()
								+ " did not match the provided METS manifest of package type " + deposit.getPackaging(), e);
						throw new SwordError("Files in the package " + deposit.getFilename()
								+ " did not match the provided METS manifest.", e);
					} catch (IngestException e) {
						log.warn("Files in the package " + deposit.getFilename()
								+ " did not match the provided METS manifest.", e);
						throw new SwordError("An exception occurred while attempting to ingest package "
								+ deposit.getFilename() + " of type " + deposit.getPackaging(), e);
					} catch (Exception e) {
						throw new SwordServerException(e);
					}
				}
			}
		}
		return null;
	}

	private DepositReceipt doAtomPubEntryDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent, Agent owner) throws Exception {

		log.debug("Preparing to perform an Atom Pub entry metadata only deposit to " + containerPID.getPid());

		if (deposit.getSwordEntry() == null || deposit.getSwordEntry().getEntry() == null)
			throw new SwordError("No AtomPub entry was included in the submission");

		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, deposit.getSwordEntry().getEntry());
		if (log.isDebugEnabled()) {
			Abdera abdera = new Abdera();
			Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
			writer.writeTo(deposit.getSwordEntry().getEntry(), System.out);
		}
		
		if (deposit.getFile() == null) {
			sip = new AtomPubEntrySIP(containerPID, deposit.getSwordEntry().getEntry());
		} else {
			sip = new AtomPubEntrySIP(containerPID, deposit.getSwordEntry().getEntry(), deposit.getFile(),
					deposit.getMimeType(), deposit.getFilename(), deposit.getMd5());
		}
		sip.setInProgress(deposit.isInProgress());
		sip.setSuggestedSlug(deposit.getSlug());

		DepositRecord record = new DepositRecord(agent, owner, DepositMethod.SWORD13);
		record.setMessage("Added through SWORD");
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);

		return buildReceipt(ingestResult, config);
	}

	private DepositReceipt doMETSDeposit(PID containerPID, Deposit deposit, AuthCredentials auth,
			SwordConfigurationImpl config, Agent agent, PackagingType type, Agent owner) throws Exception {

		log.debug("Preparing to perform a CDR METS deposit to " + containerPID.getPid());

		String name = deposit.getFilename();
		boolean isZip = name.endsWith(".zip");

		if (log.isDebugEnabled()) {
			log.debug("Working with temporary file: " + deposit.getFile().getAbsolutePath());
		}

		METSPackageSIP sip = new METSPackageSIP(containerPID, deposit.getFile(), isZip);
		// PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

		DepositRecord record = new DepositRecord(agent, owner, DepositMethod.SWORD13);
		record.setMessage("Added through SWORD");
		record.setPackagingType(type);
		IngestResult ingestResult = digitalObjectManager.addToIngestQueue(sip, record);

		return buildReceipt(ingestResult, config);
	}

	private DepositReceipt buildReceipt(IngestResult ingestResult, SwordConfigurationImpl config)
			throws SwordServerException {
		if (ingestResult == null || ingestResult.derivedPIDs == null || ingestResult.derivedPIDs.size() == 0) {
			throw new SwordServerException("Add batch request " + ingestResult.originalDepositID.getPid()
					+ " did not return any derived results.");
		}

		DepositReceipt receipt = depositReportingUtil.retrieveDepositReceipt(ingestResult, config);
		return receipt;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setDepositReportingUtil(DepositReportingUtil depositReportingUtil) {
		this.depositReportingUtil = depositReportingUtil;
	}
}
