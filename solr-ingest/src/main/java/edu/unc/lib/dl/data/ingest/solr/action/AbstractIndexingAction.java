package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

public abstract class AbstractIndexingAction implements IndexingAction {
	protected DocumentIndexingPipeline pipeline;
	protected SolrUpdateService solrUpdateService;
	protected DocumentIndexingPackageFactory dipFactory;
	protected SolrUpdateDriver solrUpdateDriver;
	protected SolrSearchService solrSearchService;
	protected PID collectionsPid;
	protected SearchSettings searchSettings;
	protected AccessGroupSet accessGroups;
	
	public static final String TARGET_ALL = "fullIndex";

	/**
	 * Gets the ancestor path facet value for
	 * 
	 * @param updateRequest
	 * @return
	 */
	protected BriefObjectMetadataBean getRootAncestorPath(SolrUpdateRequest updateRequest) {
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID.name());
		resultFields.add(SearchFieldKeys.ANCESTOR_PATH.name());
		resultFields.add(SearchFieldKeys.RESOURCE_TYPE.name());

		SimpleIdRequest idRequest = new SimpleIdRequest(updateRequest.getTargetID(), resultFields,
				accessGroups);

		try {
			return solrSearchService.getObjectById(idRequest);
		} catch (Exception e) {
			throw new IndexingException("Failed to retrieve ancestors for " + updateRequest.getTargetID(), e);
		}
	}
	
	public void setPipeline(DocumentIndexingPipeline pipeline) {
		this.pipeline = pipeline;
	}

	public void setSolrUpdateService(SolrUpdateService solrUpdateService) {
		this.solrUpdateService = solrUpdateService;
	}

	public void setDipFactory(DocumentIndexingPackageFactory dipFactory) {
		this.dipFactory = dipFactory;
	}

	public void setSolrUpdateDriver(SolrUpdateDriver solrUpdateDriver) {
		this.solrUpdateDriver = solrUpdateDriver;
	}

	public void setSolrSearchService(SolrSearchService solrSearchService) {
		this.solrSearchService = solrSearchService;
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public void setAccessGroups(AccessGroupSet accessGroups) {
		this.accessGroups = accessGroups;
	}
}
