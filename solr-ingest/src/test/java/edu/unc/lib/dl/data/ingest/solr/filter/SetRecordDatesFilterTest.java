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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doCallRealMethod;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@RunWith(MockitoJUnitRunner.class)
public class SetRecordDatesFilterTest extends Assert {
	
	private static final String EXPECTED_ADDED = "2011-10-04T20:31:52.107Z";
	private static final String EXPECTED_UPDATED = "2011-10-05T04:25:07.169Z";
	private Date expectedDateAdded;
	private Date expectedDateUpdated;
	
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	@Mock
	private TripleStoreQueryService tsqs;
	
	private DocumentIndexingPackageFactory factory;
	
	private SetRecordDatesFilter filter;
	
	@Before
	public void setup() throws Exception {		
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
		
		when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenCallRealMethod();
		doCallRealMethod().when(loader).setTsqs(any(TripleStoreQueryService.class));
		loader.setTsqs(tsqs);
		
		expectedDateAdded = DateTimeUtil.parseUTCToDate(EXPECTED_ADDED);
		expectedDateUpdated = DateTimeUtil.parseUTCToDate(EXPECTED_UPDATED);
		
		filter = new SetRecordDatesFilter();
	}
	
	@Test
	public void foxmlExtractionTest() throws Exception {
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:item");
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);
		
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		assertEquals(expectedDateAdded, idb.getDateAdded());
		assertEquals(expectedDateUpdated, idb.getDateUpdated());
	}

	@Test
	public void queryExtractionTest() throws Exception {
		Map<String, List<String>> triples = new HashMap<>();
		triples.put(FedoraProperty.createdDate.toString(), Arrays.asList(EXPECTED_ADDED));
		triples.put(FedoraProperty.lastModifiedDate.toString(), Arrays.asList(EXPECTED_UPDATED));
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);
		
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:item");
		
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		assertEquals(expectedDateAdded, idb.getDateAdded());
		assertEquals(expectedDateUpdated, idb.getDateUpdated());
	}
	
	@Test
	public void foxmlFallbackTest() throws Exception {
		Map<String, List<String>> triples = new HashMap<>();
		triples.put(FedoraProperty.createdDate.toString(), Arrays.asList(EXPECTED_ADDED));
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);
		
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		when(loader.loadFOXML(any(DocumentIndexingPackage.class))).thenReturn(foxml);
		
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		assertEquals(expectedDateAdded, idb.getDateAdded());
		assertEquals(expectedDateUpdated, idb.getDateUpdated());
		
		verify(loader).loadFOXML(any(DocumentIndexingPackage.class));
	}
}
