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
package edu.unc.lib.dl.search.solr.service;

import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldList;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * Query service which fills in missing titles for parent collection facet
 * @author bbpennel
 */
public class ParentCollectionFacetTitleService {

    private ObjectPathFactory pathFactory;

    /**
     * Populate displayValues (titles) for the parent collection facet if present
     * @param facetFields facet field list to add titles to
     */
    public void populateTitles(FacetFieldList facetFields) {
        if (facetFields == null || !facetFields.hasFacet(SearchFieldKeys.PARENT_COLLECTION.name())) {
            return;
        }

        FacetFieldObject parentCollectionFacet = facetFields.get(SearchFieldKeys.PARENT_COLLECTION.name());

        if (parentCollectionFacet != null) {
            for (SearchFacet searchFacet : parentCollectionFacet.getValues()) {
                GenericFacet pidFacet = (GenericFacet) searchFacet;
                String parentName = pathFactory.getName(pidFacet.getSearchValue());

                if (parentName != null) {
                    pidFacet.setFieldName(SearchFieldKeys.PARENT_COLLECTION.name());
                    pidFacet.setDisplayValue(parentName);
                }
            }
        }
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}
