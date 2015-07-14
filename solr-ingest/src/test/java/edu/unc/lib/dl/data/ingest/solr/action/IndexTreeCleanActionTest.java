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
package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author bbpennel
 * @date Mar 13, 2014
 */
public class IndexTreeCleanActionTest {

	@Mock
	private TripleStoreQueryService tsqs;
	@Mock
	private SolrUpdateDriver driver;
	@Mock
	private DocumentIndexingPipeline pipeline;
	@Mock
	private DeleteSolrTreeAction deleteAction;
	@Mock
	private SolrUpdateRequest request;
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	private DocumentIndexingPackageFactory factory;

	private IndexTreeCleanAction action;

	@Before
	public void setup() throws Exception {

		initMocks(this);

		when(request.getPid()).thenReturn(new PID("pid"));
		
		action = new IndexTreeCleanAction();
		action.setDeleteAction(deleteAction);
		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setCollectionsPid(new PID("uuid:1"));
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
		action.setFactory(factory);
		action.init();
	}

	@Test
	public void testPerformAction() throws Exception {

		action.performAction(request);

		verify(deleteAction).performAction(any(SolrUpdateRequest.class));
		verify(driver).commit();
		verify(driver).addDocument(any(IndexDocumentBean.class));
	}

	@Test
	public void testPerformActionChangeAddMode() throws Exception {

		action.setAddDocumentMode(false);

		action.performAction(request);

		// IndexTreeClean must be performed in addMode
		verify(driver).addDocument(any(IndexDocumentBean.class));
		verify(driver, never()).updateDocument(anyString(), any(IndexDocumentBean.class));
	}
}
