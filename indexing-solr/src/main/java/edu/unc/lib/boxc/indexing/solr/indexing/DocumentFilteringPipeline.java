package edu.unc.lib.boxc.indexing.solr.indexing;

import java.util.List;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.filter.IndexDocumentFilter;

/**
 * Pipeline class which interates through a collection of filters to modify the
 * document indexing package submitted to the process method.
 *
 * @author bbpennel
 *
 */
public interface DocumentFilteringPipeline {

    /**
     * Performs a series of filters to the provided document indexing package.
     *
     * @param dip Indexing package being modified
     * @throws IndexingException
     */
    public void process(DocumentIndexingPackage dip) throws IndexingException;

    public void setFilters(List<IndexDocumentFilter> filters);
}
