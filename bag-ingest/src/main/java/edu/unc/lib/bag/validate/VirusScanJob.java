package edu.unc.lib.bag.validate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.philvarner.clamavj.ClamScan;
import com.philvarner.clamavj.ScanResult;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;
import edu.unc.lib.workers.AbstractBagJob;

public class VirusScanJob extends AbstractBagJob {
	private static final Logger log = LoggerFactory
			.getLogger(VirusScanJob.class);
	private ClamScan clamScan = null;
	private Stages stages = null;

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

	public VirusScanJob(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	@Override
	public void run() {
		log.debug("Running virus checks on : {}", getBagDirectory());
		
		// get ClamScan software and database versions
		String version = this.clamScan.cmd("nVERSION\n".getBytes()).trim();

		Map<PID, String> hrefs = new HashMap<PID, String>();

		Map<String, String> failures = new HashMap<String, String>();

		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(getBagDirectory(), BagConstants.MODEL_FILE);
		model.read(modelFile.toURI().toString());
		Property sourceData = model
				.createProperty(ContentModelHelper.CDRProperty.sourceData
						.getURI().toString());
		Property hasStagedLocation = model
				.createProperty(ContentModelHelper.CDRProperty.hasStagingLocation
						.getURI().toString());
		StmtIterator i = model.listStatements(new SimpleSelector((Resource)null, sourceData, (RDFNode)null));
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			PID p = new PID(s.getSubject().getURI());
			NodeIterator ni = model.listObjectsOfProperty(s.getObject().asResource(), hasStagedLocation);
			if(ni.hasNext()) {
				String href = ni.nextNode().asLiteral().getString();
				hrefs.put(p, href);
			}
		}

		for (Entry<PID, String> href : hrefs.entrySet()) {
			URI manifestURI;
			URI storageURI = null;
			try {
				manifestURI = new URI(href.getValue());
				storageURI = getStages().getStorageURI(manifestURI);
			} catch (URISyntaxException e) {
				failJob(e, Type.VIRUS_CHECK, "Unable to parse manifest URI: {0}", href.getValue());
			} catch (StagingException e) {
				failJob(e, Type.VIRUS_CHECK, "Unable to resolve staging location for file: {0}", href.getValue());
			}
			if (storageURI.getScheme() == null
					|| storageURI.getScheme().contains("file")) {
				File file = new File(storageURI.getPath());
				ScanResult result = this.clamScan.scan(file);
				switch (result.getStatus()) {
				case FAILED:
					Element ev = getEventLog().logEvent(
							PremisEventLogger.Type.VIRUS_CHECK,
							"File failed pre-ingest scan for viruses: "+storageURI.getPath(), href.getKey(),
							ContentModelHelper.Datastream.DATA_FILE.getName());
					PremisEventLogger.addSoftwareAgent(ev, "ClamAV", version);
					PremisEventLogger.addDetailedOutcome(ev, "failure",
							"found virus signature " + result.getSignature(),
							null);
					failures.put(storageURI.toString(), result.getSignature());
					break;
				case ERROR:
					throw new Error(
							"Virus checks are producing errors: "
									+ result.getException()
											.getLocalizedMessage());
				case PASSED:
					Element ev2 = getEventLog().logEvent(
							PremisEventLogger.Type.VIRUS_CHECK,
							"File passed pre-ingest scan for viruses.", href.getKey(),
							ContentModelHelper.Datastream.DATA_FILE.getName());
					PremisEventLogger.addSoftwareAgent(ev2, "ClamAV", version);
					PremisEventLogger.addDetailedOutcome(ev2, "success", null,
							null);
					break;
				}
			}
		}
		if(failures.size() > 0) {
			StringBuilder sb = new StringBuilder("Virus checks failed for some files:\n");
			for(String uri : failures.keySet()) {
				sb.append(uri).append(" - ").append(failures.get(uri)).append("\n");
			}
			failJob(Type.VIRUS_CHECK, failures.size()+ " virus check(s) failed", sb.toString());
		}
	}

}
