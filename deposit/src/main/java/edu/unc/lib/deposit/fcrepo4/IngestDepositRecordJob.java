/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.fcrepo4;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.Rdfs;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Creates and ingests the deposit record object
 * 
 * @author bbpennel
 *
 */
public class IngestDepositRecordJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(IngestDepositRecordJob.class);

	public IngestDepositRecordJob() {
		super();
	}

	public IngestDepositRecordJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}
	
	@Override
	public void runJob() {
		String depositUri = getDepositPID().getURI();

		log.debug("Creating record for deposit {}", depositUri);

		Model dModel = getReadOnlyModel();
		Map<String, String> status = getDepositStatus();

		Resource deposit = dModel.getResource(getDepositPID().getURI());

		// Create aip model for the deposit record
		Resource aipObjResc = makeDepositRecord(deposit, status);
		Model aipModel = aipObjResc.getModel();

		// Add ingestion event to PREMIS log
		PremisLogger premisDepositLogger = getPremisLogger(getDepositPID());
		premisDepositLogger.buildEvent(Premis.Ingestion)
				.addEventDetail("ingested as PID: {0}. {1}", getDepositPID().getPid(), 
						aipObjResc.getProperty(DcElements.title).getObject().toString())
				.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
				.addAuthorizingAgent(DepositField.depositorName.name())
				.write();

		// Create the deposit record object in Fedora
		DepositRecord depositRecord;
		try {
			depositRecord = repository.createDepositRecord(getDepositPID(), aipModel)
					.addPremisEvents(premisDepositLogger.getEvents());

			// Add manifest files
			List<String> manifestURIs = getDepositStatusFactory().getManifestURIs(getDepositUUID());
			for (String manifestPath : manifestURIs) {
				URI uri = new URI(manifestPath);
				depositRecord.addManifest(new File(uri), "text/plain");
			}

			// TODO add references to deposited objects 
		} catch (IOException | FedoraException | URISyntaxException e) {
			failJob(e, "Failed to ingest deposit record {0}", getDepositPID());
		}
	}

	/**
	 * Generates a model containing the properties for this deposit record
	 * 
	 * @param deposit
	 * @param status
	 * @return
	 */
	private Resource makeDepositRecord(Resource deposit, Map<String, String> status) {
		Model aipModel = ModelFactory.createDefaultModel();

		Resource aipObjResc = aipModel.createResource(deposit.getURI());

		String filename = status.get(DepositField.fileName.name());
		String title = "Deposit record" + (filename == null? "" : " for " + filename);
		aipObjResc.addProperty(DcElements.title, title);

		aipObjResc.addProperty(Rdfs.type, Cdr.DepositRecord);

		String method = status.get(DepositField.depositMethod.name());
		if (method != null) {
			aipObjResc.addProperty(Cdr.depositMethod, method);
		}
		String onBehalfOf = status.get(DepositField.depositorName.name());
		if (onBehalfOf != null) {
			aipObjResc.addProperty(Cdr.depositedOnBehalfOf, onBehalfOf);
		}
		String depositPackageType = status.get(DepositField.packagingType.name());
		if (depositPackageType != null) {
			aipObjResc.addProperty(Cdr.depositPackageType, depositPackageType);
		}
		String depositPackageProfile = status.get(DepositField.packageProfile.name());
		if (depositPackageProfile != null) {
			aipObjResc.addProperty(Cdr.depositPackageProfile, depositPackageProfile);
		}

		return aipObjResc;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
