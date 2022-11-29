package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.filter.IndexDocumentFilter;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;

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
