package edu.unc.lib.bag.normalize;

import static edu.unc.lib.dl.util.DepositBagInfoTxt.PACKAGING_TYPE;

import java.io.File;

import org.jdom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.METSProfile;

public class CDRMETS2N3BagJob extends AbstractMETS2N3BagJob {
	public CDRMETS2N3BagJob() {
		super();
	}

	public CDRMETS2N3BagJob(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}
	
	@Override
	public void run() {
		gov.loc.repository.bagit.Bag bag = loadBag();
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
		saveModel(model, BagConstants.MODEL_FILE);
		bag.addFileAsTag(new File(getBagDirectory(), BagConstants.MODEL_FILE));
		
		final File modsFolder = getDescriptionDir();
		modsFolder.mkdir();
		extractor.saveDescriptions(new FilePathFunction() {
			@Override
			public String getPath(String piduri) {
				String uuid = new PID(piduri).getUUID();
				return new File(modsFolder, uuid+".xml").getAbsolutePath();
			}
		});
		bag.addFileAsTag(modsFolder);
		
		bag.getBagInfoTxt().putList(PACKAGING_TYPE, PackagingType.BAG_WITH_N3.getUri());
		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}", PackagingType.METS_CDR.getUri(), PackagingType.BAG_WITH_N3.getUri());
		
		saveBag(bag);
		enqueueDefaultNextJob();
	}

}
