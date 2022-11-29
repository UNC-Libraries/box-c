package edu.unc.lib.boxc.operations.impl.vocab;

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

import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.operations.impl.vocab.JSONVocabularyHelper;

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
