package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
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
		
		results.add(Arrays.asList("info:fedora/uuid:c34ae354-8626-48c6-9963-d907aa65a713", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:d5074f9a-97c2-4fcb-8bf1-c4b0221c00a4", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:99543401-5a08-4148-a589-4d7e924e6622", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:4ecc25e9-cc02-4191-8589-ffbaecac258f", "http://mulgara.org/mulgara#null"));
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
	
	@Test(expected=IndexingException.class)
	public void noQueryResults() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();when(tsqs.queryResourceIndex(anyString())).thenReturn(results);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		
		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);
	}
	
	@Test(expected=IndexingException.class)
	public void querySelfOnlyResults() throws Exception {
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		List<List<String>> results = new ArrayList<List<String>>();when(tsqs.queryResourceIndex(anyString())).thenReturn(results);
		results.add(Arrays.asList("info:fedora/uuid:item", "http://mulgara.org/mulgara#null"));
		results.add(Arrays.asList("info:fedora/uuid:item", "no"));
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		
		SetStatusFilter filter = new SetStatusFilter();
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);
	}
}
