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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ResourceType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetPathFilterTest extends Assert {
	
	private DocumentIndexingPackageDataLoader loader;
	private DocumentIndexingPackageFactory factory;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		
		loader = new DocumentIndexingPackageDataLoader();
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
	}
	
	@Test
	public void fromQueryFileTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));
		results
				.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);
		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection", idb.getAncestorIds());
		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:File"));
		assertEquals(3, idb.getContentModel().size());
		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromQueryAggregateTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"Article/Aggregate Collectionl", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:AggregateWork"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:aggregate");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);

		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection/uuid:aggregate", idb.getAncestorIds());
		assertEquals(ResourceType.Aggregate.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:aggregate"));
		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromQueryAggregateChildTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:aggregate",
				"info:fedora/cdr-model:AggregateWork"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);

		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection/uuid:aggregate", idb.getAncestorIds());
		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals("uuid:aggregate", idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:aggregate"));
		assertFalse(idb.getAncestorPath().contains("4,uuid:File"));

		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromQueryOutofOrderFileTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));

		results.add(Arrays.asList("repo", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("repo", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("repo", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));

		results
				.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);
		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection", idb.getAncestorIds());
		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:File"));
		assertEquals(3, idb.getContentModel().size());
		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromQueryContainerTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:Container"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);

		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection/uuid:folder", idb.getAncestorIds());
		assertEquals(ResourceType.Folder.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:folder"));
		assertEquals(3, idb.getContentModel().size());

		assertEquals(2, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromQueryOrderTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:folder",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);
		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:collection/uuid:folder", idb.getAncestorIds());
		assertEquals("1,uuid:Collections", idb.getAncestorPath().get(0));
		assertEquals("2,uuid:collection", idb.getAncestorPath().get(1));
		assertEquals("3,uuid:folder", idb.getAncestorPath().get(2));
		assertEquals(3, idb.getAncestorPath().size());
	}

	@Test
	public void fromQueryNoCollectionTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("root", "info:fedora/uuid:Collections",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:folder",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections", "info:fedora/uuid:folder",
				"info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:folder", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);

		filter.filter(dip);

		assertEquals("/uuid:Collections/uuid:folder", idb.getAncestorIds());
		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertNull(idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:folder"));
	}

	@Test(expected = IndexingException.class)
	public void fromQueryNoResults() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);

		filter.filter(dip);
	}

	private DocumentIndexingPackage getParentFolderWithCollection() {
		DocumentIndexingPackage parentCollection = factory.createDip("info:fedora/uuid:collection");
		parentCollection.setResourceType(ResourceType.Collection);
		parentCollection.setLabel("collection");
		parentCollection.getDocument().setAncestorIds("/uuid:Collections/uuid:collection");
		parentCollection.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections"));

		DocumentIndexingPackage parentFolder = factory.createDip("info:fedora/uuid:folder");
		parentFolder.getDocument().setRollup("uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.setParentDocument(parentCollection);
		parentFolder.getDocument().setParentCollection(parentCollection.getPid().getPidAsString());
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorIds("/uuid:Collections/uuid:collection/uuid:folder");
		parentFolder.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections", "2,uuid:collection"));

		return parentFolder;
	}

	@Test
	public void fromParentsAggregateTest() throws Exception {
		DocumentIndexingPackage parentFolder = getParentFolderWithCollection();

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:aggregate");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);

		assertEquals(ResourceType.Aggregate.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals("/uuid:Collections/uuid:collection/uuid:folder/uuid:aggregate", idb.getAncestorIds());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:folder"));
		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromParentsFileTest() throws Exception {
		DocumentIndexingPackage parentFolder = getParentFolderWithCollection();

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);

		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/uuid:Collections/uuid:collection/uuid:folder", idb.getAncestorIds());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:folder"));
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals(3, idb.getResourceTypeSort().intValue());
	}

	@Test
	public void fromParentsNoCollectionTest() throws Exception {
		DocumentIndexingPackage parentFolder = factory.createDip("info:fedora/uuid:folder");
		parentFolder.getDocument().setRollup("uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.getDocument().setParentCollection(null);
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorIds("/uuid:Collections/uuid:folder");
		parentFolder.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections"));

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);

		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertNull(idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/uuid:Collections/uuid:folder", idb.getAncestorIds());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:folder"));
	}

	@Test
	public void fromParentsAggregateChildTest() throws Exception {
		DocumentIndexingPackage parentCollection = factory.createDip("info:fedora/uuid:collection");
		parentCollection.getDocument().setRollup("uuid:collection");
		parentCollection.setResourceType(ResourceType.Collection);
		parentCollection.setLabel("collection");
		parentCollection.getDocument().setAncestorIds("/uuid:Collections/uuid:collection");
		parentCollection.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections"));

		DocumentIndexingPackage parentFolder = factory.createDip("info:fedora/uuid:aggregate");
		parentFolder.getDocument().setRollup("uuid:aggregate");
		parentFolder.setResourceType(ResourceType.Aggregate);
		parentFolder.setParentDocument(parentCollection);
		parentFolder.getDocument().setParentCollection(parentCollection.getPid().getPidAsString());
		parentFolder.setLabel("aggregate");
		parentFolder.getDocument().setAncestorIds("/uuid:Collections/uuid:collection/uuid:aggregate");
		parentFolder.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections", "2,uuid:collection"));

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);

		assertEquals(ResourceType.File.name(), idb.getResourceType());
		assertEquals("uuid:aggregate", idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(3, idb.getAncestorPath().size());
		assertEquals("A1100-A800 NS final.jpg", dip.getLabel());
		assertEquals("/uuid:Collections/uuid:collection/uuid:aggregate", idb.getAncestorIds());
		assertTrue(idb.getAncestorPath().contains("1,uuid:Collections"));
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection"));
		assertTrue(idb.getAncestorPath().contains("3,uuid:aggregate"));
	}

	@Test(expected = IndexingException.class)
	public void fromParentsNoAncestorsTest() throws Exception {
		DocumentIndexingPackage parentFolder = factory.createDip("info:fedora/uuid:folder");
		parentFolder.setResourceType(ResourceType.Folder);
		parentFolder.setLabel("folder");
		parentFolder.getDocument().setAncestorIds("");
		parentFolder.getDocument().setAncestorPath(new ArrayList<String>());

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		dip.setParentDocument(parentFolder);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);
	}

	@Test
	public void fromParentsImmediateChildOfCollections() throws Exception {
		DocumentIndexingPackage parentCollections = factory.createDip("info:fedora/uuid:Collections");
		parentCollections.setResourceType(ResourceType.Collection);
		parentCollections.setLabel("Collections");
		parentCollections.getDocument().setAncestorIds("/uuid:Collections");
		parentCollections.getDocument().setAncestorPath(new ArrayList<String>());

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");
		dip.setParentDocument(parentCollections);
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		IndexDocumentBean idb = dip.getDocument();

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.filter(dip);

		assertEquals(1, idb.getAncestorPath().size());
		assertEquals("/uuid:Collections", idb.getAncestorIds());
	}

	@Test(expected = IndexingException.class)
	public void orphanedTest() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();

		results.add(Arrays.asList("", "info:fedora/uuid:collection",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("", "info:fedora/uuid:collection",
				"info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("", "info:fedora/uuid:collection", "info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("", "info:fedora/uuid:collection", "info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection", "info:fedora/uuid:File",
				"info:fedora/fedora-system:FedoraObject-3.0"));

		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);

		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:File");

		SetPathFilter filter = new SetPathFilter();
		filter.setCollectionsPid(new PID("uuid:Collections"));
		filter.setTsqs(tsqs);
		filter.filter(dip);
	}
}
