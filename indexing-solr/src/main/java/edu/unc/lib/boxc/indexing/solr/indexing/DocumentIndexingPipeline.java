package edu.unc.lib.boxc.indexing.solr.indexing;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.filter.IndexDocumentFilter;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPipeline implements DocumentFilteringPipeline {
    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPipeline.class);
    protected Collection<IndexDocumentFilter> filters;

    @Override
    public void process(DocumentIndexingPackage dip) throws IndexingException {

        for (IndexDocumentFilter filter : filters) {
            log.info("filter {} executed on pid {}", filter.getClass().getName(), dip.getPid());
            filter.filter(dip);
        }
    }

    @Override
    public void setFilters(List<IndexDocumentFilter> filters) {
        this.filters = filters;
    }
}