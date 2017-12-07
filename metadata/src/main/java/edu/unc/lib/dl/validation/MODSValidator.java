/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.validation;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.SCHEMATRON_VALIDATION_REPORT_NS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.stream.Collectors;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
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

    private SchematronValidator schematronValidator;
    private Schema modsSchema;

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

    /**
     * Validates a MODS description from a File object
     *
     * @param file
     * @throws IOException
     */
    public InputStream validate(File file) throws IOException {
        return validate(new ByteArrayInputStream(Files.readAllBytes(file.toPath())));
    }

    /**
     * Validates a MODS description. NB: this method only supports InputStream types that can be reset
     * for multiple reads. Other types will be converted to a ByteArrayInputStream prior to validation.
     *
     * @param docStream
     * @throws MetadataValidationException
     * @throws IOException
     */
    public InputStream validate(InputStream docStream) throws MetadataValidationException, IOException {
        if (!docStream.markSupported()) {
            docStream = new ByteArrayInputStream(IOUtils.toByteArray(docStream));
        }
        StreamSource streamSrc = new StreamSource(docStream);
        try {
            getModsSchema().newValidator().validate(streamSrc);

            // Reset the inputstream for second read
            docStream.reset();
        } catch (SAXException e) {
            throw new MetadataValidationException("MODS is not valid with respect to the schema (XSD)",
                    e.getMessage(), e);
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

            throw new MetadataValidationException("MODS is not valid with respect to local conventions"
                    + " (Schematron rules)", failedAssertionMessage);
        }

        log.debug("Document passed vocabulary schematron validation");
        return docStream;
    }
}

