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
package edu.unc.lib.boxc.indexing.solr.action;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;

/**
 *
 * @author bbpennel
 *
 */
public abstract class AbstractIndexingAction implements IndexingAction {
    protected DocumentIndexingPipeline pipeline;
    @Autowired
    protected SolrUpdateDriver solrUpdateDriver;
    @Autowired
    protected SolrSearchService solrSearchService;
    @Autowired
    protected SearchSettings searchSettings;
    protected AccessGroupSet accessGroups;
    @Autowired
    protected SolrSettings solrSettings;
    @Autowired
    protected DocumentIndexingPackageFactory factory;
    protected boolean addDocumentMode = true;

    public static final String TARGET_ALL = "fullIndex";

    /**
     * Gets the ancestor path facet value for
     *
     * @param updateRequest
     * @return
     */
    protected ContentObjectRecord getRootAncestorPath(SolrUpdateRequest updateRequest) throws IndexingException {
        List<String> resultFields = new ArrayList<>();
        resultFields.add(SearchFieldKey.ID.name());
        resultFields.add(SearchFieldKey.ANCESTOR_PATH.name());
        resultFields.add(SearchFieldKey.RESOURCE_TYPE.name());

        SimpleIdRequest idRequest = new SimpleIdRequest(updateRequest.getPid(), resultFields,
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

    public void setSolrUpdateDriver(SolrUpdateDriver solrUpdateDriver) {
        this.solrUpdateDriver = solrUpdateDriver;
    }

    public SolrUpdateDriver getSolrUpdateDriver() {
        return this.solrUpdateDriver;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
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

    public void setFactory(DocumentIndexingPackageFactory factory) {
        this.factory = factory;
    }

    public boolean isAddDocumentMode() {
        return addDocumentMode;
    }

    public void setAddDocumentMode(boolean addDocumentMode) {
        this.addDocumentMode = addDocumentMode;
    }

    public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent)
            throws IndexingException {
        return factory.createDip(pid, parent);
    }
}
