package edu.unc.lib.dl.data.ingest.solr.filter;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;

public interface IndexDocumentFilter {
	public void filter(DocumentIndexingPackage dip) throws IndexingException;
}
