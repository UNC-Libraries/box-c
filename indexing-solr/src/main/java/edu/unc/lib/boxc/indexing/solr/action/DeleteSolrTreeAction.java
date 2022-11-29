package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

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
                    SearchFieldKey.ID.getSolrField() + ":"
                            + SolrSettings.sanitize(updateRequest.getTargetID()));

            // Delete the containers contents
            solrUpdateDriver.deleteByQuery(
                    SearchFieldKey.ANCESTOR_PATH.getSolrField()
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
