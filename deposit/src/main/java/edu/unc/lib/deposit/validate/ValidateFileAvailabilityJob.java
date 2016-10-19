package edu.unc.lib.deposit.validate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;

public class ValidateFileAvailabilityJob extends AbstractDepositJob {

	private Stages stages;
	
	public Stages getStages() {
		return stages;
	}

	public void setStages(Stages stages) {
		this.stages = stages;
	}

	public ValidateFileAvailabilityJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		Map<PID, String> hrefs = new HashMap<PID, String>();
		Set<String> failures = new HashSet<String>();

		Model model = getReadOnlyModel();
		Property fileLocation = CdrDeposit.stagingLocation;
		StmtIterator i = model.listStatements(new SimpleSelector((Resource) null, fileLocation, (RDFNode) null));
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			PID p = new PID(s.getSubject().getURI());
			String href = s.getObject().asLiteral().getString();
			hrefs.put(p, href);
		}

		setTotalClicks(hrefs.size());

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
			
			if (storageURI.getScheme() == null || storageURI.getScheme().contains("file")) {
				if (!storageURI.isAbsolute()) {
					storageURI = getDepositDirectory().toURI().resolve(storageURI);
				}
				
				File file = new File(storageURI.getPath());
				
				if (!file.exists()) {
					failures.add(storageURI.toString());
				}
			}
			
			addClicks(1);
		}
		
		if (failures.size() > 0) {
			StringBuilder sb = new StringBuilder("Some of the files referenced by the deposit could not be found:\n");
			for (String uri : failures) {
				sb.append(uri).append(" - ").append(uri).append("\n");
			}
			
			if (failures.size() == 1) {
				failJob("1 file referenced by the deposit could not be found.", sb.toString());
			} else {
				failJob(failures.size() + " files referenced by the deposit could not be found.", sb.toString());
			}
		}
	}

}
