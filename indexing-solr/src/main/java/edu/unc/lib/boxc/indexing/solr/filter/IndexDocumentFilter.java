package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;

/**
 * 
 * @author bbpennel
 *
 */
public interface IndexDocumentFilter {
    public void filter(DocumentIndexingPackage dip) throws IndexingException;
}
