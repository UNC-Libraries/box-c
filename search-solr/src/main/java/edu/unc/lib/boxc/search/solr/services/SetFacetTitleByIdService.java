package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;

import java.util.Arrays;
import java.util.List;

/**
 * Query service which fills in missing titles for facets containing object ids
 * @author bbpennel
 */
public class SetFacetTitleByIdService {
    private static final List<String> APPLICABLE_FACET_NAMES = Arrays.asList(
            SearchFieldKey.PARENT_COLLECTION.name(), SearchFieldKey.PARENT_UNIT.name()
    );

    private ObjectPathFactory pathFactory;

    /**
     * Populate displayValues (titles) for facets in a SearchState if present
     * @param searchState
     */
    public void populateSearchState(SearchState searchState) {
        if (searchState.getFacets() == null || searchState.getFacets().isEmpty()) {
            return;
        }

        for (var facetName: APPLICABLE_FACET_NAMES) {
            var facetValues = searchState.getFacets().get(facetName);
            if (facetValues == null) {
                continue;
            }
            populateInList(facetName, facetValues);
        }
    }

    private void populateInList(String facetName, List<SearchFacet> facetList) {
        for (var facetValue: facetList) {
            GenericFacet pidFacet = (GenericFacet) facetValue;
            var facetTitle = pathFactory.getName(pidFacet.getSearchValue());

            if (facetTitle != null) {
                pidFacet.setFieldName(facetName);
                pidFacet.setDisplayValue(facetTitle);
            }
        }
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}
