package edu.unc.lib.dl.data.ingest.solr.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.IndexingActionType;

import static org.mockito.Mockito.*;

public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
	private static final Logger log = LoggerFactory.getLogger(IndexTreeInplaceActionTest.class);

	protected UpdateTreeAction getAction() {
		return new IndexTreeInplaceAction();
	}
	
	@Test
	public void verifyOrphanCleanup() throws SolrServerException, IOException {
		UpdateTreeAction action = initializeOrphanSet(populateChildren());
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", "*:*");
		params.set("fl", "id,_version_");
		QueryResponse qResp = server.query(params);
		SolrDocumentList docListBefore = qResp.getResults();
		
		SolrUpdateRequest request = new SolrUpdateRequest(new PID("uuid:2"), IndexingActionType.RECURSIVE_ADD, "1", null);
		request.setStatus(ProcessingStatus.ACTIVE);
		
		action.performAction(request);
		server.commit();
		
		qResp = server.query(params);
		SolrDocumentList docListAfter = qResp.getResults();
		
		log.debug("Docs: " + docListBefore);
		log.debug("Docs: " + docListAfter);
		
		// Verify that the number of results has decreased
		assertEquals(6, docListBefore.getNumFound());
		assertEquals(5, docListAfter.getNumFound());
		
		// Verify that the orphan is not in the new result set
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			assertFalse("uuid:5".equals(id));
		}
	}
	
	protected UpdateTreeAction initializeOrphanSet(final Map<String, List<PID>> children) throws SolrServerException, IOException {
		UpdateTreeAction action = super.initializeOrphanSet(children);
		
		SolrSettings solrSettings = mock(SolrSettings.class);
		when(solrSettings.getFieldName(eq(SearchFieldKeys.ANCESTOR_PATH.name()))).thenReturn("ancestorPath");
		when(solrSettings.getFieldName(eq(SearchFieldKeys.TIMESTAMP.name()))).thenReturn("timestamp");
		((IndexTreeInplaceAction)action).setSolrSettings(solrSettings);
		
		SolrSearchService solrSearchService = mock(SolrSearchService.class);
		BriefObjectMetadataBean metadata = new BriefObjectMetadataBean();
		metadata.setId("uuid:2");
		metadata.setAncestorPath(Arrays.asList("1,uuid:1,Collections"));
		
		when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(metadata);
		action.setSolrSearchService(solrSearchService);
		
		return action;
	}
}
