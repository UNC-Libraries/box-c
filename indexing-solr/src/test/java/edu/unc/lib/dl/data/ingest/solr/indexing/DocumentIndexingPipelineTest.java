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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.filter.IndexDocumentFilter;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPipelineTest extends Assert {

    private DocumentIndexingPipeline pipeline;

    @Mock
    private DocumentIndexingPackage dip;

    private List<IndexDocumentFilter> filters;
    @Mock
    private IndexDocumentFilter mockFilter1;
    @Mock
    private IndexDocumentFilter mockFilter2;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        filters = Arrays.asList(mockFilter1, mockFilter2);

        pipeline = new DocumentIndexingPipeline();
        pipeline.setFilters(filters);
    }

    @Test
    public void testProcessFilters() throws Exception {
        pipeline.process(dip);

        verify(mockFilter1).filter(dip);
        verify(mockFilter2).filter(dip);
    }

    @Test(expected = IndexingException.class)
    public void testProcessIndexingException() throws Exception {
        doThrow(new IndexingException("")).when(mockFilter2).filter(dip);

        pipeline.process(dip);
    }
}
