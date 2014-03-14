package edu.unc.lib.deposit.normalize;

import java.io.File;

import org.jdom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;

public class CDRMETS2N3BagJob extends AbstractMETS2N3BagJob implements Runnable {
	public CDRMETS2N3BagJob() {
		super();
	}

	public CDRMETS2N3BagJob(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}
	
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
		saveModel(model, DepositConstants.MODEL_FILE);
		
		final File modsFolder = getDescriptionDir();
		modsFolder.mkdir();
		extractor.saveDescriptions(new FilePathFunction() {
			@Override
			public String getPath(String piduri) {
				String uuid = new PID(piduri).getUUID();
				return new File(modsFolder, uuid+".xml").getAbsolutePath();
			}
		});

		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}", PackagingType.METS_CDR.getUri(), PackagingType.BAG_WITH_N3.getUri());
	}

}
