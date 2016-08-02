package edu.unc.lib.deposit.normalize;

import java.io.File;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventBuilder;
import edu.unc.lib.dl.xml.METSProfile;

public class CDRMETS2N3BagJob extends AbstractMETS2N3BagJob {
	private static final Logger LOG = LoggerFactory.getLogger(CDRMETS2N3BagJob.class);
	public CDRMETS2N3BagJob() {
		super();
	}

	public CDRMETS2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		validateMETS();

		// Store a reference to the manifest file
		addManifestURI();

		LOG.info("METS XML validated");
		validateProfile(METSProfile.CDR_SIMPLE);
		LOG.info("METS Schematron validated");
		Document mets = loadMETS();
		LOG.info("METS dom document loaded");
		assignPIDs(mets); // assign any missing PIDsq
		LOG.info("PIDs assigned");
		saveMETS(mets); // manifest updated to have record of all PIDs
		LOG.info("METS saved with new PIDs");

		Model model = getWritableModel();
		CDRMETSGraphExtractor extractor = new CDRMETSGraphExtractor(mets, this.getDepositPID());
		LOG.info("Extractor initialized");
		extractor.addArrangement(model);
		LOG.info("Extractor arrangement added");
		extractor.helper.addFileAssociations(model, true);
		LOG.info("Extractor file associations added");
		extractor.addAccessControls(model);
		LOG.info("Extractor access controls added");

		final File modsFolder = getDescriptionDir();
		modsFolder.mkdir();
		extractor.saveDescriptions(new FilePathFunction() {
		@Override
			public String getPath(String piduri) {
				String uuid = new PID(piduri).getUUID();
				return new File(modsFolder, uuid+".xml").getAbsolutePath();
			}
		});
		LOG.info("MODS descriptions saved");
		
		PID depositPID = getDepositPID();
		PremisLogger premisDepositLogger = getPremisLogger(depositPID);
		PremisEventBuilder premisDepositEventBuilder = premisDepositLogger.buildEvent(Premis.Normalization);
		Resource premisDepositEvent = premisDepositEventBuilder
				.addEventDetail("Normalized deposit package from {0} to {1}", PackagingType.METS_CDR.getUri(), PackagingType.BAG_WITH_N3.getUri())
				.create();
		premisDepositLogger.writeEvent(premisDepositEvent);
	}

}
