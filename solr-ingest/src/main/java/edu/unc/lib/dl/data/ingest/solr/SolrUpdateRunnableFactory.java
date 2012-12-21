package edu.unc.lib.dl.data.ingest.solr;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class SolrUpdateRunnableFactory {

	/**
	 * Creates a new SolrUpdateRunnable.  This method is overridden by spring.
	 * 
	 * @return
	 */
	public abstract SolrUpdateRunnable createJob();
}
