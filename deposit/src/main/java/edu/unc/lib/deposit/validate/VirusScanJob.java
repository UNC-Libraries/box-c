package edu.unc.lib.deposit.validate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.philvarner.clamavj.ClamScan;
import com.philvarner.clamavj.ScanResult;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventBuilder;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;

/**
 * Scans all files mentioned in N3 manifest for viruses. If a Staging configuration
 * is supplied then staged files will be resolved. Otherwise files are resolved
 * with the bag directory as the base URI.
 * @author count0
 *
 */
public class VirusScanJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory
			.getLogger(VirusScanJob.class);
	private ClamScan clamScan;
	private Stages stages;

	public VirusScanJob() {
		super();
	}

	public ClamScan getClamScan() {
		return clamScan;
	}

	public void setClamScan(ClamScan clamScan) {
		this.clamScan = clamScan;
	}

	public Stages getStages() {
		return stages;
	}

	public void setStages(Stages stages) {
		this.stages = stages;
	}

	public VirusScanJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		log.debug("Running virus checks on : {}", getDepositDirectory());

		// get ClamScan software and database versions
		String version = this.clamScan.cmd("nVERSION\n".getBytes()).trim();
		String softwareAgent = "ClamAV ("+version+")";

		Map<PID, String> hrefs = new HashMap<PID, String>();

		Map<String, String> failures = new HashMap<String, String>();

		Resource premisEvent = null;
		Model model = getReadOnlyModel();
		Property fileLocation = model
				.createProperty(ContentModelHelper.DepositRelationship.stagingLocation.toString());
		StmtIterator i = model.listStatements(new SimpleSelector((Resource)null, fileLocation, (RDFNode)null));
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			PID p = new PID(s.getSubject().getURI());
			String href = s.getObject().asLiteral().getString();
			hrefs.put(p, href);
		}

		setTotalClicks(hrefs.size());
		int scannedObjects = 0;

		for (Entry<PID, String> href : hrefs.entrySet()) {
			verifyRunning();

			URI manifestURI;
			URI storageURI = null;
			try {
				manifestURI = new URI(href.getValue());
				if (manifestURI.getScheme() == null) {
					storageURI = manifestURI;
				} else {
					storageURI = getStages().getStorageURI(manifestURI);
				}
			} catch (URISyntaxException e) {
				failJob(e, "Unable to parse manifest URI: {0}", href.getValue());
			} catch (StagingException e) {
				failJob(e, "Unable to resolve staging location for file: {0}", href.getValue());
			}
			
			PremisLogger premisLogger = getPremisLogger(href.getKey());
			PremisEventBuilder premisEventBuilder = premisLogger.buildEvent(Premis.VirusCheck);
			
			if (storageURI.getScheme() == null
					|| storageURI.getScheme().contains("file")) {
				if(!storageURI.isAbsolute()) {
					storageURI = getDepositDirectory().toURI().resolve(storageURI);
				}
				File file = new File(storageURI.getPath());
				ScanResult result = this.clamScan.scan(file);
				
				switch (result.getStatus()) {
				case FAILED:
					premisEvent = premisEventBuilder.addSoftwareAgent(softwareAgent)
						.addEventDetail("found virus signature " + result.getSignature())
						.create();
				
					failures.put(storageURI.toString(), result.getSignature());
					break;
				case ERROR:
					throw new Error(
							"Virus checks are producing errors: "
									+ result.getException()
											.getLocalizedMessage());
				case PASSED:
					premisEvent = premisEventBuilder.addSoftwareAgent(softwareAgent)
						.addEventDetail("File passed pre-ingest scan for viruses")
						.create();
					
					scannedObjects++;
					break;
				}
				
				premisLogger.writeEvent(premisEvent);
			}
			addClicks(1);
		}
		if(failures.size() > 0) {
			StringBuilder sb = new StringBuilder("Virus checks failed for some files:\n");
			for(String uri : failures.keySet()) {
				sb.append(uri).append(" - ").append(failures.get(uri)).append("\n");
			}
			failJob(failures.size() + " virus check(s) failed.", sb.toString());
		} else {
			if (scannedObjects != hrefs.size()) {
				failJob("Virus scan job did not attempt to scan all files.",
						(hrefs.size() - scannedObjects) + " objects were not scanned.");
			}
			
			PID depositPID = getDepositPID();
			PremisLogger premisDepositLogger = getPremisLogger(depositPID);
			PremisEventBuilder premisDepositEventBuilder = premisDepositLogger.buildEvent(Premis.VirusCheck);
			Resource premisDepositEvent = premisDepositEventBuilder.addEventDetail(scannedObjects + "files scanned for viruses.").create();
			premisDepositLogger.writeEvent(premisDepositEvent);
		}
	}
}