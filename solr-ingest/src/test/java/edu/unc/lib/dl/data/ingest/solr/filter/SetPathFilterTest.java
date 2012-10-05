package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.ResourceType;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import static org.mockito.Mockito.*;

public class SetPathFilterTest extends Assert {

	@Test
	public void fromQueryItemTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();
		
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:item","item.jpg","info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:item","item.jpg","info:fedora/cdr-model:JP2DerivedImage"));
		results.add(Arrays.asList("info:fedora/uuid:item","item.jpg","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:item","item.jpg","info:fedora/fedora-system:FedoraObject-3.0"));
		
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
		
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","Article/Aggregate Collection","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection","Article/Aggregate Collection","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection","Article/Aggregate Collectionl","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","Article/Aggregate Collection","info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum","info:fedora/cdr-model:AggregateWork"));
		
		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		IndexDocumentBean idb = dip.getDocument();
		
		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);
		
		filter.filter(dip);
		
		assertEquals("/Collections/Article\\/Aggregate Collection/2, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum", idb.getAncestorNames());
		assertEquals(ResourceType.Aggregate.name(), idb.getResourceType());
		assertEquals(idb.getId(), idb.getRollup());
		assertEquals("uuid:collection", idb.getParentCollection());
		assertEquals(2, idb.getAncestorPath().size());
		assertTrue(idb.getAncestorPath().contains("2,uuid:collection,Article/Aggregate Collection"));
		assertFalse(idb.getAncestorPath().contains("3,uuid:aggregate,2\\, 4-Diamino-6- hydroxy pyrimidine inhibits NSAIDs induced nitrosyl-complex EPR signals and ulcer in rat jejunum"));
	}
	
	@Test
	public void fromQueryAggregateChildTest() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();
		
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","aggregate","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","aggregate","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","aggregate","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:aggregate","aggregate","info:fedora/cdr-model:AggregateWork"));
		results.add(Arrays.asList("info:fedora/uuid:item","child.pdf","info:fedora/cdr-model:Simple"));
		results.add(Arrays.asList("info:fedora/uuid:item","child.pdf","info:fedora/cdr-model:JP2DerivedImage"));
		results.add(Arrays.asList("info:fedora/uuid:item","child.pdf","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:item","child.pdf","info:fedora/fedora-system:FedoraObject-3.0"));
		
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
		
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:Collections","Collections","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Container"));
		results.add(Arrays.asList("info:fedora/uuid:collection","collection","info:fedora/cdr-model:Collection"));
		results.add(Arrays.asList("info:fedora/uuid:folder","folder","info:fedora/cdr-model:PreservedObject"));
		results.add(Arrays.asList("info:fedora/uuid:folder","folder","info:fedora/fedora-system:FedoraObject-3.0"));
		results.add(Arrays.asList("info:fedora/uuid:folder","folder","info:fedora/cdr-model:Container"));
		
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
	
	@Test(expected=IndexingException.class)
	public void fromQueryNoResults() {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();
		
		when(tsqs.queryResourceIndex(anyString())).thenReturn(results);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		
		SetPathFilter filter = new SetPathFilter();
		filter.setTripleStoreQueryService(tsqs);
		
		filter.filter(dip);
	}
}

