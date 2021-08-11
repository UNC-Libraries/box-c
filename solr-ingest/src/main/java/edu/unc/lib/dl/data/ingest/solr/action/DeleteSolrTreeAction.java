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

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;

/**
 *
 * @author bbpennel
 *
 */
public class DeleteSolrTreeAction extends AbstractIndexingAction {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteSolrTreeAction.class);

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        // If the collections root is being deleted, then delete everything
        if (getContentRootPid().equals(updateRequest.getPid())) {
            LOG.debug("Delete Solr Tree, targeting all object.");
            solrUpdateDriver.deleteByQuery("*:*");
            return;
        }

        ContentObjectRecord ancestorPathBean = getRootAncestorPath(updateRequest);
        if (ancestorPathBean == null) {
            LOG.debug("Root object " + updateRequest.getTargetID() + " was not found while attempting to delete tree.");
            return;
        }

        // Determine if the starting node is a container.
        if (isContainer(ancestorPathBean)) {
            // Deleting a container, so perform a full path delete.

            // Delete the container itself
            solrUpdateDriver.deleteByQuery(
                    solrSettings.getFieldName(SearchFieldKey.ID.name()) + ":"
                            + SolrSettings.sanitize(updateRequest.getTargetID()));

            // Delete the containers contents
            solrUpdateDriver.deleteByQuery(
                    solrSettings.getFieldName(SearchFieldKey.ANCESTOR_PATH.name())
                            + ":" + SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue()));
        } else {
            // Targeting an individual file, just delete it.
            solrUpdateDriver.delete(updateRequest.getTargetID());
        }
    }

    private boolean isContainer(ContentObjectRecord mdObj) {
        String resourceType = mdObj.getResourceType();
        return ResourceType.Collection.equals(resourceType)
                || ResourceType.AdminUnit.equals(resourceType)
                || ResourceType.Folder.equals(resourceType)
                || ResourceType.Work.equals(resourceType)
                || ResourceType.ContentRoot.equals(resourceType);
    }

}
