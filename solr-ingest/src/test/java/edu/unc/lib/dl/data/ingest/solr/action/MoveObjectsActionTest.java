package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class MoveObjectsActionTest extends Assert {

	@Mock
	private DocumentIndexingPackageFactory dipFactory;
	@Mock
	private DocumentIndexingPipeline pipeline;
	@Mock
	private TripleStoreQueryService tsqs;
	@Mock
	private SolrUpdateDriver driver;
	@Mock
	private Element mdContents;

	@Mock
	private DocumentIndexingPackage parentDip;

	private MoveObjectsAction action;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		initMocks(this);

		when(parentDip.getMdContents()).thenReturn(mdContents);

		when(dipFactory.createDocumentIndexingPackageWithMDContents(any(PID.class))).thenReturn(parentDip);

		when(tsqs.queryResourceIndex(anyString())).thenReturn(Arrays.asList(Arrays.asList("0")));

		// Perform action
		action = new MoveObjectsAction();
		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setDipFactory(dipFactory);
		action.setAddDocumentMode(false);
		action.init();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMove() throws Exception {

		when(parentDip.getDisplayOrder(anyString())).thenReturn(2L, 5L, 1L);

		DocumentIndexingPackage dipRoot = mock(DocumentIndexingPackage.class);
		IndexDocumentBean idbRoot = mock(IndexDocumentBean.class);
		when(dipRoot.getDocument()).thenReturn(idbRoot);
		List<PID> children1 = Arrays.asList(new PID("c1"), new PID("c2"), new PID("c4"));
		when(dipRoot.getChildren()).thenReturn(children1);

		Map<String, List<String>> triples = new HashMap<String, List<String>>();
		triples.put(ContentModelHelper.Relationship.contains.toString(), Arrays.asList("c3"));
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(triples, (Map<String, List<String>>) null);

		when(dipFactory.createDocumentIndexingPackageWithMDContents(any(PID.class))).thenReturn(parentDip);

		ChildSetRequest request = new ChildSetRequest("c0", Arrays.asList("c1", "c2"),
				IndexingActionType.MOVE);
		action.performAction(request);

		// Check that pipeline ran on the parent, 2 immediate children, and 1 nested child
		verify(pipeline, times(4)).process(any(DocumentIndexingPackage.class));

		verify(dipFactory).createDocumentIndexingPackageWithMDContents(any(PID.class));
		assertEquals(3, request.getChildrenProcessed());

		ArgumentCaptor<IndexDocumentBean> idbCaptor = ArgumentCaptor.forClass(IndexDocumentBean.class);
		verify(driver, times(3)).updateDocument(eq("set"), idbCaptor.capture());

		List<IndexDocumentBean> idbs = idbCaptor.getAllValues();

		assertEquals("Must be 3 index documents submitted", 3, idbs.size());

		assertEquals("c1", idbs.get(0).getId());
		assertEquals(2L, idbs.get(0).getDisplayOrder().longValue());
		assertEquals("c2", idbs.get(2).getId());
		assertEquals(5L, idbs.get(2).getDisplayOrder().longValue());
		assertEquals("c3", idbs.get(1).getId());
		assertNull("Display order should not have changed for child of child", idbs.get(1).getDisplayOrder());

	}

	@Test
	public void testMoveNoMdContents() throws Exception {

		parentDip.setMdContents(null);

		ChildSetRequest request = new ChildSetRequest("uuid:2", Arrays.asList("uuid:6", "uuid:7"),
				IndexingActionType.MOVE);
		action.performAction(request);

		verify(pipeline, times(3)).process(any(DocumentIndexingPackage.class));
		verify(dipFactory).createDocumentIndexingPackageWithMDContents(any(PID.class));
		assertEquals(2, request.getChildrenProcessed());

	}
}
