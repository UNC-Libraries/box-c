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

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * Sets the status-tags field in the IndexDocumentBean
 * NB: This filter must be run *after* SetAccessStatusFilter and SetContentStatusFilter,
 * as it depends on their outputs
 *
 * @author harring
 *
 */
public class SetStatusTagsFilter implements IndexDocumentFilter {

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        IndexDocumentBean idb = dip.getDocument();
        idb.setStatusTags(determineStatusTags(idb));
    }

    private ArrayList<String> determineStatusTags(IndexDocumentBean idb) throws IndexingException {

        List<String> statusTags = new ArrayList<String>();
        List<String> contentStatus = idb.getContentStatus();
        List<String> accessStatus = idb.getStatus();

        if (contentStatus.contains(FacetConstants.UNPUBLISHED)) {
            statusTags.add(FacetConstants.UNPUBLISHED);
        }

        if (contentStatus.contains(FacetConstants.CONTENT_DESCRIBED)) {
            statusTags.add(FacetConstants.CONTENT_DESCRIBED);
        } else {
            statusTags.add(FacetConstants.CONTENT_NOT_DESCRIBED);
        }

        if (contentStatus.contains(FacetConstants.IS_PRIMARY_OBJECT)) {
            statusTags.add(FacetConstants.IS_PRIMARY_OBJECT);
        }

        if (accessStatus.contains(FacetConstants.EMBARGOED)) {
            statusTags.add(FacetConstants.EMBARGOED);
        }

        return (ArrayList<String>) statusTags;
    }

}
