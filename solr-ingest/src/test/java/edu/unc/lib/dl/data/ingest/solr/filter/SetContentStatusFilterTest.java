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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SetContentStatusFilterTest extends Assert {

	@Test
	public void described() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		Map<String, List<String>> triples = new HashMap<String, List<String>>();
		triples.put(ContentModelHelper.FedoraProperty.disseminates.toString(), Arrays.asList("info:fedora/uuid:item/"
				+ ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), "info:fedora/uuid:item/"
				+ ContentModelHelper.Datastream.RELS_EXT.getName()));
		
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetContentStatusFilter filter = new SetContentStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();
		assertTrue(idb.getContentStatus().contains("Described"));
		
		triples.put(ContentModelHelper.FedoraProperty.disseminates.toString(), Arrays.asList("info:fedora/uuid:item/"
				+ ContentModelHelper.Datastream.RELS_EXT.getName()));
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);
		filter.filter(dip);
		assertTrue(idb.getContentStatus().contains("Not Described"));
		assertFalse(idb.getContentStatus().contains("Described"));
	}
	
	@Test
	public void describedFoxml() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		SetContentStatusFilter filter = new SetContentStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(idb.getContentStatus().contains("Described"));
		assertTrue(idb.getContentStatus().contains("Default Access Object Assigned"));
	}
}
