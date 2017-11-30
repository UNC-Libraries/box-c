package edu.unc.lib.dl.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Asserts that all MODS in the package complies with the XSD and controlled vocabularies,
 * as per the configured schematron validator.
 * @author harring
 *
 */
public class MODSValidator {
    private static final Logger log = LoggerFactory.getLogger(MODSValidator.class);

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

    public void validateMODS(PID objectPid) {
        Resource premisNormalizationEvent;
        String message;
        String detailNote;
        int count = 0;
        int invalidXSD = 0;
        int invalidVocab = 0;

            count++;
            String xsdMessage = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";

            PremisLogger premisLogger = getPremisLogger(objectPid);
            PremisEventBuilder premisValidationBuilder = premisLogger.buildEvent(Premis.Validation);
            Resource premisEvent = premisValidationBuilder
                    .addEventDetail(xsdMessage)
                    .create();
            premisLogger.writeEvent(premisEvent);

            try {
                // XSD validation
                getModsSchema().newValidator().validate(new StreamSource(f));
                premisNormalizationEvent = premisValidationBuilder
                        .addEventDetail("MODS is valid with respect to the schema (XSD)")
                        .create();
                premisLogger.writeEvent(premisNormalizationEvent);
            } catch (SAXException e) {
                invalidXSD++;

                premisNormalizationEvent = premisValidationBuilder
                        .addEventDetail("MODS is not valid with respect to the schema (XSD)")
                        .addEventDetailOutcomeNote(e.getMessage())
                        .create();
                premisLogger.writeEvent(premisNormalizationEvent);
            }

            // Schematron validation
            message = "Validation of Controlled Vocabularies in Descriptive Metadata (MODS)";
            premisEvent = premisValidationBuilder
                    .addEventDetail(message)
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
                    .create();

            premisLogger.writeEvent(premisEvent);
        }

        if ((invalidVocab + invalidXSD) > 0) {
            message = MessageFormat.format("{0} invalid against XSD; {1} invalid against vocabularies",
                    invalidXSD, invalidVocab);
            //throw new MODSValidationException("Some descriptive metadata (MODS) did not meet requirements.", message);
        } else {
            PremisLogger premisLogger = getPremisLogger(objectPid);
            PremisEventBuilder premisEventBuilder = premisLogger.buildEvent(Premis.Validation);
            Resource premisEvent = premisEventBuilder
                    .addEventDetail("{0} MODS records validated", count)
                    .create();

            premisLogger.writeEvent(premisEvent);

        }
    }

    private PID getPIDFromFile(File file) {
        String path = file.getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1,
                path.lastIndexOf('.'));
        return PIDs.get(uuid);
    }

    private PremisLogger getPremisLogger() {

}

