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

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SetStatusFilterTest extends Assert {

	@Test
	public void publishedFromCache() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		assertNull(dip.getIsPublished());

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
	}

	@Test
	public void explicitlyPublishedFromCache() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder
				.build(new FileInputStream(new File("src/test/resources/foxml/itemExplicitPublished.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		assertNull(dip.getIsPublished());

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
	}

	@Test
	public void unpublishedFromRelsExt() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/itemUnpublished.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		assertNull(dip.getIsPublished());

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertTrue(idb.getStatus().contains("Unpublished"));
		assertFalse(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void unpublishedFromParent() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(false);
		dip.setParentDocument(parentCollection);

		assertNull(dip.getIsPublished());

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
		assertTrue(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void unpublishedFromBoth() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/itemUnpublished.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(false);
		dip.setParentDocument(parentCollection);

		assertNull(dip.getIsPublished());

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertTrue(idb.getStatus().contains("Unpublished"));
		assertTrue(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void publishedFromQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
	}

	@Test
	public void publishedExplicitlyFromQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:c34ae354-8626-48c6-9963-d907aa65a713",
				"http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:d5074f9a-97c2-4fcb-8bf1-c4b0221c00a4",
				"http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:99543401-5a08-4148-a589-4d7e924e6622",
				"http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:4ecc25e9-cc02-4191-8589-ffbaecac258f",
				"http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:4ecc25e9-cc02-4191-8589-ffbaecac258f", "yes"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:4ecc25e9-cc02-4191-8589-ffbaecac258f");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
	}

	@Test
	public void unpublishedFromQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "no"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertTrue(idb.getStatus().contains("Unpublished"));
	}

	@Test
	public void unpublishedFromParentQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "no"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
		assertTrue(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void publishedFromSelfAndParentQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "yes"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
		assertFalse(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void unpublishedFromSelfAndParentQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "no"));
		results.add(Arrays.asList("info:fedora/uuid:item", "no"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertTrue(idb.getStatus().contains("Unpublished"));
		assertTrue(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void unpublishedFromOverrideSelfQuery() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "yes"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "no"));
		results.add(Arrays.asList("info:fedora/uuid:item", "yes"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertFalse(dip.getIsPublished());
		assertFalse(idb.getStatus().contains("Published"));
		assertFalse(idb.getStatus().contains("Unpublished"));
		assertTrue(idb.getStatus().contains("Parent Unpublished"));
	}

	@Test
	public void described() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		Map<String, List<String>> triples = new HashMap<String, List<String>>();
		triples.put(ContentModelHelper.FedoraProperty.disseminates.toString(), Arrays.asList("info:fedora/uuid:item/"
				+ ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), "info:fedora/uuid:item/"
				+ ContentModelHelper.Datastream.RELS_EXT.getName()));
		
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples);

		List<List<String>> results = new ArrayList<List<String>>();
		results.add(Arrays.asList("info:fedora/uuid:Repository", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "yes"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "yes"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Published"));
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

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(idb.getContentStatus().contains("Described"));
		assertTrue(idb.getContentStatus().contains("Default Access Object Assigned"));
		
		assertTrue(dip.getIsPublished());
	}
	
	@Test
	public void embargoedStatus() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/embargoed.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Embargoed"));
		assertFalse(idb.getStatus().contains("Not Discoverable"));
	}
	
	@Test
	public void rolesAssigned() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/rolesAssigned.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setIsPublished(true);
		dip.setParentDocument(parentCollection);

		SetStatusFilter filter = new SetStatusFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertTrue(dip.getIsPublished());
		assertTrue(idb.getStatus().contains("Roles Assigned"));
		assertTrue(idb.getStatus().contains("Not Inheriting Roles"));
	}

	@Test(expected = IndexingException.class)
	public void noQueryResults() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();
		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);
	}
}
