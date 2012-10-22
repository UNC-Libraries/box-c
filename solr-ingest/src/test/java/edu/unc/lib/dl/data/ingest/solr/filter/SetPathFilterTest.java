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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.ResourceType;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import static org.mockito.Mockito.*;

public class SetPathFilterTest extends Assert {

	@Test
	public void fromQueryItemTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results
				.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item.jpg", "info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item.jpg", "info:fedora/cdr-model:JP2DerivedImage"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item.jpg", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item.jpg", "info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);

		assertEquals("/Collections/collection", idb.getAncestorNames());
		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:item,item.jpg"));
		assertEquals(4, idb.getContentModel().size());
	}

	@Test
	public void fromQueryAggregateTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results
				.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "Article/Aggregate Collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "Article/Aggregate Collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "Article/Aggregate Collectionl",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "Article/Aggregate Collection",
				"info:fedora/cdr-model:Collection"));
		results
				.add(Arrays
						.asList(
								"info:fedora/uuid:aggregate",
								"2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum",
								"info:fedora/cdr-model:PreservedObject"));
		results
				.add(Arrays
						.asList(
								"info:fedora/uuid:aggregate",
								"2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum",
								"info:fedora/fedora-system:FedoraObject-3.0"));
		results
				.add(Arrays
						.asList(
								"info:fedora/uuid:aggregate",
								"2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum",
								"info:fedora/cdr-model:Container"));
		results
				.add(Arrays
						.asList(
								"info:fedora/uuid:aggregate",
								"2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum",
								"info:fedora/cdr-model:AggregateWork"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);

		assertEquals(
				"/Collections/Article\\/Aggregate Collection/2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum",
				idb.getAncestorNames());
		assertEquals(ResourceType.Aggregate.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,Article/Aggregate Collection"));
		assertFalse(idb
				.getAncestorPath()
				.contains(
						"3,uuid:aggregate,2\\, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum"));
	}

	@Test
	public void fromQueryAggregateChildTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results
				.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "aggregate", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays
				.asList("info:fedora/uuid:aggregate", "aggregate", "info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "aggregate", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "aggregate", "info:fedora/cdr-model:AggregateWork"));
		results.add(Arrays.asList("info:fedora/uuid:item", "child.pdf", "info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:item", "child.pdf", "info:fedora/cdr-model:JP2DerivedImage"));
		results.add(Arrays.asList("info:fedora/uuid:item", "child.pdf", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:item", "child.pdf", "info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);

		assertEquals("/Collections/collection/aggregate", idb.getAncestorNames());
		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals("uuid:aggregate", idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:aggregate,aggregate"));
		assertFalse(idb.getAncestorPath().contains("4,uuid:item,child.pdf"));
	}

	@Test
	public void fromQueryContainerTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results
				.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "collection", "info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/cdr-model:Container"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);

		assertEquals("/Collections/collection/folder", idb.getAncestorNames());
		assertEquals(ResourceType.Folder.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:folder,folder"));
		assertEquals(3, idb.getContentModel().size());
	}

	@Test
	public void fromQueryNoCollectionTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results
				.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "Collections", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "folder", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item", "info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item", "info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:item", "item", "info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);

		assertEquals("/Collections/folder", idb.getAncestorNames());
		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertNull(idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:folder,folder"));
	}

	@Test(expected = IndexingException.class)
	public void fromQueryNoResults() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");

		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);

		filter.filter(dip);
	}
	
	private DocumentIndexingPackage getParentFolderWithCollection() {
		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setResourceType(ResourceType.Collection);
		parentCollection.setLabel("collection");
		parentCollection.getDocument().setAncestorNames("/Collections/collection");
		parentCollection.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections,Collections"));

		DocumentIndexingPackage parentFolder = new DocumentIndexingPackage("info:fedora/uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.setParentDocument(parentCollection);
		parentFolder.getDocument().setParentCollection(parentCollection.getPid().getPid());
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorNames("/Collections/collection/folder");
		parentFolder.getDocument().setAncestorPath(
				Arrays.asList("1,uuid:Collections,Collections", "2,uuid:collection,collection"));
		
		return parentFolder;
	}

	@Test
	public void fromParentsAggregateTest() throws FileNotFoundException, JDOMException, IOException {
		DocumentIndexingPackage parentFolder = getParentFolderWithCollection();

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.filter(dip);

		assertEquals(ResourceType.Aggregate.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals(
				"A Comparison of Machine Learning Algorithms for Chemical Toxicity Classification Using a Simulated Multi-Scale Data Model",
				dip.getLabel());
		assertEquals(
				"/Collections/collection/folder/A Comparison of Machine Learning Algorithms for Chemical Toxicity Classification Using a Simulated Multi-Scale Data Model",
				idb.getAncestorNames());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections,Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:folder,folder"));
	}

	@Test
	public void fromParentsItemTest() throws FileNotFoundException, JDOMException, IOException {
		DocumentIndexingPackage parentFolder = getParentFolderWithCollection();

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.filter(dip);

		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/Collections/collection/folder", idb.getAncestorNames());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections,Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:folder,folder"));
	}
	
	@Test
	public void fromParentsNoCollectionTest() throws FileNotFoundException, JDOMException, IOException {
		DocumentIndexingPackage parentFolder = new DocumentIndexingPackage("info:fedora/uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.getDocument().setParentCollection(null);
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorNames("/Collections/folder");
		parentFolder.getDocument().setAncestorPath(
				Arrays.asList("1,uuid:Collections,Collections"));

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.filter(dip);

		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertNull(idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/Collections/folder", idb.getAncestorNames());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections,Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:folder,folder"));
	}
	
	@Test
	public void fromParentsAggregateChildTest() throws FileNotFoundException, JDOMException, IOException {
		DocumentIndexingPackage parentCollection = new DocumentIndexingPackage("info:fedora/uuid:collection");
		parentCollection.setResourceType(ResourceType.Collection);
		parentCollection.setLabel("collection");
		parentCollection.getDocument().setAncestorNames("/Collections/collection");
		parentCollection.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections,Collections"));

		DocumentIndexingPackage parentFolder = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		parentFolder.setResourceType(ResourceType.Aggregate);
		parentFolder.setParentDocument(parentCollection);
		parentFolder.getDocument().setParentCollection(parentCollection.getPid().getPid());
		parentFolder.setLabel("aggregate");
		parentFolder.getDocument().setAncestorNames("/Collections/collection/aggregate");
		parentFolder.getDocument().setAncestorPath(
				Arrays.asList("1,uuid:Collections,Collections", "2,uuid:collection,collection"));

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.filter(dip);

		assertEquals(ResourceType.Item.name(), idb.getResourceType());
		assertEquals("uuid:aggregate", idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/Collections/collection/aggregate", idb.getAncestorNames());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections,Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:aggregate,aggregate"));
	}
	
	@Test(expected = IndexingException.class)
	public void fromParentsNoAncestorsTest() throws FileNotFoundException, JDOMException, IOException {
		DocumentIndexingPackage parentFolder = new DocumentIndexingPackage("info:fedora/uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorNames("");
		parentFolder.getDocument().setAncestorPath(new ArrayList<String>());

		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPID(new PID("uuid:Collections"));
		filter.filter(dip);
	}
}
