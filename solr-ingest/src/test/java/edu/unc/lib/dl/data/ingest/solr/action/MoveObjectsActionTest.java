package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

import static org.mockito.Mockito.*;

public class MoveObjectsActionTest extends Assert {

	@SuppressWarnings("unchecked")
	@Test
	public void testMove() throws Exception {
		DocumentIndexingPackageFactory dipFactory = mock(DocumentIndexingPackageFactory.class);
		when(dipFactory.createDocumentIndexingPackageWithMDContents(any(PID.class))).thenAnswer(
				new Answer<DocumentIndexingPackage>() {
					public DocumentIndexingPackage answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						PID pid = (PID) args[0];
						SAXBuilder builder = new SAXBuilder();
						Document document = builder.build(new FileInputStream(new File(
								"src/test/resources/foxml/mdContents.xml")));
						DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);
						dip.setMdContents(document.getRootElement());
						dip.getDocument().setAncestorPath(Arrays.asList("1,uuid:1,Collections"));
						return dip;
					}
				});

		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		when(tsqs.queryResourceIndex(anyString())).thenReturn(Arrays.asList(Arrays.asList("0")));

		DocumentIndexingPipeline pipeline = mock(DocumentIndexingPipeline.class);

		SolrUpdateDriver driver = mock(SolrUpdateDriver.class);

		// Perform action
		MoveObjectsAction action = new MoveObjectsAction();
		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setDipFactory(dipFactory);
		action.setAddDocumentMode(false);
		action.init();

		ChildSetRequest request = new ChildSetRequest("uuid:2", Arrays.asList("uuid:6", "uuid:7"),
				IndexingActionType.MOVE);
		action.performAction(request);

		verify(pipeline, times(3)).process(any(DocumentIndexingPackage.class));
		verify(dipFactory).createDocumentIndexingPackageWithMDContents(any(PID.class));
		assertEquals(2, request.getChildrenProcessed());
	}
}
