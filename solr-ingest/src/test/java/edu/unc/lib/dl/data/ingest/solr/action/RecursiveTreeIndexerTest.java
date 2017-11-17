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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author bbpennel
 *
 */
public class RecursiveTreeIndexerTest extends Assert {

    private RecursiveTreeIndexer indexer;

    @Mock
    private SolrUpdateDriver driver;
    @Mock
    private UpdateTreeAction action;
    @Mock
    private DocumentIndexingPipeline pipeline;

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private DocumentIndexingPackage parentDip;
    @Mock
    private SolrUpdateRequest request;

    @Mock
    private ContentContainerObject containerObj;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(action.getSolrUpdateDriver()).thenReturn(driver);
        when(action.getPipeline()).thenReturn(pipeline);

        containerObj = makeContainerObject();

        indexer = new RecursiveTreeIndexer(request, action, true);

        when(action.getDocumentIndexingPackage(any(PID.class), any(DocumentIndexingPackage.class)))
                .thenReturn(dip);
    }

    @Test
    public void indexNoDip() throws Exception {

        when(action.getDocumentIndexingPackage(any(PID.class), any(DocumentIndexingPackage.class))).thenReturn(null);

        indexer.index(containerObj, parentDip);

        verify(pipeline, never()).process(any(DocumentIndexingPackage.class));
        verify(driver, never()).addDocument(any(IndexDocumentBean.class));
        verify(request, never()).incrementChildrenProcessed();

    }

    @Test
    public void indexGetDipException() throws Exception {

        when(action.getDocumentIndexingPackage(any(PID.class), any(DocumentIndexingPackage.class)))
            .thenThrow(new IndexingException(""));

        indexer.index(containerObj, parentDip);

        verify(pipeline, never()).process(any(DocumentIndexingPackage.class));
        verify(driver, never()).addDocument(any(IndexDocumentBean.class));
        verify(request, never()).incrementChildrenProcessed();

    }

    @Test
    public void indexNoChildren() throws Exception {

        when(containerObj.getMembers()).thenReturn(Collections.emptyList());

        indexer.index(containerObj, parentDip);

        verify(pipeline).process(any(DocumentIndexingPackage.class));
        verify(driver).addDocument(any(IndexDocumentBean.class));
        verify(request).incrementChildrenProcessed();

    }

    @Test
    public void indexUpdate() throws Exception {

        indexer = new RecursiveTreeIndexer(request, action, false);

        when(action.getDocumentIndexingPackage(any(PID.class), any(DocumentIndexingPackage.class))).thenReturn(dip);

        indexer.index(containerObj, parentDip);

        verify(pipeline).process(eq(dip));
        verify(driver, never()).addDocument(any(IndexDocumentBean.class));
        verify(driver).updateDocument(any(IndexDocumentBean.class));
        verify(request).incrementChildrenProcessed();

    }

    @Test
    public void indexHierarchyTest() throws Exception {

        ContentContainerObject child1 = makeContainerObject();
        ContentContainerObject child2 = makeContainerObject();
        ContentContainerObject child3 = makeContainerObject();

        addMembers(containerObj, child1, child2);
        addMembers(child1, child3);

        indexer.index(containerObj, parentDip);

        verify(driver, times(4)).addDocument(any(IndexDocumentBean.class));
        verify(request, times(4)).incrementChildrenProcessed();
    }

    /**
     * Test that indexing continues after encountering an indexing exception
     *
     * @throws Exception
     */
    @Test
    public void testIndexingExceptionThrown() throws Exception {
        ContentContainerObject child1 = makeContainerObject();
        ContentContainerObject child2 = makeContainerObject();
        when(action.getDocumentIndexingPackage(eq(child2.getPid()), any(DocumentIndexingPackage.class)))
                .thenReturn(null);
        ContentContainerObject child3 = makeContainerObject();

        addMembers(containerObj, child1, child2);
        addMembers(child1, child3);

        indexer.index(containerObj, parentDip);

        // All objects, including the children of c2 should have been retrieved
        verify(action, times(4)).getDocumentIndexingPackage(any(PID.class),
                any(DocumentIndexingPackage.class));
        // All objects except c2 should have been added to driver
        verify(driver, times(3)).addDocument(any(IndexDocumentBean.class));
        // Count should only be increment for objects that successfully indexed
        verify(request, times(3)).incrementChildrenProcessed();
    }

    /**
     * Test that processing ends when an unexpected exception is encountered
     */
    @Test(expected = IndexingException.class)
    public void indexUnexpectedException() throws Exception {
        ContentContainerObject child1 = makeContainerObject();
        ContentContainerObject child2 = makeContainerObject();

        addMembers(containerObj, child1, child2);

        // Fail on the second object
        doNothing().doThrow(new RuntimeException()).when(pipeline).process(any(DocumentIndexingPackage.class));

        try {
            indexer.index(containerObj, parentDip);
        } finally {
            verify(driver, times(1)).addDocument(any(IndexDocumentBean.class));
        }

    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private ContentContainerObject makeContainerObject() {
        PID pid = makePid();
        ContentContainerObject container = mock(ContentContainerObject.class);
        when(container.getPid()).thenReturn(pid);
        return container;
    }

    private void addMembers(ContentContainerObject container, ContentObject... children) {
        when(container.getMembers()).thenReturn(Arrays.asList(children));
    }
}
