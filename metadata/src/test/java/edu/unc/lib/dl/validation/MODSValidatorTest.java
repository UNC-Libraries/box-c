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

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import edu.unc.lib.dl.schematron.SchematronValidator;

/**
 *
 * @author bbpennel
 *
 */
public class MODSValidatorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MODSValidator validator;
    private Schema schema;

    private SchematronValidator schematronValidator;

    private Document doc;

    @Before
    public void init() throws Exception {
        Map<String, Resource> schemas = new HashMap<>();
        schemas.put("object-mods", new ClassPathResource("edu/unc/lib/dl/schematron/object-mods.sch"));
        schemas.put("vocabularies-mods", new ClassPathResource("edu/unc/lib/dl/schematron/vocabularies-mods.sch"));

        schematronValidator = new SchematronValidator();
        schematronValidator.setSchemas(schemas);
        schematronValidator.loadSchemas();

        StreamSource[] xsdSources = {
                new StreamSource(getClass().getResourceAsStream("/schemas/xml.xsd")),
                new StreamSource(getClass().getResourceAsStream("/schemas/xlink.xsd")),
                new StreamSource(getClass().getResourceAsStream("/schemas/mods-3-6.xsd"))
        };

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = factory.newSchema(xsdSources);

        validator = new MODSValidator();
        validator.setSchematronValidator(schematronValidator);
        validator.setModsSchema(schema);

        doc = new Document();
    }

    @Test
    public void testNonModsDocument() throws Exception {
        thrown.expect(MetadataValidationException.class);
        thrown.expectMessage("schema");

        doc.addContent(new Element("root"));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testValidateSimple() throws Exception {
        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("Value"))));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testSchemaFailure() throws Exception {
        thrown.expect(MetadataValidationException.class);
        thrown.expectMessage("schema");

        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("invalid", MODS_V3_NS)));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testInvalidLanguageCode() throws Exception {
        thrown.expect(MetadataValidationException.class);
        thrown.expectMessage("local conventions");

        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("language", MODS_V3_NS)
                        .addContent(new Element("languageTerm", MODS_V3_NS)
                                .setText("java")
                                .setAttribute("authority", "iso639-2b")
                                .setAttribute("type", "code"))));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testUnacceptableLanguageAuthority() throws Exception {
        thrown.expect(MetadataValidationException.class);
        thrown.expectMessage("local conventions");

        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("language", MODS_V3_NS)
                        .addContent(new Element("languageTerm", MODS_V3_NS)
                                .setText("eng")
                                .setAttribute("authority", "rfc3066")
                                .setAttribute("type", "code"))));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testValidLanguage() throws Exception {
        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("language", MODS_V3_NS)
                        .addContent(new Element("languageTerm", MODS_V3_NS)
                                .setText("eng")
                                .setAttribute("authority", "iso639-2b")
                                .setAttribute("type", "code"))));

        validator.validate(convertDocumentToStream(doc));
    }

    @Test
    public void testFromFile() throws Exception {
        doc.addContent(new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText("Value"))));

        File docFile = Files.createTempFile("mods", ".xml").toFile();
        new XMLOutputter().output(doc, new FileOutputStream(docFile));

        validator.validate(docFile);
    }

    @Test(expected = IOException.class)
    public void testMissingFile() throws Exception {
        File docFile = Files.createTempFile("mods", ".xml").toFile();
        docFile.delete();

        validator.validate(docFile);
    }

    private InputStream convertDocumentToStream(Document doc) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());

    }
}
