package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateChildSetActionTest {

	@Mock
	private SolrUpdateService updateService;
	@Mock
	private SolrUpdateDriver driver;
	@Mock
	private DocumentIndexingPipeline pipeline;
	@Mock
	private TripleStoreQueryService tsqs;
	@Mock
	private DocumentIndexingPackageFactory dipFactory;

	private UpdateChildSetAction action;

	private ChildSetRequest request;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws SolrServerException, IOException {
		initMocks(this);

		when(tsqs.queryResourceIndex(anyString())).thenReturn(Arrays.asList(Arrays.asList("0")));

		action = new UpdateChildSetAction();
		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setSolrUpdateService(updateService);
		action.setDipFactory(dipFactory);
		action.setAddDocumentMode(false);
		action.setCollectionsPid(new PID("uuid:1"));
		action.init();
	}

	@Test(expected = IndexingException.class)
	public void testInvalidRequest() throws Exception {

		SolrUpdateRequest request = mock(SolrUpdateRequest.class);

		action.performAction(request);

	}

	@Test
	public void testPerformAction() throws Exception {

		DocumentIndexingPackage dipRoot = mock(DocumentIndexingPackage.class);
		IndexDocumentBean idbRoot = mock(IndexDocumentBean.class);
		when(dipRoot.getDocument()).thenReturn(idbRoot);
		List<PID> children1 = Arrays.asList(new PID("c1"), new PID("c2"), new PID("c4"));
		when(dipRoot.getChildren()).thenReturn(children1);

		DocumentIndexingPackage dipC1 = mock(DocumentIndexingPackage.class);
		DocumentIndexingPackage dipC2 = mock(DocumentIndexingPackage.class);
		List<PID> children2 = Arrays.asList(new PID("c3"));
		when(dipC2.getChildren()).thenReturn(children2);
		DocumentIndexingPackage dipC3 = mock(DocumentIndexingPackage.class);

		when(dipFactory.createDocumentIndexingPackage(any(PID.class))).thenReturn(dipRoot, dipC1, dipC2, dipC3);

		request = new ChildSetRequest("c0", Arrays.asList("c1", "c2"), IndexingActionType.ADD);

		action.performAction(request);

		// Only the two top level objects specified should have been looked up
		verify(tsqs, times(2)).queryResourceIndex(anyString());
		// DIPs for all objects except c4 should have been retrieved
		verify(dipFactory, times(4)).createDocumentIndexingPackage(any(PID.class));
		// All objects except c0 (the parent) should have been updated
		verify(driver, times(3)).updateDocument(eq("set"), any(IndexDocumentBean.class));
		verify(driver, never()).updateDocument(eq("set"), eq(idbRoot));

	}

	@Test
	public void testPerformActionNoRequestTargets() throws Exception {

		DocumentIndexingPackage dipRoot = mock(DocumentIndexingPackage.class);
		IndexDocumentBean idbRoot = mock(IndexDocumentBean.class);
		when(dipRoot.getDocument()).thenReturn(idbRoot);
		List<PID> children1 = Arrays.asList(new PID("c1"), new PID("c2"));
		when(dipRoot.getChildren()).thenReturn(children1);

		request = new ChildSetRequest("c0", new ArrayList<String>(), IndexingActionType.ADD);

		action.performAction(request);

		verify(tsqs, never()).queryResourceIndex(anyString());
		verify(dipFactory).createDocumentIndexingPackage(any(PID.class));
		// No updates should have occurred
		verify(driver, never()).updateDocument(eq("set"), any(IndexDocumentBean.class));

	}
}
