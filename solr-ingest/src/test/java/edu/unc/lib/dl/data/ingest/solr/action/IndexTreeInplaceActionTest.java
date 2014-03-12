package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.IndexingActionType;

public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
	private static final Logger log = LoggerFactory.getLogger(IndexTreeInplaceActionTest.class);

	@Mock
	private SearchSettings searchSettings;
	@Mock
	private SolrSettings solrSettings;
	@Mock
	private SolrSearchService solrSearchService;
	@Mock
	private BriefObjectMetadataBean metadata;
	@Mock
	CutoffFacet path;

	@Before
	@Override
	public void setup() throws SolrServerException, IOException {
		super.setup();

		when(solrSettings.getFieldName(eq(SearchFieldKeys.ANCESTOR_PATH.name()))).thenReturn("ancestorPath");
		when(solrSettings.getFieldName(eq(SearchFieldKeys.TIMESTAMP.name()))).thenReturn("timestamp");
		((IndexTreeInplaceAction) action).setSolrSettings(solrSettings);

		when(path.getSearchValue()).thenReturn("");
		when(metadata.getPath()).thenReturn(path);

		when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(metadata);
		action.setSolrSearchService(solrSearchService);
	}

	@Override
	protected UpdateTreeAction getAction() {
		return new IndexTreeInplaceAction();
	}

	@Test
	public void verifyOrphanCleanup() throws SolrServerException, IOException {

		when(metadata.getId()).thenReturn("uuid:2");
		when(metadata.getAncestorPath()).thenReturn(Arrays.asList("1,uuid:1,Collections"));
		when(path.getSearchValue()).thenReturn("2,uuid:2");

		SolrDocumentList docListBefore = getDocumentList();

		SolrUpdateRequest request = new SolrUpdateRequest("uuid:2", IndexingActionType.RECURSIVE_ADD);
		request.setStatus(ProcessingStatus.ACTIVE);

		action.performAction(request);
		server.commit();

		SolrDocumentList docListAfter = getDocumentList();

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

	@Test
	public void testIndexAll() throws SolrServerException, IOException {

		SolrDocumentList docListBefore = getDocumentList();

		SolrUpdateRequest request = new SolrUpdateRequest(UpdateTreeAction.TARGET_ALL,
				IndexingActionType.RECURSIVE_ADD);
		request.setStatus(ProcessingStatus.ACTIVE);

		action.performAction(request);
		server.commit();

		SolrDocumentList docListAfter = getDocumentList();

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

	@Test(expected = IndexingException.class)
	public void testNoAncestorBean() throws SolrServerException, IOException {

		when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(null);

		SolrUpdateRequest request = new SolrUpdateRequest("uuid:2", IndexingActionType.RECURSIVE_ADD);
		request.setStatus(ProcessingStatus.ACTIVE);

		action.performAction(request);
	}
}
