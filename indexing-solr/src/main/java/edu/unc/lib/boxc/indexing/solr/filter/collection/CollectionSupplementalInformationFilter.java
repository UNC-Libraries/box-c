package edu.unc.lib.boxc.indexing.solr.filter.collection;

import edu.unc.lib.boxc.indexing.solr.filter.IndexDocumentFilter;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class CollectionSupplementalInformationFilter implements IndexDocumentFilter  {
    public boolean isApplicable(DocumentIndexingPackage dip) {
        return false;
    }
}
