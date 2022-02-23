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
package edu.unc.lib.boxc.search.solr.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stores properties related to searching retrieved from a properties file. Includes default values and lists of
 * possible field types for validation or lookup purposes.
 *
 * @author bbpennel
 */
public class SearchSettings extends AbstractSettings {
    private static final Logger log = LoggerFactory.getLogger(SearchSettings.class);

    public static final String DEFAULT_OPERATOR = "AND";
    public static final String SORT_ORDER_REVERSED = "reverse";
    public static final Collection<String> DEFAULT_RESOURCE_TYPES =
            Arrays.asList(ResourceType.File.name(), ResourceType.Work.name(), ResourceType.Folder.name(),
                    ResourceType.Collection.name(), ResourceType.AdminUnit.name());
    public static final Collection<String> DEFAULT_COLLECTION_RESOURCE_TYPES =
            Collections.singletonList(ResourceType.Collection.name());

    // Set of field keys to use in structure result sets
    public static final List<String> RESULT_FIELDS_STRUCTURE = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.RESOURCE_TYPE.name(), SearchFieldKey.ANCESTOR_PATH.name(),
            SearchFieldKey.PARENT_COLLECTION.name());

    // Set of fields which can be used in range search criteria
    public static final Set<String> FIELDS_RANGE_SEARCHABLE = Set.of(
            SearchFieldKey.DATE_CREATED.name(), SearchFieldKey.DATE_ADDED.name(), SearchFieldKey.DATE_UPDATED.name());
    // Set of fields which should be treated as dates in search criteria
    public static final Set<String> FIELDS_DATE_SEARCHABLE = FIELDS_RANGE_SEARCHABLE;

    public static final String URL_PARAM_FACET_LIMIT_FIELDS = "facetLimits";
    public static final String URL_PARAM_BASE_FACET_LIMIT = "facetLimit";
    public static final String URL_PARAM_START_ROW = "start";
    public static final String URL_PARAM_ROWS_PER_PAGE = "rows";
    public static final String URL_PARAM_SORT_TYPE = "sort";
    public static final String URL_PARAM_FACET_FIELDS_TO_RETRIEVE = "facetSelect";
    public static final String URL_PARAM_RESOURCE_TYPES = "types";
    public static final String URL_PARAM_SEARCH_TERM_OPERATOR = "operator";
    public static final String URL_PARAM_ROLLUP = "rollup";
    public static final String URL_PARAM_FIELDSET = "fieldSet";
    public static final String URL_PARAM_FIELDS = "fields";

    private Properties properties;
    // Default number of rows to return for a search request.
    public int defaultPerPage;
    // Upper limit to the number of results allowed to be returned in a single search request.
    public int maxPerPage;
    // Upper limit to the number of results allowed in a browse request.
    public int maxBrowsePerPage;
    // Upper limit to the number of page navigation links to display at a time.
    public int pagesToDisplay;
    // Max number of neighbor items to display in the neighbor view
    public int maxNeighborResults;
    // Values for allow depths for structure browse
    public int structuredDepthMax;
    public int structuredDepthDefault;
    // Search field parameter names as they appear in GET requests to controllers
    public Map<String, String> searchFieldParams;
    // Inverted field parameter names for easily getting keys by parameter name
    public Map<String, String> searchFieldKeys;
    // Search field display labels
    public Map<String, String> searchFieldLabels;
    // Fields which are allowed to be directly queried via keyword searches
    public Set<String> searchableFields;
    // Fields which are filterable as facets
    public List<String> facetNames;
    // Facets shown by default in normal search results
    public List<String> searchFacetNames;
    // Classes for facet fields. If not specified, then it is a GenericFacet
    public Map<String, Class<?>> facetClasses;
    // Default number of facets entries to return for a single facet field result set.
    public int facetsPerGroup;
    // Default number of facets entries to return for a single facet field result set.
    public int expandedFacetsPerGroup;
    // Max number of facet entries that can be returned for a single facet field result set.
    public int maxFacetsPerGroup;
    // Indicates whether to limit search results to only those with administrative viewing privileges
    public Boolean allowPatronAccess;
    // Search manipulation related actions, remove on
    public final Map<String, String> actions = Map.of("SET_FACET_LIMIT", "setFacetLimit",
            "REMOVE_FACET_LIMIT", "removeFacetLimit",
            "NEXT_PAGE", "nextPage",
            "PREVIOUS_PAGE", "previousPage",
            "SET_START_ROW", "setStartRow");

    // Resource/content model constants
    public String resourceTypeFile;
    public String resourceTypeAggregate;
    public String resourceTypeFolder;
    public String resourceTypeCollection;
    public String resourceTypeUnit;
    public String resourceTypeContentRoot;
    // Sort types, which are groupings of any number of field names with matching sort orders.
    public Map<String, List<SortField>> sortTypes;
    // Display names for sort types.
    public Map<String, String> sortDisplayNames;

    public SearchSettings() {
    }

    /**
     * Retrieves and stores all search related constants from the provided properties file
     *
     * @param properties
     */
    @Autowired(required = true)
    public void setProperties(Properties properties) {
        this.properties = properties;

        facetNames = new ArrayList<>();
        searchFacetNames = new ArrayList<>();
        this.facetClasses = new HashMap<>();

        searchableFields = new HashSet<>();
        searchFieldParams = new HashMap<>();
        searchFieldKeys = new HashMap<>();
        searchFieldLabels = new HashMap<>();

        sortTypes = new HashMap<>();
        sortDisplayNames = new HashMap<>();

        // Query validation properties
        setDefaultPerPage(Integer.parseInt(properties.getProperty("search.results.defaultPerPage", "0")));
        setMaxPerPage(Integer.parseInt(properties.getProperty("search.results.maxPerPage", "0")));
        setMaxBrowsePerPage(Integer.parseInt(properties.getProperty("search.results.maxBrowsePerPage", "100")));
        setPagesToDisplay(Integer.parseInt(properties.getProperty("search.results.pagesToDisplay", "0")));
        setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));

        setStructuredDepthDefault(Integer.parseInt(properties.getProperty("search.structure.depth.default", "1")));
        setStructuredDepthMax(Integer.parseInt(properties.getProperty("search.structure.depth.max", "4")));

        setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));
        setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));

        // Facet properties
        setFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.facetsPerGroup", "0")));
        setExpandedFacetsPerGroup(Integer.parseInt(
                properties.getProperty("search.facet.expandedFacetsPerGroup", "0")));
        setMaxFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.maxFacetsPerGroup", "0")));
        populateCollectionFromProperty("search.facet.fields", facetNames, properties, ",");
        populateCollectionFromProperty("search.facet.defaultSearch", searchFacetNames, properties, ",");
        facetNames = Collections.unmodifiableList(facetNames);
        searchFacetNames = Collections.unmodifiableList(searchFacetNames);
        try {
            populateClassMapFromProperty("search.facet.class.", "edu.unc.lib.boxc.search.solr.facets.",
                    this.facetClasses, properties);
        } catch (ClassNotFoundException e) {
            log.error("Invalid facet class specified in search.facet.class property", e);
        }

        // Field names
        populateCollectionFromProperty("search.field.searchable", searchableFields, properties, ",");
        searchableFields = Collections.unmodifiableSet(searchableFields);

        searchFieldParams = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::name, SearchFieldKey::getUrlParam));
        searchFieldKeys = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::getUrlParam, SearchFieldKey::name));
        searchFieldLabels = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::name, SearchFieldKey::getDisplayLabel));

        // Populate sort types
        populateMapFromProperty("search.sort.name.", sortDisplayNames, properties);

        // Access field names
        this.setAllowPatronAccess(new Boolean(properties.getProperty("search.access.allowPatrons", "true")));

        // Resource Types, stored here temporarily for usage in jsp
        resourceTypeFile = ResourceType.File.name();
        resourceTypeAggregate = ResourceType.Work.name();
        resourceTypeFolder = ResourceType.Folder.name();
        resourceTypeCollection = ResourceType.Collection.name();
        resourceTypeUnit = ResourceType.AdminUnit.name();
        resourceTypeContentRoot = ResourceType.ContentRoot.name();

        Iterator<Map.Entry<Object, Object>> propIt = properties.entrySet().iterator();
        while (propIt.hasNext()) {
            Map.Entry<Object, Object> propEntry = propIt.next();
            String propertyKey = (String) propEntry.getKey();

            // Populate sort types
            if (propertyKey.indexOf("search.sort.type.") == 0) {
                String sortTypes[] = ((String) propEntry.getValue()).split(",");
                List<SortField> sortFields = new ArrayList<>();
                for (String sortField : sortTypes) {
                    sortFields.add(new SortField(sortField));
                }
                this.sortTypes.put(propertyKey.substring(propertyKey.lastIndexOf(".") + 1), sortFields);
            }
        }
        sortTypes = Collections.unmodifiableMap(sortTypes);
    }

    public int getFacetsPerGroup() {
        return facetsPerGroup;
    }

    public void setFacetsPerGroup(int facetsPerGroup) {
        this.facetsPerGroup = facetsPerGroup;
    }

    public int getExpandedFacetsPerGroup() {
        return expandedFacetsPerGroup;
    }

    public void setExpandedFacetsPerGroup(int expandedFacetsPerGroup) {
        this.expandedFacetsPerGroup = expandedFacetsPerGroup;
    }

    public int getDefaultPerPage() {
        return defaultPerPage;
    }

    public void setDefaultPerPage(int defaultPerPage) {
        this.defaultPerPage = defaultPerPage;
    }

    public int getMaxPerPage() {
        return maxPerPage;
    }

    public void setMaxPerPage(int maxPerPage) {
        this.maxPerPage = maxPerPage;
    }

    public int getMaxBrowsePerPage() {
        return maxBrowsePerPage;
    }

    public void setMaxBrowsePerPage(int maxBrowsePerPage) {
        this.maxBrowsePerPage = maxBrowsePerPage;
    }

    public Set<String> getSearchableFields() {
        return searchableFields;
    }

    public void setSearchableFields(Set<String> searchableFields) {
        this.searchableFields = searchableFields;
    }

    public List<String> getFacetNames() {
        return facetNames;
    }

    public void setFacetNames(List<String> facetNames) {
        this.facetNames = facetNames;
    }

    public Map<String, List<SortField>> getSortTypes() {
        return sortTypes;
    }

    public void setSortTypes(Map<String, List<SortField>> sortTypes) {
        this.sortTypes = sortTypes;
    }

    public Map<String, Class<?>> getFacetClasses() {
        return facetClasses;
    }

    public void setFacetClasses(Map<String, Class<?>> facetClasses) {
        this.facetClasses = facetClasses;
    }

    public Boolean getAllowPatronAccess() {
        return allowPatronAccess;
    }

    public void setAllowPatronAccess(Boolean allowPatronAccess) {
        this.allowPatronAccess = allowPatronAccess;
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public String actionName(String actionKey) {
        return this.actions.get(actionKey);
    }

    public int getMaxFacetsPerGroup() {
        return maxFacetsPerGroup;
    }

    public void setMaxFacetsPerGroup(int maxFacetsPerGroup) {
        this.maxFacetsPerGroup = maxFacetsPerGroup;
    }

    /**
     * Storage class for holding sort field and direction pairs.
     *
     * @author bbpennel
     *
     */
    public class SortField {
        private String fieldName;
        private String sortOrder;

        public SortField(String sortField) {
            String sortValues[] = sortField.split("\\|");
            this.fieldName = sortValues[0];
            this.sortOrder = sortValues[1];
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    public String searchFieldParam(String key) {
        return searchFieldParams.get(key);
    }

    public String getSearchFieldParam(String key) {
        return this.searchFieldParam(key);
    }

    public String searchFieldKey(String name) {
        return searchFieldKeys.get(name);
    }

    public Map<String, String> getSearchFieldParams() {
        return searchFieldParams;
    }

    public void setSearchFieldParams(Map<String, String> searchFieldParams) {
        this.searchFieldParams = searchFieldParams;
    }

    public Map<String, String> getSearchFieldLabels() {
        return searchFieldLabels;
    }

    public void setSearchFieldLabels(Map<String, String> searchFieldLabels) {
        this.searchFieldLabels = searchFieldLabels;
    }

    public int getPagesToDisplay() {
        return pagesToDisplay;
    }

    public void setPagesToDisplay(int pagesToDisplay) {
        this.pagesToDisplay = pagesToDisplay;
    }

    public Map<String, String> getSortDisplayNames() {
        return sortDisplayNames;
    }

    public void setSortDisplayNames(Map<String, String> sortDisplayNames) {
        this.sortDisplayNames = sortDisplayNames;
    }

    public int getMaxNeighborResults() {
        return maxNeighborResults;
    }

    public void setMaxNeighborResults(int maxNeighborResults) {
        this.maxNeighborResults = maxNeighborResults;
    }

    public int getStructuredDepthMax() {
        return structuredDepthMax;
    }

    public void setStructuredDepthMax(int structuredDepthMax) {
        this.structuredDepthMax = structuredDepthMax;
    }

    public int getStructuredDepthDefault() {
        return structuredDepthDefault;
    }

    public void setStructuredDepthDefault(int structuredDepthDefault) {
        this.structuredDepthDefault = structuredDepthDefault;
    }

    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }

    @Override
    public String toString() {
        return "";
    }
}
