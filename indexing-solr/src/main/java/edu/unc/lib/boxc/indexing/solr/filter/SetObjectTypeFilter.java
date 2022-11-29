package edu.unc.lib.boxc.indexing.solr.filter;

import java.util.List;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 * Sets the resource type and resource type sort order for the object being indexed
 *
 * @author bbpennel
 *
 */
public class SetObjectTypeFilter implements IndexDocumentFilter {

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        IndexDocumentBean idb = dip.getDocument();

        ContentObject contentObj = dip.getContentObject();
        List<String> types = contentObj.getTypes();

        ResourceType resourceType = ResourceType.getResourceTypeForUris(types);
        if (resourceType == null) {
            throw new IndexingException("Object " + dip.getPid()
                + " could not be indexed, it did not have a valid resource type");
        }

        idb.setResourceType(resourceType.name());
        idb.setResourceTypeSort(resourceType.getDisplayOrder());
    }

}
