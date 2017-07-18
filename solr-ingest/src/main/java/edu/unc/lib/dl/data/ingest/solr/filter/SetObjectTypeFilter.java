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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.List;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ResourceType;

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
