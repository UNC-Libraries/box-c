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

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.facets.CaseInsensitiveFacet;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.facets.MultivaluedHierarchicalFacet;
import edu.unc.lib.boxc.search.solr.facets.RoleGroupFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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
    // Sort types, which are groupings of any number of field names with matching sort orders.
    private static final Map<String, List<SortField>> SORT_TYPES = Map.of(
            "bestMatch", constructSortFields("SCORE|desc,TITLE|asc,LABEL|asc"),
            "collection", constructSortFields(
                    "ANCESTOR_IDS|asc,IDENTIFIER_SORT|asc,DISPLAY_ORDER|asc,TITLE|asc,LABEL|asc"),
            "creator", constructSortFields("CREATOR_SORT|asc"),
            "dateAdded", constructSortFields("DATE_ADDED|desc"),
            "dateCreated", constructSortFields("DATE_CREATED|desc"),
            "dateUpdated", constructSortFields("DATE_UPDATED|desc"),
            "default", constructSortFields(
                    "SCORE|desc,RESOURCE_TYPE_SORT|asc,IDENTIFIER_SORT|asc,TITLE|asc,LABEL|asc"),
            "resourceType", constructSortFields("RESOURCE_TYPE_SORT|asc"),
            "title", constructSortFields("TITLE_LC|asc,ID|asc")
    );

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
            SearchFieldKey.DATE_CREATED.name(), SearchFieldKey.DATE_ADDED.name(), SearchFieldKey.DATE_UPDATED.name(),
            SearchFieldKey.DATE_CREATED_YEAR.name());
    // Set of fields which should be treated as dates in search criteria
    public static final Set<String> FIELDS_DATE_SEARCHABLE = FIELDS_RANGE_SEARCHABLE;

    // Classes for facet fields. If not specified, then it is a GenericFacet
    private static final Map<String, Class<?>> FACET_CLASS_MAP = Map.of(
            SearchFieldKey.ANCESTOR_PATH.name(), CutoffFacetImpl.class,
            SearchFieldKey.CONTENT_TYPE.name(), MultivaluedHierarchicalFacet.class,
            SearchFieldKey.DEPARTMENT.name(), CaseInsensitiveFacet.class,
            SearchFieldKey.DEPARTMENT_LC.name(), CaseInsensitiveFacet.class,
            SearchFieldKey.ROLE_GROUP.name(), RoleGroupFacet.class
    );

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

        searchableFields = new HashSet<>();
        searchFieldParams = new HashMap<>();
        searchFieldKeys = new HashMap<>();
        searchFieldLabels = new HashMap<>();

        // Query validation properties
        setDefaultPerPage(Integer.parseInt(properties.getProperty("search.results.defaultPerPage", "20")));
        setMaxPerPage(Integer.parseInt(properties.getProperty("search.results.maxPerPage", "40")));
        setMaxBrowsePerPage(Integer.parseInt(properties.getProperty("search.results.maxBrowsePerPage", "100")));
        setPagesToDisplay(Integer.parseInt(properties.getProperty("search.results.pagesToDisplay", "10")));
        setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "7")));

        setStructuredDepthDefault(Integer.parseInt(properties.getProperty("search.structure.depth.default", "1")));
        setStructuredDepthMax(Integer.parseInt(properties.getProperty("search.structure.depth.max", "4")));

        // Facet properties
        setFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.facetsPerGroup", "6")));
        setExpandedFacetsPerGroup(Integer.parseInt(
                properties.getProperty("search.facet.expandedFacetsPerGroup", "15")));
        setMaxFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.maxFacetsPerGroup", "15")));
        populateCollectionFromProperty("search.facet.fields", facetNames, properties, ",");
        populateCollectionFromProperty("search.facet.defaultSearch", searchFacetNames, properties, ",");
        facetNames = Collections.unmodifiableList(facetNames);
        searchFacetNames = Collections.unmodifiableList(searchFacetNames);

        // Field names
        populateCollectionFromProperty("search.field.searchable", searchableFields, properties, ",");
        searchableFields = Collections.unmodifiableSet(searchableFields);

        searchFieldParams = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::name, SearchFieldKey::getUrlParam));
        searchFieldKeys = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::getUrlParam, SearchFieldKey::name));
        searchFieldLabels = Arrays.stream(SearchFieldKey.values())
                .collect(Collectors.toMap(SearchFieldKey::name, SearchFieldKey::getDisplayLabel));

        // Access field names
        this.setAllowPatronAccess(new Boolean(properties.getProperty("search.access.allowPatrons", "false")));

        // Resource Types, stored here temporarily for usage in jsp
        resourceTypeFile = ResourceType.File.name();
        resourceTypeAggregate = ResourceType.Work.name();
        resourceTypeFolder = ResourceType.Folder.name();
        resourceTypeCollection = ResourceType.Collection.name();
        resourceTypeUnit = ResourceType.AdminUnit.name();
        resourceTypeContentRoot = ResourceType.ContentRoot.name();
    }

    private static List<SortField> constructSortFields(String value) {
        return Arrays.stream(value.split(",")).map(SortField::new).collect(Collectors.toList());
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
        return actionKey == null ? null : this.actions.get(actionKey);
    }

    public int getMaxFacetsPerGroup() {
        return maxFacetsPerGroup;
    }

    public void setMaxFacetsPerGroup(int maxFacetsPerGroup) {
        this.maxFacetsPerGroup = maxFacetsPerGroup;
    }

    /**
     * @param fieldKey Field key, must not be null
     * @return Get the facet class associated with the provided key. Defaults to GenericFacet.
     */
    public static Class<?> getFacetClass(String fieldKey) {
        if (fieldKey == null) {
            throw new IllegalArgumentException("Cannot get facet class for null field key");
        }
        var fClass = FACET_CLASS_MAP.get(fieldKey);
        return fClass == null ? GenericFacet.class : fClass;
    }

    /**
     * @param sortKey
     * @return Get list of SortField types for the provided sort type key
     */
    public static List<SortField> getSortFields(String sortKey) {
        return sortKey == null ? null : SORT_TYPES.get(sortKey);
    }

    /**
     * Storage class for holding sort field and direction pairs.
     *
     * @author bbpennel
     *
     */
    public static class SortField {
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

    public String getResourceTypeFile() {
        return resourceTypeFile;
    }

    public String getResourceTypeAggregate() {
        return resourceTypeAggregate;
    }

    public String getResourceTypeFolder() {
        return resourceTypeFolder;
    }

    public String getResourceTypeCollection() {
        return resourceTypeCollection;
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

    @Override
    public String toString() {
        return "";
    }
}
