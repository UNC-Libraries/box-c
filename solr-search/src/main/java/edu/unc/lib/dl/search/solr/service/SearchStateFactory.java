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

import static edu.unc.lib.dl.util.ResourceType.AdminUnit;
import static edu.unc.lib.dl.util.ResourceType.Collection;
import static edu.unc.lib.dl.util.ResourceType.Folder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.exception.InvalidFacetException;
import edu.unc.lib.dl.search.solr.model.CaseInsensitiveFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Factory which generates SearchState objects.
 * @author bbpennel
 */
public class SearchStateFactory {
    private static final Logger log = LoggerFactory.getLogger(SearchStateFactory.class);
    private SearchSettings searchSettings;
    @Autowired
    private FacetFieldFactory facetFieldFactory;
    @Autowired
    private FacetFieldUtil facetFieldUtil;

    public SearchStateFactory() {

    }

    /**
     * Creates and returns a SearchState object representing the default search state
     * for a blank search.
     * @return
     */
    public SearchState createSearchState() {
        SearchState searchState = new SearchState();

        searchState.setBaseFacetLimit(searchSettings.facetsPerGroup);
        searchState.setResourceTypes(searchSettings.defaultResourceTypes);
        searchState.setSearchTermOperator(searchSettings.defaultOperator);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setFacetsToRetrieve(new ArrayList<>(searchSettings.searchFacetNames));
        searchState.setStartRow(0);
        searchState.setSortType("default");
        searchState.setSortNormalOrder(true);
        return searchState;
    }

    /**
     * Creates and returns a SearchState object starting from the default options for a
     * collection browse search, and then populating it with the search state.
     * from the http request.
     * @param request
     * @return SearchState object containing the search state for a collection browse
     */
    public SearchState createCollectionBrowseSearchState(Map<String,String[]> request) {
        SearchState searchState = createSearchState();

        searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
        searchState.setResourceTypes(new ArrayList<>(searchSettings.defaultCollectionResourceTypes));
        searchState.setFacetsToRetrieve(new ArrayList<>(searchSettings.collectionBrowseFacetNames));

        CutoffFacet depthFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "1,*");
        depthFacet.setCutoff(2);
        searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), depthFacet);

        populateSearchState(searchState, request);
        return searchState;
    }

    /**
     * Creates and returns a SearchState object starting from the default options for a
     * normal search, and then populating it with the search state.
     * from the http request.
     * @param request
     * @return SearchState object containing the search state
     */
    public SearchState createSearchState(Map<String,String[]> request) {
        SearchState searchState = createSearchState();
        populateSearchState(searchState, request);

        return searchState;
    }

    /**
     * Returns a search state object for an advanced search request.
     * @param request
     * @return
     */
    public SearchState createSearchStateAdvancedSearch(Map<String,String[]> request) {
        SearchState searchState = createSearchState();
        populateSearchStateAdvancedSearch(searchState, request);

        return searchState;
    }

    /**
     * Returns a search state for a result set of only identifiers.
     * @return
     */
    public SearchState createIDSearchState() {
        SearchState searchState = new SearchState();

        List<String> resultFields = new ArrayList<>();
        resultFields.add(SearchFieldKeys.ID.name());
        searchState.setResultFields(resultFields);

        searchState.setSearchTermOperator(searchSettings.defaultOperator);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setFacetsToRetrieve(null);
        searchState.setStartRow(0);
        return searchState;
    }

    /**
     * Returns a search state for a result set of titles and identifiers.
     * @return
     */
    public SearchState createTitleListSearchState() {
        SearchState searchState = createIDSearchState();
        searchState.getResultFields().add(SearchFieldKeys.TITLE.name());
        return searchState;
    }

    /**
     * Returns a search state for results listing the containers within a hierarchy.
     * @return
     */
    public SearchState createHierarchyListSearchState() {
        SearchState searchState = createIDSearchState();
        searchState.setResultFields(new ArrayList<>(searchSettings.resultFields.get("structure")));

        List<String> containerTypes = new ArrayList<>();
        containerTypes.add(Collection.name());
        containerTypes.add(Folder.name());
        containerTypes.add(AdminUnit.name());
        searchState.setResourceTypes(containerTypes);

        searchState.setSortType("title");
        searchState.setSortNormalOrder(true);

        return searchState;
    }

    public SearchState createStructureBrowseSearchState() {
        SearchState searchState = new SearchState();
        searchState.setResultFields(new ArrayList<>(searchSettings.resultFields.get("structure")));
        searchState.setResourceTypes(searchSettings.defaultResourceTypes);
        searchState.setSearchTermOperator(searchSettings.defaultOperator);
        searchState.setRowsPerPage(0);
        searchState.setStartRow(0);

        searchState.setSortType("title");
        searchState.setSortNormalOrder(true);

        return searchState;
    }

    public SearchState createStructureBrowseSearchState(Map<String,String[]> request) {
        SearchState searchState = createStructureBrowseSearchState();
        populateSearchState(searchState, request);
        return searchState;
    }

    /**
     * Returns a search state representing the default navigation search state for a hierarchical
     * structure browse request.
     * @return
     */
    public SearchState createHierarchicalBrowseSearchState() {
        SearchState searchState = new SearchState();
        searchState.setResultFields(new ArrayList<>(searchSettings.resultFields.get("structure")));
        searchState.setBaseFacetLimit(searchSettings.facetsPerGroup);
        searchState.setResourceTypes(searchSettings.defaultResourceTypes);
        searchState.setSearchTermOperator(searchSettings.defaultOperator);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);

        searchState.setSortType("collection");
        searchState.setSortNormalOrder(true);

        searchState.setFacetsToRetrieve(new ArrayList<>(searchSettings.facetNamesStructureBrowse));

        return searchState;
    }

    /**
     * Returns a search state representing the navigation search state for a hierarchical structure
     * browse request with the users previously existing search state overlayed.
     * @param request
     * @return
     */
    public SearchState createHierarchicalBrowseSearchState(Map<String,String[]> request) {
        SearchState searchState = createHierarchicalBrowseSearchState();

        populateSearchState(searchState, request);

        return searchState;
    }

    /**
     * Returns a search state usable for looking up all facet values for the facet field
     * specified.  A base value may be given for the facet being queried, for use in
     * querying specific tiers in a hierarchical facet.
     * @param facetField
     * @param baseValue
     * @return
     */
    public SearchState createFacetSearchState(String facetField, String facetSort, int maxResults) {
        SearchState searchState = new SearchState();

        searchState.setResourceTypes(searchSettings.defaultResourceTypes);
        searchState.setRowsPerPage(0);
        searchState.setStartRow(0);

        ArrayList<String> facetList = new ArrayList<>();
        facetList.add(facetField);
        searchState.setFacetsToRetrieve(facetList);

        if (facetSort != null) {
            HashMap<String,String> facetSorts = new HashMap<>();
            facetSorts.put(facetField, facetSort);
            searchState.setFacetSorts(facetSorts);
        }

        searchState.setBaseFacetLimit(maxResults);

        return searchState;
    }

    public SearchState createFacetSearchState(String facetField, int maxResults) {
        return createFacetSearchState(facetField, null, maxResults);
    }

    private String getParameter(Map<String,String[]> request, String key) {
        String[] value = request.get(key);
        if (value != null) {
            return value[0];
        }
        return null;
    }

    private void populateQueryableFields(SearchState searchState, Map<String,String[]> request) {
        Map<String,String> searchFields = searchState.getSearchFields();
        Map<String,SearchState.RangePair> rangeFields = searchState.getRangeFields();
        Map<String,Object> facetFields = searchState.getFacets();

        Iterator<Entry<String, String[]>> paramIt = request.entrySet().iterator();
        while (paramIt.hasNext()) {
            Entry<String, String[]> param = paramIt.next();
            if (param.getValue().length == 0) {
                continue;
            }
            String key = searchSettings.searchFieldKey(param.getKey());
            if (key == null) {
                continue;
            }
            String value;
            try {
                value = URLDecoder.decode(param.getValue()[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                continue;
            }
            if (searchSettings.searchableFields.contains(key)) {
                searchFields.put(key, value);
            } else if (searchSettings.facetNames.contains(key)) {
                try {
                    GenericFacet facet = this.facetFieldFactory.createFacet(key, value);
                    facetFields.put(facet.getFieldName(), facet);
                } catch (InvalidFacetException e) {
                    log.debug("Invalid facet " + key + " with value " + value, e);
                }
            } else if (searchSettings.rangeSearchableFields.contains(key)) {
                try {
                    rangeFields.put(key, new SearchState.RangePair(value));
                } catch (ArrayIndexOutOfBoundsException e) {
                    //An invalid range was specified, throw away the term pair
                }
            }
        }
    }


    /**
     * Populates the attributes of the given SearchState object with search state
     * parameters retrieved from the request mapping.
     * @param searchState SearchState object to populate
     * @param request
     * @return SearchState object containing all the parameters representing the current
     * search state in the request.
     */
    private void populateSearchState(SearchState searchState, Map<String,String[]> request) {
        populateQueryableFields(searchState, request);

        //retrieve facet limits
        String parameter = getParameter(request, searchSettings.searchStateParam("FACET_LIMIT_FIELDS"));
        if (parameter != null) {
            String parameterArray[] = parameter.split("\\|");
            for (String parameterPair: parameterArray) {
                String parameterPairArray[] = parameterPair.split(":", 2);
                if (parameterPairArray.length > 1) {
                    try {
                        facetFieldUtil.setFacetLimit(searchSettings.searchFieldKey(parameterPairArray[0]),
                                Integer.parseInt(parameterPairArray[1]), searchState);
                    } catch (Exception e) {
                        log.warn("Failed to add facet limit {} to field {}", new Object[] { parameterPairArray[0],
                                parameterPairArray[1] }, e);
                    }
                }
            }
        }

        //Set the base facet limit if one is provided
        parameter = getParameter(request, searchSettings.searchStateParam("BASE_FACET_LIMIT"));
        if (parameter != null) {
            try {
                searchState.setBaseFacetLimit(Integer.parseInt(parameter));
            } catch (Exception e) {
                log.error("Failed to parse base facet limit: " + parameter);
            }
        }

        //Determine resource types selected
        parameter = getParameter(request, searchSettings.searchStateParam("RESOURCE_TYPES"));
        ArrayList<String> resourceTypes = new ArrayList<>();
        if (parameter == null) {
            //If resource types aren't specified, load the defaults.
            resourceTypes.addAll(searchSettings.defaultResourceTypes);
        } else {
            String resourceArray[] = parameter.split(",");
            for (String resourceType: resourceArray) {
                if (resourceType != null && resourceType.trim().length() > 0) {
                    resourceTypes.add(resourceType);
                }
            }
        }
        searchState.setResourceTypes(resourceTypes);

        //Get search term operator
        parameter = getParameter(request, searchSettings.searchStateParam("SEARCH_TERM_OPERATOR"));
        if (parameter == null) {
            //If no operator set, use the default.
            searchState.setSearchTermOperator(searchSettings.defaultOperator);
        } else {
            searchState.setSearchTermOperator(parameter);
        }

        //Get Start row
        int startRow = 0;
        try {
            startRow = Integer.parseInt(getParameter(request, searchSettings.searchStateParam("START_ROW")));
        } catch (Exception e) {
        }
        searchState.setStartRow(startRow);

        //Get number of rows per page
        int rowsPerPage = 0;
        try {
            rowsPerPage = Integer.parseInt(getParameter(request, searchSettings.searchStateParam("ROWS_PER_PAGE")));
        } catch (Exception e) {
            //If not specified, then get the appropriate default value based on search content types.
            if (!resourceTypes.contains("File") && resourceTypes.contains("Collection")) {
                rowsPerPage = searchSettings.defaultCollectionsPerPage;
            } else {
                rowsPerPage = searchSettings.defaultPerPage;
            }
        }
        searchState.setRowsPerPage(rowsPerPage);

        //Set sort
        parameter = getParameter(request, searchSettings.searchStateParam("SORT_TYPE"));
        if (parameter != null) {
            String[] sortParts = parameter.split(",");
            if (sortParts.length > 0) {
                searchState.setSortType(sortParts[0]);
                if (sortParts.length == 2) {
                    searchState.setSortNormalOrder(!sortParts[1].equals(searchSettings.sortReverse));
                }
            }
        }

        //facetsToRetrieve
        parameter = getParameter(request, searchSettings.searchStateParam("FACET_FIELDS_TO_RETRIEVE"));
        ArrayList<String> facetsToRetrieve = new ArrayList<>();
        if (parameter != null) {
            String facetArray[] = parameter.split(",");
            for (String facet: facetArray) {
                String facetKey = searchSettings.searchFieldKey(facet);
                if (facetKey != null && searchSettings.getFacetNames().contains(facetKey)) {
                    facetsToRetrieve.add(searchSettings.searchFieldKey(facet));
                }
            }
            searchState.setFacetsToRetrieve(facetsToRetrieve);
        }

        parameter = getParameter(request, searchSettings.searchStateParam("ROLLUP"));
        if (parameter == null) {
            searchState.setRollup(null);
        } else {
            Boolean rollup = new Boolean(parameter);
            searchState.setRollup(rollup);
        }
    }

    /**
     * Populates a search state according to parameters expected from an advanced search request.
     * @param searchState
     * @param request
     */
    private void populateSearchStateAdvancedSearch(SearchState searchState, Map<String,String[]> request) {
        String parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DEFAULT_INDEX.name()));
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKeys.DEFAULT_INDEX.name(), parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.SUBJECT_INDEX.name()));
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKeys.SUBJECT_INDEX.name(), parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.CONTRIBUTOR_INDEX.name()));
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKeys.CONTRIBUTOR_INDEX.name(), parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.TITLE_INDEX.name()));
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKeys.TITLE_INDEX.name(), parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.ANCESTOR_PATH.name()));
        if (parameter != null && parameter.length() > 0) {
            CutoffFacet hierFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), parameter);
            searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), hierFacet);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DEPARTMENT.name()));
        if (parameter != null && parameter.length() > 0) {
            CaseInsensitiveFacet facet = new CaseInsensitiveFacet(SearchFieldKeys.DEPARTMENT.name(), parameter);
            searchState.getFacets().put(SearchFieldKeys.DEPARTMENT.name(), facet);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.CONTENT_TYPE.name()));
        if (parameter != null && parameter.length() > 0) {
            MultivaluedHierarchicalFacet hierFacet = new MultivaluedHierarchicalFacet(
                    SearchFieldKeys.CONTENT_TYPE.name(), parameter);
            searchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE.name(), hierFacet);
        }

        //Store date added.
        SearchState.RangePair dateAdded = new SearchState.RangePair();
        parameter = getParameter(request, searchSettings.searchFieldParam(
                SearchFieldKeys.DATE_ADDED.name()) + "Start");
        if (parameter != null && parameter.length() > 0) {
            dateAdded.setLeftHand(parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(
                SearchFieldKeys.DATE_ADDED.name()) + "End");
        if (parameter != null && parameter.length() > 0) {
            dateAdded.setRightHand(parameter);
        }

        if (dateAdded.getLeftHand() != null || dateAdded.getRightHand() != null) {
            searchState.getRangeFields().put(SearchFieldKeys.DATE_ADDED.name(), dateAdded);
        }

        //Store date added.
        SearchState.RangePair dateCreated = new SearchState.RangePair();
        parameter = getParameter(request, searchSettings.searchFieldParam(
                SearchFieldKeys.DATE_CREATED.name()) + "Start");
        if (parameter != null && parameter.length() > 0) {
            dateCreated.setLeftHand(parameter);
        }

        parameter = getParameter(request, searchSettings.searchFieldParam(
                SearchFieldKeys.DATE_CREATED.name()) + "End");
        if (parameter != null && parameter.length() > 0) {
            dateCreated.setRightHand(parameter);
        }

        if (dateCreated.getLeftHand() != null || dateCreated.getRightHand() != null) {
            searchState.getRangeFields().put(SearchFieldKeys.DATE_CREATED.name(), dateCreated);
        }
    }

    public SearchSettings getSearchSettings() {
        return searchSettings;
    }

    @Autowired
    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    public void setFacetFieldFactory(FacetFieldFactory facetFieldFactory) {
        this.facetFieldFactory = facetFieldFactory;
    }
}
