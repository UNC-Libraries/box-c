package edu.unc.lib.bag.validate;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.jdom.Document;
import org.jdom.Element;
import org.xml.sax.SAXException;

import edu.unc.lib.bag.AbstractBagJob;
import edu.unc.lib.bag.BagConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import gov.loc.repository.bagit.Bag;

/**
 * Asserts that all MODS in the package complies with the XSD and controlled vocabularies,
 * as per the configured schematron validator.
 * @author count0
 *
 */
public class ValidateMODS extends AbstractBagJob {

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

	public ValidateMODS(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	@Override
	public void run() {
		Bag bag = loadBag();
		int count = 0;
		int invalidXSD = 0;
		int invalidVocab = 0;
		for (File f : getDescriptionDir().listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return (f.isFile() && f.getName().endsWith(".xml"));
			}
		})) {
			count++;
			PID p = BagConstants.getPIDForTagFile(f.getPath());
			String xsdMessage = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
			Element xsdEvent = getEventLog().logEvent(Type.VALIDATION, xsdMessage, p, "MD_DESCRIPTIVE");
			try {
				// XSD validation
				getModsSchema().newValidator().validate(new StreamSource(f));
			} catch (SAXException e) {
				invalidXSD++;
				PremisEventLogger.addDetailedOutcome(
						xsdEvent,
						"MODS is not valid",
						e.getMessage(), null);
				continue;
			} catch (IOException unexpected) {
				throw new Error(unexpected);
			}
			// Schematron validation
			String message = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
			Element event = getEventLog().logEvent(Type.VALIDATION, message, p, "MD_DESCRIPTIVE");
			Document svrl = this.getSchematronValidator().validate(
					new StreamSource(f), "vocabularies-mods");
			if (!this.getSchematronValidator().hasFailedAssertions(svrl)) {
				PremisEventLogger.addDetailedOutcome(
								event,
								"MODS is valid",
								"The supplied MODS metadata meets CDR vocabulary requirements.",
								null);
			} else {
				PremisEventLogger.addDetailedOutcome(
								event,
								"MODS is not valid",
								"The supplied MODS metadata does not meet CDR vocabulary requirements.",
								svrl.detachRootElement());
				invalidVocab++;
			}
		}
		
		if((invalidVocab + invalidXSD) > 0) {
			String message = MessageFormat.format("{0} invalid against XSD; {1} invalid against vocabularies", invalidXSD, invalidVocab);
			failDeposit(Type.VALIDATION, "Some descriptive metadata (MODS) did not meet requirements.", message);
		} else {
			recordDepositEvent(Type.VALIDATION, "{0} MODS records validated", count);
		}

		saveBag(bag);
		enqueueDefaultNextJob();
	}

}
