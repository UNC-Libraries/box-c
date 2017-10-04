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
package edu.unc.lib.cdr;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author bbpennel
 *
 */
public class SolrIngestProcessorTest {

    private SolrIngestProcessor processor;

    @Mock
    private DocumentIndexingPackageFactory dipFactory;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean docBean;
    @Mock
    private DocumentIndexingPipeline pipeline;
    @Mock
    private SolrUpdateDriver solrUpdateDriver;
    private int maxRetries = 3;
    private long retryDelay = 10;

    @Mock
    private Repository repository;
    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        initMocks(this);
        processor = new SolrIngestProcessor(dipFactory, pipeline, solrUpdateDriver, maxRetries, retryDelay);

        PIDs.setRepository(repository);
        when(repository.getBaseUri()).thenReturn("http://fedora");

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn("http://fedora/content/45/66/76/67/45667667-ed3f-41fc-94cc-7764fc266075");

        when(dip.getDocument()).thenReturn(docBean);
        when(dipFactory.createDip(any(PID.class))).thenReturn(dip);
    }

    @Test
    public void testIndexObject() throws Exception {
        processor.process(exchange);

        verify(pipeline).process(eq(dip));
        verify(solrUpdateDriver).addDocument(eq(docBean));
    }

    @Test(expected = IndexingException.class)
    public void testIndexingFailed() throws Exception {
        doThrow(new IndexingException("Fail")).when(pipeline).process(any(DocumentIndexingPackage.class));

        processor.process(exchange);
    }

    @Test
    public void testIndexRetry() throws Exception {
        doThrow(new IndexingException("Fail")).doNothing().when(pipeline).process(any(DocumentIndexingPackage.class));

        processor.process(exchange);

        verify(pipeline, times(2)).process(eq(dip));
        verify(solrUpdateDriver).addDocument(eq(docBean));
    }
}
