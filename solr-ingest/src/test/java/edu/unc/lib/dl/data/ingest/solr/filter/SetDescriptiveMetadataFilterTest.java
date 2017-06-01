/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.VocabularyHelperManager;

/**
 * 
 * @author bbpennel, harring
 *
 */
public class SetDescriptiveMetadataFilterTest {

	private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";
	
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	@Mock
	private DocumentIndexingPackage dip;
	@Mock
	private IndexDocumentBean idb;
	
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

		when(pid.getPid()).thenReturn(PID_STRING);
		
		when(dip.getDocument()).thenReturn(idb);
		when(dip.getPid()).thenReturn(pid);

		filter = new SetDescriptiveMetadataFilter();
		setField(filter, "vocabManager", vocabManager);
	}

	@Test
	public void testInventory() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document modsDoc = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/inventoryMods.xml")));
		when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
		
		List<String> keywords = new ArrayList<>();
		when(idb.getKeyword()).thenReturn(keywords);
		
		filter.filter(dip);
		
		verify(idb).setTitle(eq("Paper title"));
		verify(idb, never()).setOtherTitle(anyListOf(String.class));
		
		verify(idb).setCreator(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("Test, author"));
		verify(idb).setCreatorSort("Test, author");

		verify(idb).setContributor(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("Test, author"));
		assertTrue(listCaptor.getValue().contains("Test, contributor"));
		
		verify(idb, never()).setDepartment(anyListOf(String.class));
		
		verify(idb).setAbstractText("Abstract text");
		
		verify(idb).setSubject(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("Test resource"));
		
		verify(idb).setLanguage(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("English"));
		
		verify(idb).setDateCreated(dateCaptor.capture());
		Date dateCreated = dateCaptor.getValue();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		assertEquals("2006-04", dateFormat.format(dateCreated));
		
		verify(idb).setIdentifier(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("local|abc123"));
		assertFalse(listCaptor.getValue().contains("uri|http://example.com"));
		assertTrue(keywords.contains("abc123"));
		
		assertTrue(keywords.contains("Dissertation"));
		assertTrue(keywords.contains("text"));
		assertTrue(keywords.contains("note"));
		assertTrue(keywords.contains("phys note"));
		assertTrue(keywords.contains("Cited source"));
		
		verify(idb).setCitation(eq("citation text"));
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
		
		verify(idb).setDateCreated(dateCaptor.capture());
		Date dateIssued = dateCaptor.getValue();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		assertEquals("2006-05", dateFormat.format(dateIssued));
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
		
		verify(idb).setDateCreated(dateCaptor.capture());
		Date dateCaptured = dateCaptor.getValue();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		assertEquals("2006-03", dateFormat.format(dateCaptured));
	}
	
	@Test
	public void testNamePartConcatenation() throws Exception {
		
	}
	
	@Test
	public void testMultipleCreators() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document modsDoc = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/multipleCreators.xml")));
		when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
		
		filter.filter(dip);
		
		verify(idb).setCreator(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("Test, Creator1"));
		verify(idb).setCreatorSort("Test, Creator1");
		assertTrue(listCaptor.getValue().contains("Test, Creator2"));

	}
	
	@Test
	public void testAffiliationVocabTerm() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document modsDoc = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/inventoryMods.xml")));
		when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
		when(vocabManager.getAuthoritativeForms(any(PID.class), any(Element.class))).thenReturn(value);
		
		filter.filter(dip);
		
		verify(idb).setDepartment(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("Music"));
	}
	
	@Test
	public void testInvalidLanguageCode() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document modsDoc = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/invalidLanguage.xml")));
		when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
		
		filter.filter(dip);
		
		verify(idb, never()).setLanguage(anyListOf(String.class));
	}
	
	@Test
	public void testMultipleLanguages() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document modsDoc = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/inventoryMods.xml")));
		when(dip.getMods()).thenReturn(modsDoc.detachRootElement());
		
		filter.filter(dip);
		
		verify(idb).setLanguage(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains("English"));
		assertTrue(listCaptor.getValue().contains("Cherokee"));
	}

	@Test
	public void noMODS() throws Exception {
		when(idb.getTitle()).thenReturn(null);
		when(dip.getLabel()).thenReturn("test label");
		when(idb.getKeyword()).thenReturn(new ArrayList<String>());

		filter.filter(dip);
		
		verify(idb, never()).setAbstractText(any(String.class));
		verify(idb, never()).setLanguage(anyListOf(String.class));
		verify(idb, never()).setSubject(anyListOf(String.class));
		verify(idb, never()).setDateCreated(any(Date.class));
		verify(idb, never()).setCitation(any(String.class));
		verify(idb, never()).setIdentifier(anyListOf(String.class));
		verify(idb).setTitle(stringCaptor.capture());
		// check that title and keyword still get set in spite of no mods
		assertEquals("test label", stringCaptor.getValue());
		assertTrue(idb.getKeyword().contains(PID_STRING));
		
	}

}
