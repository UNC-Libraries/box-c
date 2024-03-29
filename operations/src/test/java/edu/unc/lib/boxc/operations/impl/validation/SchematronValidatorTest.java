package edu.unc.lib.boxc.operations.impl.validation;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Test the functions of the SchematronValidator class.
 *
 * @author count0
 *
 */
public class SchematronValidatorTest extends Assertions {
    private static final Log log = LogFactory.getLog(SchematronValidatorTest.class);

    /**
     * Tests that the expected output from the forms app should be valid.
     */
    @Test
    public void testGoodSimpleForm() {

        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource bad = new ClassPathResource("/samples/good-simple-form.xml", SchematronValidator.class);

        try {
            boolean valid = sv.isValid(bad, "test");
            Document output = sv.validate(bad, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertTrue(valid, "This XML should be valid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }

    }

    /**
     * Tests the result of a missing metsHdr section.
     */
    @Test
    public void testMissingMetsHdr() {

        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource bad = new ClassPathResource("/samples/bad-missing-mets-hdr.xml", SchematronValidator.class);

        try {
            boolean valid = sv.isValid(bad, "test");
            Document output = sv.validate(bad, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertFalse(valid, "This XML must be invalid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }

    }

    /**
     * Tests the result of isValid on an invalid document.
     */
    @Test
    public void testBadValidation() {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource bad = new ClassPathResource("/samples/bad-test.xml", SchematronValidator.class);
        try {
            boolean valid = sv.isValid(bad, "test");
            Document output = sv.validate(bad, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertFalse(valid, "This XML must be invalid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }
    }

    /**
     * Tests the result of a missing Flocat.
     */
    @Test
    public void testMissingFlocat() {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource bad = new ClassPathResource("/samples/missing-flocat-test.xml", SchematronValidator.class);
        try {
            boolean valid = sv.isValid(bad, "test");
            Document output = sv.validate(bad, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertFalse(valid, "This XML must be invalid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }
    }

    /**
     * Tests the result of an invalid namespace under rightsMD.
     */
    @Test
    public void testBadACLValidation() {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource bad = new ClassPathResource("/samples/bad-acl-test.xml", SchematronValidator.class);
        try {
            boolean valid = sv.isValid(bad, "test");
            Document output = sv.validate(bad, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertFalse(valid, "This XML must be invalid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }
    }

    /**
     * Tests the result of isValid on an valid document.
     */
    @Test
    public void testGoodEPrintGenreValidation() {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("vocabularies-mods.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource good = new ClassPathResource("/samples/good-eprints-test.xml", SchematronValidator.class);

        try {
            boolean valid = sv.isValid(good, "test");
            Document output = sv.validate(good, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            log.info(dbout.outputString(output));
            assertTrue(valid, "This XML must be valid according to the schema");
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }
    }

    /**
     * Tests the result of isValid on an valid document.
     */
    @Test
    public void testGoodValidation() {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource good = new ClassPathResource("/samples/good-test.xml", SchematronValidator.class);

        try {
            boolean valid = sv.isValid(good, "test");
            Document output = sv.validate(good, "test");
            XMLOutputter dbout = new XMLOutputter();
            dbout.setFormat(Format.getPrettyFormat());
            String msg = dbout.outputString(output);
            log.info(msg);
            assertTrue(valid, "This XML must be valid according to the schema \n" + msg);
        } catch (IOException e) {
            fail("Got exception" + e.getMessage());
        }
    }

    /**
     * Tests that a METS document attempting to set doctype info is rejected
     */
    @Test
    public void testDoctypeDisallowed() throws Exception {
        SchematronValidator sv = new SchematronValidator();
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        ClassPathResource good = new ClassPathResource("/samples/simple-xxe-mets.xml", SchematronValidator.class);

        try {
            sv.isValid(good, "test");
            fail("Must not allow doctype");
        } catch (Error e) {
            assertTrue(e.getCause().getMessage().contains("DOCTYPE is disallowed"));
        }
        try {
            sv.validate(good, "test");
            fail("Must not allow doctype");
        } catch (Error e) {
            assertTrue(e.getCause().getMessage().contains("DOCTYPE is disallowed"));
        }
    }

    /**
     * Test initialization of SchematronValidator.
     */
    @Test
    public void testLoadSchemas() {
        SchematronValidator sv = new SchematronValidator();
        // load empty schema map (tests the loading of iso stylesheets)
        sv.loadSchemas();

        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
        sv.getSchemas().put("test", test);
        sv.loadSchemas();

        // now reload of the same schemas
        sv.loadSchemas();
    }

    @Test
    public void testObjectModsSchema() {
        SchematronValidator sv = new SchematronValidator();
        // now add a schema by means of a Spring Resource object
        ClassPathResource test = new ClassPathResource("object-mods.sch", SchematronValidator.class);
        sv.getSchemas().put("object-mods", test);
        sv.loadSchemas();

        try {
            ClassPathResource mods = new ClassPathResource("/samples/raschke_mods.xml", SchematronValidator.class);
            Document report = sv.validate(mods, "object-mods");
            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            String doc = out.outputString(report);
            log.info(doc);
            boolean valid = sv.isValid(mods, "object-mods");
            assertTrue(valid, "The Raschke example MODS file should be valid.");
        } catch (IOException e) {
            log.error("cannot read test file", e);
            fail(e.getLocalizedMessage());
        }
    }
}
