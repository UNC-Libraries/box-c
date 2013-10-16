package edu.unc.lib.dl.data.ingest.solr.filter.collection;

import edu.unc.lib.dl.data.ingest.solr.filter.AbstractIndexDocumentFilter;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;

public abstract class CollectionSupplementalInformationFilter extends AbstractIndexDocumentFilter {
	public boolean isApplicable(DocumentIndexingPackage dip) {
		return false;
	}
}
