/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public abstract class AbstractIndexingAction implements IndexingAction {
	protected DocumentIndexingPipeline pipeline;
	protected SolrUpdateService solrUpdateService;
	protected DocumentIndexingPackageFactory dipFactory;
	protected SolrUpdateDriver solrUpdateDriver;
	protected SolrSearchService solrSearchService;
	protected PID collectionsPid;
	protected SearchSettings searchSettings;
	protected AccessGroupSet accessGroups;
	protected SolrSettings solrSettings;
	protected boolean addDocumentMode = true;
	
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

	public DocumentIndexingPipeline getPipeline() {
		return pipeline;
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
	
	public SolrUpdateDriver getSolrUpdateDriver() {
		return this.solrUpdateDriver;
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

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}

	public void setAccessGroups(AccessGroupSet accessGroups) {
		this.accessGroups = accessGroups;
	}
	
	public boolean isAddDocumentMode() {
		return addDocumentMode;
	}

	public void setAddDocumentMode(boolean addDocumentMode) {
		this.addDocumentMode = addDocumentMode;
	}
}
