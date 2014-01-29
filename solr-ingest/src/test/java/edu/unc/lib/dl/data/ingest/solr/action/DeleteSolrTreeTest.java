package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.IndexingActionType;

public class DeleteSolrTreeTest extends BaseEmbeddedSolrTest {
	SearchSettings searchSettings;
	SolrSettings solrSettings;
	SolrSearchService solrSearchService;
	SolrUpdateDriver driver;
	
	@Test
	public void deleteTree() throws Exception {
		initializeDependencies();
		server.add(populate());
		server.commit();
		
		DeleteSolrTreeAction action = getDeleteAction();
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", "*:*");
		params.set("fl", "id,resourceType");
		QueryResponse qResp = server.query(params);
		SolrDocumentList docListBefore = qResp.getResults();
		
		assertEquals(4, docListBefore.getNumFound());
		
		action.performAction(new SolrUpdateRequest("uuid:2", IndexingActionType.DELETE_SOLR_TREE));
		server.commit();
		
		qResp = server.query(params);
		SolrDocumentList docListAfter = qResp.getResults();
		
		assertEquals(2, docListAfter.getNumFound());
		
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			if ("uuid:2".equals(id) || "uuid:6".equals(id))
				fail("Object was not deleted: " + id);
		}
	}
	
	@Test
	public void deleteNonexistent() throws Exception {
		initializeDependencies();
		server.add(populate());
		server.commit();
		
		DeleteSolrTreeAction action = getDeleteAction();
		
		SolrSearchService solrSearchService = mock(SolrSearchService.class);
		when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(null);
		action.setSolrSearchService(solrSearchService);
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", "*:*");
		params.set("fl", "id,resourceType,_version_");
		QueryResponse qResp = server.query(params);
		SolrDocumentList docListBefore = qResp.getResults();
		
		assertEquals(4, docListBefore.getNumFound());
		
		action.performAction(new SolrUpdateRequest("uuid:doesnotexist", IndexingActionType.DELETE_SOLR_TREE));
		server.commit();
		
		qResp = server.query(params);
		SolrDocumentList docListAfter = qResp.getResults();
		
		assertEquals(4, docListAfter.getNumFound());
	}
	
	protected DeleteSolrTreeAction getDeleteAction() {
		DeleteSolrTreeAction action = new DeleteSolrTreeAction();

		action.setSolrUpdateDriver(driver);
		action.setSolrSettings(solrSettings);
		action.setSearchSettings(searchSettings);
		action.setSolrSearchService(solrSearchService);
		action.setAccessGroups(new AccessGroupSet("admin"));
		
		return action;
	}
	
	protected void initializeDependencies() throws SolrServerException, IOException {
		
		
		searchSettings = mock(SearchSettings.class);
		when(searchSettings.getResourceTypeCollection()).thenReturn("Collection");
		when(searchSettings.getResourceTypeFolder()).thenReturn("Folder");
		
		solrSettings = mock(SolrSettings.class);
		when(solrSettings.getFieldName(eq(SearchFieldKeys.ANCESTOR_PATH.name()))).thenReturn("ancestorPath");
		when(solrSettings.getFieldName(eq(SearchFieldKeys.ID.name()))).thenReturn("id");
		
		solrSearchService = mock(SolrSearchService.class);
		BriefObjectMetadataBean metadata = new BriefObjectMetadataBean();
		metadata.setId("uuid:2");
		metadata.setAncestorPath(Arrays.asList("1,uuid:1,Collections"));
		metadata.setResourceType("Collection");
		when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(metadata);
		
		driver = new SolrUpdateDriver();
		driver.setSolrServer(server);
	}

	protected List<SolrInputDocument> populate() {
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		SolrInputDocument newDoc = new SolrInputDocument();
		newDoc.addField("title", "Collections");
		newDoc.addField("id", "uuid:1");
		newDoc.addField("rollup", "uuid:1");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections");
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "A collection");
		newDoc.addField("id", "uuid:2");
		newDoc.addField("rollup", "uuid:2");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "File");
		newDoc.addField("id", "uuid:6");
		newDoc.addField("rollup", "uuid:6");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections", "2,uuid:2,A collection"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Second collection");
		newDoc.addField("id", "uuid:3");
		newDoc.addField("rollup", "uuid:3");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/Second_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);
		
		return docs;
	}
}
