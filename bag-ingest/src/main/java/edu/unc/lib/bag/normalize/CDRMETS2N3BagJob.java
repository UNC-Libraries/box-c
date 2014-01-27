package edu.unc.lib.bag.normalize;

import java.io.File;

import org.jdom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;

public class CDRMETS2N3BagJob extends AbstractMETS2N3BagJob {
	public CDRMETS2N3BagJob() {
		super();
	}

	public CDRMETS2N3BagJob(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}
	
	@Override
	public void run() {
		validateMETS();
		validateProfile(METSProfile.CDR_SIMPLE);
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs

		Model model = ModelFactory.createDefaultModel();
		CDRMETSGraphExtractor extractor = new CDRMETSGraphExtractor(mets, this.getDepositPID());
		extractor.addArrangement(model);
		extractor.helper.addFileAssociations(model, false);
		extractor.addAccessControls(model);
		final File modsFolder = new File(getBagDirectory(), "description");
		modsFolder.mkdir();
		extractor.saveDescriptions(new FilePathFunction() {
			@Override
			public String getPath(String piduri) {
				String uuid = new PID(piduri).getUUID();
				return new File(modsFolder, uuid+".xml").getAbsolutePath();
			}
		});
		saveModel(model, "everything.n3");
		// addN3PackagingType();
		recordEvent(Type.NORMALIZATION, "Converted METS {1} to N3 form", METSProfile.CDR_SIMPLE.getName());
	}

}
