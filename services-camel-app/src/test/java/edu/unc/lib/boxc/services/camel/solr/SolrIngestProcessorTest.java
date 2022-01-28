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
package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.services.camel.solr.SolrIngestProcessor;

import java.util.UUID;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class SolrIngestProcessorTest {

    private static final String CONTENT_BASE_URI = "http://localhost:48085/rest";
    private static final String TEST_URI =
            "http://localhost:48085/rest/content/7c/73/29/6f/7c73296f-54ae-438e-b8d5-1890eba41676";

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
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private MessageSender messageSender;

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase(CONTENT_BASE_URI);
        initMocks(this);
        processor = new SolrIngestProcessor(dipFactory, pipeline, solrUpdateDriver, repoObjLoader);
        processor.setUpdateWorkSender(messageSender);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn(TEST_URI);

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
    public void testBinaryMessageUpdateAncestors() throws Exception {
        PID filePid = PIDs.get(TEST_URI);
        PID binaryPid = DatastreamPids.getOriginalFilePid(filePid);
        when(message.getHeader(eq(RESOURCE_TYPE))).thenReturn(Fcrepo4Repository.Binary.getURI());
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(binaryPid.getRepositoryPath());

        var targetFile = mock(FileObject.class);
        when(targetFile.getPid()).thenReturn(filePid);
        var parentWork = mock(WorkObject.class);
        var workPid = PIDs.get(UUID.randomUUID().toString());
        when(targetFile.getParent()).thenReturn(parentWork);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repoObjLoader.getFileObject(filePid)).thenReturn(targetFile);

        processor.process(exchange);

        verify(messageSender).sendMessage(workPid.getQualifiedId());
        // Regular indexing should also happen
        verify(pipeline).process(eq(dip));
        verify(solrUpdateDriver).addDocument(eq(docBean));
    }
}
