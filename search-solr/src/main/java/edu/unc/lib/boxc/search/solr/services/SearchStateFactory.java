package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.InvalidFacetException;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.ranges.RangeValue;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.ranges.RangePair;
import edu.unc.lib.boxc.search.solr.ranges.UnknownRange;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.model.api.ResourceType.AdminUnit;
import static edu.unc.lib.boxc.model.api.ResourceType.Collection;
import static edu.unc.lib.boxc.model.api.ResourceType.Folder;

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
        searchState.setResourceTypes(new ArrayList<>(SearchSettings.DEFAULT_RESOURCE_TYPES));
        searchState.setSearchTermOperator(SearchSettings.DEFAULT_OPERATOR);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setFacetsToRetrieve(new ArrayList<>(searchSettings.searchFacetNames));
        searchState.setStartRow(0);
        searchState.setSortType("default");
        searchState.setSortNormalOrder(true);
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
        resultFields.add(SearchFieldKey.ID.name());
        searchState.setResultFields(resultFields);

        searchState.setSearchTermOperator(SearchSettings.DEFAULT_OPERATOR);
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
        searchState.getResultFields().add(SearchFieldKey.TITLE.name());
        return searchState;
    }

    /**
     * Returns a search state for results listing the containers within a hierarchy.
     * @return
     */
    public SearchState createHierarchyListSearchState() {
        SearchState searchState = createIDSearchState();
        searchState.setResultFields(new ArrayList<>(SearchSettings.RESULT_FIELDS_STRUCTURE));

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
        searchState.setResultFields(new ArrayList<>(SearchSettings.RESULT_FIELDS_STRUCTURE));
        searchState.setResourceTypes(new ArrayList<>(SearchSettings.DEFAULT_RESOURCE_TYPES));
        searchState.setSearchTermOperator(SearchSettings.DEFAULT_OPERATOR);
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
        searchState.setResultFields(new ArrayList<>(SearchSettings.RESULT_FIELDS_STRUCTURE));
        searchState.setBaseFacetLimit(searchSettings.facetsPerGroup);
        searchState.setResourceTypes(new ArrayList<>(SearchSettings.DEFAULT_RESOURCE_TYPES));
        searchState.setSearchTermOperator(SearchSettings.DEFAULT_OPERATOR);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);

        searchState.setSortType("collection");
        searchState.setSortNormalOrder(true);

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
     * @param facetSort
     * @param maxResults
     * @return
     */
    public SearchState createFacetSearchState(String facetField, String facetSort, int maxResults) {
        SearchState searchState = new SearchState();

        searchState.setResourceTypes(new ArrayList<>(SearchSettings.DEFAULT_RESOURCE_TYPES));
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
        var searchFields = searchState.getSearchFields();
        var rangeFields = searchState.getRangeFields();
        var facetFields = searchState.getFacets();

        Iterator<Entry<String, String[]>> paramIt = request.entrySet().iterator();
        while (paramIt.hasNext()) {
            Entry<String, String[]> param = paramIt.next();
            String[] paramValue = param.getValue();
            if (paramValue == null || paramValue.length == 0) {
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
            } else if (SearchSettings.FIELDS_RANGE_SEARCHABLE.contains(key)) {
                addRangeField(rangeFields, key, value);
            } else if (searchSettings.facetNames.contains(key)) {
                try {
                    List<SearchFacet> facetValues = Arrays.stream(value.split("\\s*\\|\\|\\s*"))
                            .map(v -> facetFieldFactory.createFacet(key, v))
                            .collect(Collectors.toList());
                    facetFields.put(key, facetValues);
                } catch (InvalidFacetException e) {
                    log.debug("Invalid facet " + key + " with value " + value, e);
                }
            }
        }
    }

    private void addRangeField(Map<String, RangeValue> rangeFields, String key, String value) {
        try {
            RangeValue rangeVal;
            if (UnknownRange.isUnknown(value)) {
                rangeVal = new UnknownRange();
            } else {
                rangeVal = new RangePair(value);
            }
            rangeFields.put(key, rangeVal);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            //An invalid range was specified, throw away the term pair
            log.debug("Invalid ranged pair {}: {}", key, value, e);
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
        String parameter = getParameter(request, SearchSettings.URL_PARAM_FACET_LIMIT_FIELDS);
        if (parameter != null) {
            String parameterArray[] = parameter.split("\\|");
            for (String parameterPair: parameterArray) {
                String parameterPairArray[] = parameterPair.split(":", 2);
                if (parameterPairArray.length > 1) {
                    try {
                        var fieldKey = searchSettings.searchFieldKey(parameterPairArray[0]);
                        if (fieldKey == null) {
                            log.warn("Unknown facet limit field key: {}", parameterPairArray[0]);
                            continue;
                        }
                        facetFieldUtil.setFacetLimit(fieldKey, Integer.parseInt(parameterPairArray[1]), searchState);
                    } catch (IllegalArgumentException | InvalidFacetException e) {
                        log.warn("Failed to add facet limit {} to field {}: {}", parameterPairArray[0],
                                parameterPairArray[1], e.getMessage());
                        log.debug("Exception from invalid facet limit", e);
                    }
                }
            }
        }

        //Set the base facet limit if one is provided
        parameter = getParameter(request, SearchSettings.URL_PARAM_BASE_FACET_LIMIT);
        if (parameter != null) {
            try {
                searchState.setBaseFacetLimit(Integer.parseInt(parameter));
            } catch (Exception e) {
                log.error("Failed to parse base facet limit: " + parameter);
            }
        }

        //Determine resource types selected
        parameter = getParameter(request, SearchSettings.URL_PARAM_RESOURCE_TYPES);
        var resourceTypes = new ArrayList<String>();
        if (parameter == null) {
            //If resource types aren't specified, load the defaults.
            resourceTypes.addAll(SearchSettings.DEFAULT_RESOURCE_TYPES);
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
        parameter = getParameter(request, SearchSettings.URL_PARAM_SEARCH_TERM_OPERATOR);
        if (parameter == null) {
            //If no operator set, use the default.
            searchState.setSearchTermOperator(SearchSettings.DEFAULT_OPERATOR);
        } else {
            searchState.setSearchTermOperator(parameter);
        }

        //Get Start row
        int startRow = 0;
        try {
            startRow = Integer.parseInt(getParameter(request, SearchSettings.URL_PARAM_START_ROW));
        } catch (Exception e) {
        }
        searchState.setStartRow(startRow);

        //Get number of rows per page
        int rowsPerPage = 0;
        try {
            rowsPerPage = Integer.parseInt(getParameter(request, SearchSettings.URL_PARAM_ROWS_PER_PAGE));
        } catch (Exception e) {
            // If not specified or invalid, then use default page size
            rowsPerPage = searchSettings.defaultPerPage;
        }
        searchState.setRowsPerPage(rowsPerPage);

        //Set sort
        parameter = getParameter(request, SearchSettings.URL_PARAM_SORT_TYPE);
        if (parameter != null) {
            String[] sortParts = parameter.split(",");
            if (sortParts.length > 0) {
                searchState.setSortType(sortParts[0]);
                if (sortParts.length == 2) {
                    searchState.setSortNormalOrder(!sortParts[1].equals(SearchSettings.SORT_ORDER_REVERSED));
                }
            }
        }

        //facetsToRetrieve
        parameter = getParameter(request, SearchSettings.URL_PARAM_FACET_FIELDS_TO_RETRIEVE);
        ArrayList<String> facetsToRetrieve = new ArrayList<>();
        if (parameter != null) {
            String facetArray[] = parameter.split(",");
            for (String facet : facetArray) {
                String facetKey = searchSettings.searchFieldKey(facet);
                if (facetKey != null && searchSettings.getFacetNames().contains(facetKey)) {
                    facetsToRetrieve.add(searchSettings.searchFieldKey(facet));
                }
            }
            searchState.setFacetsToRetrieve(facetsToRetrieve);
        }

        parameter = getParameter(request, SearchSettings.URL_PARAM_ROLLUP);
        if (parameter == null) {
            searchState.setRollup(null);
        } else {
            Boolean rollup = Boolean.valueOf(parameter);
            searchState.setRollup(rollup);
        }
    }

    /**
     * Populates a search state according to parameters expected from an advanced search request.
     * @param searchState
     * @param request
     */
    private void populateSearchStateAdvancedSearch(SearchState searchState, Map<String,String[]> request) {
        String parameter = getParameter(request, SearchFieldKey.DEFAULT_INDEX.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKey.DEFAULT_INDEX.name(), parameter);
        }

        parameter = getParameter(request, SearchFieldKey.SUBJECT_INDEX.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKey.SUBJECT_INDEX.name(), parameter);
        }

        parameter = getParameter(request, SearchFieldKey.CONTRIBUTOR_INDEX.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKey.CONTRIBUTOR_INDEX.name(), parameter);
        }

        parameter = getParameter(request, SearchFieldKey.TITLE_INDEX.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKey.TITLE_INDEX.name(), parameter);
        }

        parameter = getParameter(request, SearchFieldKey.PARENT_COLLECTION.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.setFacet(new GenericFacet(SearchFieldKey.PARENT_COLLECTION, parameter));
        }

        parameter = getParameter(request, SearchFieldKey.FILE_FORMAT_CATEGORY.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            var fileFormatCat = new GenericFacet(SearchFieldKey.FILE_FORMAT_CATEGORY.name(), parameter);
            searchState.addFacet(fileFormatCat);
        }

        parameter = getParameter(request, SearchFieldKey.COLLECTION_ID.getUrlParam());
        if (parameter != null && parameter.length() > 0) {
            searchState.getSearchFields().put(SearchFieldKey.COLLECTION_ID.name(), parameter);
        }

        //Store date added.
        RangePair dateAdded = new RangePair();
        parameter = getParameter(request, SearchFieldKey.DATE_ADDED.getUrlParam() + "Start");
        if (parameter != null && parameter.length() > 0) {
            dateAdded.setLeftHand(parameter);
        }

        parameter = getParameter(request, SearchFieldKey.DATE_ADDED.getUrlParam() + "End");
        if (parameter != null && parameter.length() > 0) {
            dateAdded.setRightHand(parameter);
        }

        if (dateAdded.getLeftHand() != null || dateAdded.getRightHand() != null) {
            searchState.getRangeFields().put(SearchFieldKey.DATE_ADDED.name(), dateAdded);
        }

        //Store date added.
        RangePair dateCreated = new RangePair();
        parameter = getParameter(request, SearchFieldKey.DATE_CREATED_YEAR.getUrlParam() + "Start");
        if (parameter != null && parameter.length() > 0) {
            dateCreated.setLeftHand(parameter);
        }

        parameter = getParameter(request, SearchFieldKey.DATE_CREATED_YEAR.getUrlParam() + "End");
        if (parameter != null && parameter.length() > 0) {
            dateCreated.setRightHand(parameter);
        }

        if (dateCreated.getLeftHand() != null || dateCreated.getRightHand() != null) {
            searchState.getRangeFields().put(SearchFieldKey.DATE_CREATED_YEAR.name(), dateCreated);
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

    public void setFacetFieldUtil(FacetFieldUtil facetFieldUtil) {
        this.facetFieldUtil = facetFieldUtil;
    }
}
