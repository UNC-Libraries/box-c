package edu.unc.lib.deposit.fcrepo4;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.Rdfs;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

public class IngestDepositRecordJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(IngestDepositRecordJob.class);

	private Repository repository;
	
	@Override
	public void runJob() {
		String depositUri = getDepositPID().getURI();
		
		log.debug("Creating record for deposit {}", depositUri);
		getSubdir(DepositConstants.AIPS_DIR).mkdir();
		
		Model dModel = getReadOnlyModel();
		Map<String, String> status = getDepositStatus();
		
		Resource deposit = dModel.getResource(getDepositPID().getURI());
		
		// Create aip model for the deposit record
		Resource aipObjResc = makeDepositRecord(deposit, status);
		Model aipModel = aipObjResc.getModel();
		serializeObjectModel(getDepositPID(), aipModel);
		
		// Ingest the deposit record AIP
		DepositRecord depositRecord = repository.createDepositRecord(getDepositPID().getUUID(), aipModel);

		// Add manifest files
		StmtIterator manifestIt = deposit.listProperties(CdrDeposit.hasManifest);
		while (manifestIt.hasNext()) {
			String manifestPath = manifestIt.next().getString();
			depositRecord.addManifest(new File(getDepositDirectory(), manifestPath));
		}
		
		// Add ingestion event to PREMIS log
		PremisLogger premisDepositLogger = getPremisLogger(getDepositPID());
		Resource premisDepositEvent = premisDepositLogger.buildEvent(Premis.Ingestion)
					.addEventDetail("ingested as PID: {0}. {1}", getDepositPID().getPid(), 
							aipObjResc.getProperty(DcElements.title).getObject().toString())
					.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
					.addAuthorizingAgent(DepositField.depositorName.name())
					.create();
		premisDepositLogger.writeEvent(premisDepositEvent);
	}
	
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
