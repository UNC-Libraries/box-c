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
package edu.unc.lib.dl.schematron;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Test the functions of the SchematronValidator class.
 *
 * @author count0
 *
 */
public class SchematronValidatorTest extends Assert {
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
            assertTrue("This XML should be valid according to the schema", valid);
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
            assertFalse("This XML must be invalid according to the schema", valid);
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
        assertFalse("This XML must be invalid according to the schema", valid);
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
        assertFalse("This XML must be invalid according to the schema", valid);
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
        assertFalse("This XML must be invalid according to the schema", valid);
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
        assertTrue("This XML must be valid according to the schema", valid);
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
        assertTrue("This XML must be valid according to the schema \n"+msg, valid);
    } catch (IOException e) {
        fail("Got exception" + e.getMessage());
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
        assertTrue("The Raschke example MODS file should be valid.", valid);
    } catch (IOException e) {
        log.error("cannot read test file", e);
        fail(e.getLocalizedMessage());
    }
    }
}
