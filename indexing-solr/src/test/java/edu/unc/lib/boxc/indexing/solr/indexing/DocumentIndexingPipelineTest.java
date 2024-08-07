package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.filter.IndexDocumentFilter;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPipelineTest extends Assertions {

    private DocumentIndexingPipeline pipeline;
    private AutoCloseable closeable;

    @Mock
    private DocumentIndexingPackage dip;

    private List<IndexDocumentFilter> filters;
    @Mock
    private IndexDocumentFilter mockFilter1;
    @Mock
    private IndexDocumentFilter mockFilter2;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        filters = Arrays.asList(mockFilter1, mockFilter2);

        pipeline = new DocumentIndexingPipeline();
        pipeline.setFilters(filters);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testProcessFilters() throws Exception {
        pipeline.process(dip);

        verify(mockFilter1).filter(dip);
        verify(mockFilter2).filter(dip);
    }

    @Test
    public void testProcessIndexingException() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            doThrow(new IndexingException("")).when(mockFilter2).filter(dip);

            pipeline.process(dip);
        });
    }
}
