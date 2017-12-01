package edu.unc.lib.dl.validation;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.SCHEMATRON_VALIDATION_REPORT_NS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.stream.Collectors;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.schematron.SchematronValidator;

/**
 * Asserts that MODS complies with the XSD and controlled vocabularies,
 * as per the configured schematron validator.
 *
 * @author bbpennel
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

    public void validate(Document doc) throws MetadataValidationException, IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        InputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
        validate(inStream);
    }

    public void validate(File file) throws IOException {
        validate(new ByteArrayInputStream(Files.readAllBytes(file.toPath())));
    }

    private void validate(InputStream docStream) throws MetadataValidationException, IOException {
        StreamSource streamSrc = new StreamSource(docStream);
        try {
            getModsSchema().newValidator().validate(streamSrc);

            // Reset the inputstream for second read
            docStream.reset();
        } catch (SAXException e) {
            throw new MetadataValidationException("MODS is not valid with respect to the schema (XSD)", e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document stream", e);
        }
        log.debug("Document passed MODS schema validation");


        Document svrl = this.getSchematronValidator().validate(
                streamSrc, "vocabularies-mods");

        if (this.getSchematronValidator().hasFailedAssertions(svrl)) {
            String failedAssertionMessage = svrl.getRootElement()
                    .getChildren("failed-assert", SCHEMATRON_VALIDATION_REPORT_NS).stream()
                    .map(assertion -> assertion.getChildText("text", SCHEMATRON_VALIDATION_REPORT_NS))
                    .collect(Collectors.joining("\n"));

            throw new MetadataValidationException("MODS is not valid with respect to local conventions (Schematron rules)",
                    failedAssertionMessage);
        }

        log.debug("Document passed vocabulary schematron validation");
    }
}

