package edu.unc.lib.deposit.validate;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PremisEventBuilder;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Asserts that all MODS in the package complies with the XSD and controlled vocabularies,
 * as per the configured schematron validator.
 * @author count0
 *
 */
public class ValidateMODS extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(ValidateMODS.class);

	private SchematronValidator schematronValidator = null;
	private Schema modsSchema = null;

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	public Schema getModsSchema() {
		return modsSchema;
	}

	public void setModsSchema(Schema modsSchema) {
		this.modsSchema = modsSchema;
	}

	public ValidateMODS() {
		super();
	}

	public ValidateMODS(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
		Resource premisNormalizationEvent;
		String message;
		String detailNote;
		int count = 0;
		int invalidXSD = 0;
		int invalidVocab = 0;
		if(!getDescriptionDir().exists()) {
			log.debug("MODS directory does not exist");
			return;
		}
		File[] modsFiles = getDescriptionDir().listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return (f.isFile() && f.getName().endsWith(".xml"));
			}
		});
		setTotalClicks(modsFiles.length);
		for (File f : modsFiles) {
			count++;
			PID p = DepositConstants.getPIDForTagFile(f.getPath());
			String xsdMessage = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
			
			PremisLogger premisLogger = getPremisLogger(p);
			PremisEventBuilder premisValidationBuilder = premisLogger.buildEvent(Premis.Validation);
			Resource premisEvent = premisValidationBuilder
					.addEventDetail(xsdMessage)
					.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
					.create();
			premisLogger.writeEvent(premisEvent); 

			try {
				// XSD validation
				getModsSchema().newValidator().validate(new StreamSource(f));
				premisNormalizationEvent = premisValidationBuilder
						.addEventDetail("MODS is valid with respect to the schema (XSD)")
						.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
						.create();
				premisLogger.writeEvent(premisNormalizationEvent);
			} catch (SAXException e) {
				invalidXSD++;
				
				premisNormalizationEvent = premisValidationBuilder
						.addEventDetail("MODS is not valid with respect to the schema (XSD)")
						.addEventDetailOutcomeNote(e.getMessage())
						.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
						.create();
				premisLogger.writeEvent(premisNormalizationEvent);

				continue;
			} catch (IOException unexpected) {
				throw new Error(unexpected);
			}

			// Schematron validation
			message = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
			premisEvent = premisValidationBuilder 
					.addEventDetail(message)
					.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
					.create();
			premisLogger.writeEvent(premisEvent);

			Document svrl = this.getSchematronValidator().validate(
					new StreamSource(f), "vocabularies-mods");
			if (!this.getSchematronValidator().hasFailedAssertions(svrl)) {
				message = "MODS is valid with respect to local conventions (Schematron rules)";
				detailNote = "The supplied MODS metadata meets CDR vocabulary requirements.";
			} else {
				message = "MODS is not valid with respect to local conventions (Schematron rules).";
				detailNote = "The supplied MODS metadata does not meet CDR vocabulary requirements.";
				invalidVocab++;
			}
			premisEvent = premisValidationBuilder 
					.addEventDetail(message)
					.addEventDetailOutcomeNote(detailNote)
					.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
					.create();
			
			premisLogger.writeEvent(premisEvent);
			addClicks(1);
		}

		if ((invalidVocab + invalidXSD) > 0) {
			message = MessageFormat.format("{0} invalid against XSD; {1} invalid against vocabularies", invalidXSD, invalidVocab);
			failJob("Some descriptive metadata (MODS) did not meet requirements.", message);
		} else {
			PID depositPID = getDepositPID();
			PremisLogger premisDepositLogger = getPremisLogger(depositPID);
			PremisEventBuilder premisDepositEventBuilder = premisDepositLogger.buildEvent(Premis.Validation);
			Resource premisEvent = premisDepositEventBuilder
					.addEventDetail("{0} MODS records validated", count)
					.addSoftwareAgent(SoftwareAgent.depositService.getFullname())
					.create();
			
			premisDepositLogger.writeEvent(premisEvent);

		}
	}

}
