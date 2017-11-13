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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.VocabularyHelperManager;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class SetDescriptiveMetadataFilterTest {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;

    private IndexDocumentBean idb;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Resource objResc;

    @Mock
    private PID pid;

    @Mock
    private VocabularyHelperManager vocabManager;

    private SetDescriptiveMetadataFilter filter;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Captor
    private ArgumentCaptor<Date> dateCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.getId()).thenReturn(PID_STRING);

        idb = new IndexDocumentBean();
        idb.setTitle("Title");

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);
        when(contentObj.getResource()).thenReturn(objResc);

        filter = new SetDescriptiveMetadataFilter();
        setField(filter, "vocabManager", vocabManager);
    }

    @Test
    public void testInventory() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("Paper title", idb.getTitle());
        assertNull(idb.getOtherTitle());

        assertTrue(idb.getCreator().contains("Test, author"));
        assertEquals("Test, author", idb.getCreatorSort());

        List<String> contributors = idb.getContributor();
        assertTrue(contributors.contains("Test, author"));
        assertTrue(contributors.contains("Test, contributor"));

        assertNull(idb.getDepartment());

        assertEquals("Abstract text", idb.getAbstractText());

        assertTrue(idb.getSubject().contains("Test resource"));

        assertTrue(idb.getLanguage().contains("English"));

        assertEquals("2006-04", dateFormat.format(idb.getDateCreated()));

        List<String> ids = idb.getIdentifier();
        assertTrue(ids.contains("local|abc123"));
        assertFalse(ids.contains("uri|http://example.com"));

        List<String> keywords = idb.getKeyword();
        assertTrue(keywords.contains("abc123"));

        assertTrue(keywords.contains("Dissertation"));
        assertTrue(keywords.contains("text"));
        assertTrue(keywords.contains("note"));
        assertTrue(keywords.contains("phys note"));
        assertTrue(keywords.contains("Cited source"));

        assertEquals("citation text", idb.getCitation());
    }

    /*
     * Covers case when there is not a dateCreated, but there are both dateIssued and dateCaptured fields
     */
    @Test
    public void testDateIssuedPreference() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/dateIssued.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("2006-05", dateFormat.format(idb.getDateCreated()));
    }

    /*
     * Covers case when there is only a dateCaptured field
     */
    @Test
    public void testDateCapturedPreference() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/dateCaptured.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("2006-03", dateFormat.format(idb.getDateCreated()));
    }

    @Test
    public void testNamePartConcatenation() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/nameParts.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertTrue(idb.getCreator().contains("Repo, Boxy"));
        assertEquals("Repo, Boxy", idb.getCreatorSort());

        assertTrue(idb.getContributor().contains("Boxy"));
    }

    @Test
    public void testMultipleCreators() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/multipleCreators.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertTrue(idb.getCreator().contains("Test, Creator1"));
        assertTrue(idb.getCreator().contains("Test, Creator2"));

        assertEquals("Test, Creator1", idb.getCreatorSort());
    }

    @Test
    public void testAffiliationVocabTerm() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
        Map<String, List<List<String>>> authTerms = new HashMap<>();
        List<List<String>> terms = new ArrayList<>();
        List<String> depts = new ArrayList<>();
        depts.add("Music");
        terms.add(depts);
        authTerms.put("http://cdr.unc.edu/vocabulary/Affiliation", terms);
        when(vocabManager.getAuthoritativeForms(any(PID.class), any(Element.class))).thenReturn(authTerms);

        filter.filter(dip);

        assertTrue(idb.getDepartment().contains("Music"));
    }

    @Test
    public void testInvalidLanguageCode() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/invalidLanguage.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertNull(idb.getLanguage());
    }

    @Test
    public void testMultipleLanguages() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertTrue(idb.getLanguage().contains("English"));
        assertTrue(idb.getLanguage().contains("Cherokee"));
    }

    @Test
    public void noMODS() throws Exception {
        Date dateAdded = new Date();
        idb.setDateAdded(dateAdded);

        idb.setTitle(null);

        when(objResc.hasProperty(DcElements.title)).thenReturn(true);
        Statement titleStmt = mock(Statement.class);
        when(titleStmt.getString()).thenReturn("test label");
        when(objResc.getProperty(DcElements.title)).thenReturn(titleStmt);

        filter.filter(dip);

        assertNull(idb.getAbstractText());
        assertNull(idb.getLanguage());
        assertNull(idb.getSubject());
        assertNull(idb.getCitation());
        assertNull(idb.getIdentifier());

        // check that title, keyword and date created still get set
        assertEquals("test label", idb.getTitle());
        assertEquals(dateAdded, idb.getDateCreated());
        assertTrue(idb.getKeyword().contains(PID_STRING));
    }

}
