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
package edu.unc.lib.dl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Oct 7, 2015
 */
public class JSONVocabularyHelperTest {

    private JSONVocabularyHelper helper;

    @Before
    public void init() throws Exception {
        helper = new JSONVocabularyHelper();
        helper.setContent(Files.readAllBytes(Paths.get("src/test/resources/jsonVocab.json")));
        helper.setSelectorNamespaces(new Namespace[] {JDOMNamespaceUtil.MODS_V3_NS});
        helper.setSelector("/mods:mods/mods:name/mods:role/mods:roleTerm");
    }

    @Test
    public void getAuthoritativeFormTest() {
        List<List<String>> terms = helper.getAuthoritativeForm("Creator");
        assertEquals("Creator", terms.get(0).get(0));

        terms = helper.getAuthoritativeForm("Rectangle");
        assertNull(terms);
    }

    @Test
    public void getVocabularyTermsTest() {
        Collection<String> terms = helper.getVocabularyTerms();
        assertEquals(6, terms.size());
        assertTrue(terms.contains("Reticulated Spline"));
        assertTrue(terms.contains("Creator"));
    }

    @Test
    public void getInvalidTermsTest() throws Exception {
        SAXBuilder builder = new SAXBuilder();

        InputStream modsStream = new FileInputStream(new File("src/test/resources/vocabTest.xml"));
        Document modsDoc = builder.build(modsStream);

        Set<String> invalids = helper.getInvalidTerms(modsDoc.getRootElement());

        assertEquals("Incorrect number of invalid terms", 2, invalids.size());
        assertTrue("Designated Destroyer not detected", invalids.contains("Designated Destroyer"));
        assertTrue(invalids.contains("Advisor"));
    }

}
