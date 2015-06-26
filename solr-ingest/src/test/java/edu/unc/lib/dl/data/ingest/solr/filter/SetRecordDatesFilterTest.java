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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetRecordDatesFilterTest extends Assert {
	
	private DocumentIndexingPackageDataLoader loader;
	private DocumentIndexingPackageFactory factory;
	
	@Before
	public void setup() throws Exception {
		loader = new DocumentIndexingPackageDataLoader();
		
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
	}
	
	@Test
	public void foxmlExtractionTest() throws Exception {
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);
		
		SetRecordDatesFilter filter = new SetRecordDatesFilter();
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		Date dateAdded = DateTimeUtil.parseUTCToDate("2011-10-04T20:31:52.107Z");
		Date dateUpdated = DateTimeUtil.parseUTCToDate("2011-10-05T04:25:07.169Z");
		
		assertEquals(dateAdded, idb.getDateAdded());
		assertEquals(dateUpdated, idb.getDateUpdated());
	}

	@Test
	public void queryExtractionTest() throws Exception {
		
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		Map<String, List<String>> triples = new HashMap<>();
		triples.put(FedoraProperty.createdDate.toString(), Arrays.asList("2011-10-04T20:31:52.107Z"));
		triples.put(FedoraProperty.lastModifiedDate.toString(), Arrays.asList("2011-10-05T04:25:07.169Z"));
		
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);
		loader.setTsqs(tsqs);
		
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:item");
		
		SetRecordDatesFilter filter = new SetRecordDatesFilter();
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		Date dateAdded = DateTimeUtil.parseUTCToDate("2011-10-04T20:31:52.107Z");
		Date dateUpdated = DateTimeUtil.parseUTCToDate("2011-10-05T04:25:07.169Z");
		
		assertEquals(dateAdded, idb.getDateAdded());
		assertEquals(dateUpdated, idb.getDateUpdated());
	}
}
