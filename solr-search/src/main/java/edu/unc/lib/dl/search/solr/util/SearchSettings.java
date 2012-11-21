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
package edu.unc.lib.dl.search.solr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	// Upper limit to the number of characters allowed in a single query field.
	public int queryMaxLength;
	// Default operator for looking up keyword terms.
	public String defaultOperator;
	// Set of possible boolean operators for looking up keyword terms.
	public HashSet<String> operators;
	// Default number of rows to return for a search request.
	public int defaultPerPage;
	// Default number of entries to return for a collection browse request.
	public int defaultCollectionsPerPage;
	// Default number of entries to retrieve for embedded/widget result lists
	public int defaultListResultsPerPage;
	// Upper limit to the number of results allowed to be returned in a single search request.
	public int maxPerPage;
	// Upper limit to the number of page navigation links to display at a time.
	public int pagesToDisplay;
	// Max number of neighbor items to display in the neighbor view
	public int maxNeighborResults;
	// Values for allow depths for structure browse
	public int structuredDepthMax;
	public int structuredDepthDefault;
	// Search field parameter names as they appear in GET requests to controllers
	public HashMap<String, String> searchFieldParams;
	// Search field display labels
	public HashMap<String, String> searchFieldLabels;
	// Fields which are allowed to be directly queried via keyword searches
	public HashSet<String> searchableFields;
	// Fields which should be searched as ranged queries.
	public HashSet<String> rangeSearchableFields;
	// Fields which are searched as date fields
	public HashSet<String> dateSearchableFields;
	// Fields which are filterable as facets
	public HashSet<String> facetNames;
	// Fields which are filterable as facets in a collection browse
	public HashSet<String> collectionBrowseFacetNames;
	// Fields which are filterable as facets in a structure browse
	public HashSet<String> facetNamesStructureBrowse;
	// Classes for facet fields. If not specified, then it is a GenericFacet
	public Map<String, Class<?>> facetClasses;
	// Order which facet fields should be displayed in facet lists.
	public List<String> facetDisplayOrder;
	// Delimiter which separates the components of a hierarchical facet in string form (such as tier, search value).
	public String facetSubfieldDelimiter;
	// Delimiter string which separates tiers of a hierarchical facet from each other.
	public String facetTierDelimiter;
	// Default number of facets entries to return for a single facet field result set.
	public int facetsPerGroup;
	// Default number of facets entries to return for a single facet field result set.
	public int expandedFacetsPerGroup;
	// Max number of facet entries that can be returned for a single facet field result set.
	public int maxFacetsPerGroup;
	// Indicates whether to limit search results to only those with administrative viewing privileges
	public Boolean allowPatronAccess;
	// Fields which should be treated as access filters
	public HashSet<String> accessFields;
	// Access filter fields which users are allowed to filter by.
	public HashSet<String> accessFilterableFields;
	// Set of accepted resource types/content models that results can be limited to.
	public HashSet<String> resourceTypes;
	// Search manipulation related actions
	public HashMap<String, String> actions;
	// Request parameter names for parameters used to construct search states.
	public HashMap<String, String> searchStateParams;
	// Resource/content model constants
	public String resourceTypeFile;
	public String resourceTypeAggregate;
	public String resourceTypeFolder;
	public String resourceTypeCollection;
	// Default set of resource types to retrieve in a search request
	public List<String> defaultResourceTypes;
	// Default set of resources types to retrieve in collection browse requests.
	public List<String> defaultCollectionResourceTypes;
	// Sort types, which are groupings of any number of field names with matching sort orders.
	public HashMap<String, List<SortField>> sortTypes;
	// Display names for sort types.
	public HashMap<String, String> sortDisplayNames;
	// Order in which sort names should be displayed to the user.
	public List<String> sortDisplayOrder;
	// Sort direction constants
	public String sortReverse;
	public String sortNormal;

	public SearchSettings() {
	}

	/**
	 * Retrieves and stores all search related constants from the provided properties file
	 * 
	 * @param properties
	 */
	@Autowired(required = true)
	public void setProperties(Properties properties) {
		facetDisplayOrder = new ArrayList<String>();
		facetNames = new HashSet<String>();
		collectionBrowseFacetNames = new HashSet<String>();
		facetNamesStructureBrowse = new HashSet<String>();
		this.facetClasses = new HashMap<String, Class<?>>();
		operators = new HashSet<String>();

		searchableFields = new HashSet<String>();
		rangeSearchableFields = new HashSet<String>();
		searchFieldParams = new HashMap<String, String>();
		searchFieldLabels = new HashMap<String, String>();
		dateSearchableFields = new HashSet<String>();

		actions = new HashMap<String, String>();
		searchStateParams = new HashMap<String, String>();

		resourceTypes = new HashSet<String>();
		defaultResourceTypes = new ArrayList<String>();
		defaultCollectionResourceTypes = new ArrayList<String>();

		sortTypes = new HashMap<String, List<SortField>>();
		sortDisplayNames = new HashMap<String, String>();
		sortDisplayOrder = new ArrayList<String>();

		accessFields = new HashSet<String>();
		accessFilterableFields = new HashSet<String>();

		// Query validation properties
		setQueryMaxLength(Integer.parseInt(properties.getProperty("search.query.maxLength", "255")));
		setDefaultOperator(properties.getProperty("search.query.defaultOperator", ""));
		populateCollectionFromProperty("search.query.operators", operators, properties, ",");
		setDefaultPerPage(Integer.parseInt(properties.getProperty("search.results.defaultPerPage", "0")));
		setDefaultCollectionsPerPage(Integer.parseInt(properties.getProperty("search.results.defaultCollectionsPerPage",
				"0")));
		setDefaultListResultsPerPage(Integer.parseInt(properties.getProperty("search.results.defaultListResultsPerPage",
				"0")));
		setMaxPerPage(Integer.parseInt(properties.getProperty("search.results.maxPerPage", "0")));
		setPagesToDisplay(Integer.parseInt(properties.getProperty("search.results.pagesToDisplay", "0")));
		setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));

		setStructuredDepthDefault(Integer.parseInt(properties.getProperty("search.structure.depth.default", "0")));
		setStructuredDepthMax(Integer.parseInt(properties.getProperty("search.structure.depth.max", "0")));

		setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));
		setMaxNeighborResults(Integer.parseInt(properties.getProperty("search.results.neighborItems", "0")));

		// Facet properties
		setFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.facetsPerGroup", "0")));
		setExpandedFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.expandedFacetsPerGroup", "0")));
		setMaxFacetsPerGroup(Integer.parseInt(properties.getProperty("search.facet.maxFacetsPerGroup", "0")));
		populateCollectionFromProperty("search.facet.fields", facetNames, properties, ",");
		populateCollectionFromProperty("search.facet.defaultCollectionBrowse", collectionBrowseFacetNames, properties,
				",");
		populateCollectionFromProperty("search.facet.defaultStructureBrowse", facetNamesStructureBrowse, properties, ",");
		populateCollectionFromProperty("search.facet.displayOrder", facetDisplayOrder, properties, ",");
		try {
			populateClassMapFromProperty("search.facet.class.", "edu.unc.lib.dl.search.solr.model.", this.facetClasses, properties);
		} catch (ClassNotFoundException e) {
			log.error("Invalid facet class specified in search.facet.class property", e);
		}
		setFacetSubfieldDelimiter(properties.getProperty("search.facet.subfieldDelimiter", ""));
		setFacetTierDelimiter(properties.getProperty("search.facet.tierDelimiter", ""));

		// Field names
		populateCollectionFromProperty("search.field.searchable", searchableFields, properties, ",");
		populateCollectionFromProperty("search.field.rangeSearchable", rangeSearchableFields, properties, ",");
		populateCollectionFromProperty("search.field.dateSearchable", dateSearchableFields, properties, ",");
		populateMapFromProperty("search.field.paramName.", searchFieldParams, properties);
		populateMapFromProperty("search.field.display.", searchFieldLabels, properties);
		populateMapFromProperty("search.actions.", actions, properties);
		populateMapFromProperty("search.url.param.", searchStateParams, properties);

		// Populate sort types
		setSortReverse(properties.getProperty("search.sort.order.reverse", ""));
		setSortNormal(properties.getProperty("search.sort.order.normal", ""));
		populateMapFromProperty("search.sort.name.", sortDisplayNames, properties);
		populateCollectionFromProperty("search.sort.displayOrder", sortDisplayOrder, properties, "\\|");

		// Access field names
		this.setAllowPatronAccess(new Boolean(properties.getProperty("search.access.allowPatrons", "true")));
		populateCollectionFromProperty("search.access.fields", accessFields, properties, ",");
		populateCollectionFromProperty("search.access.filterableFields", accessFilterableFields, properties, ",");

		// Resource Types
		setResourceTypeFile(properties.getProperty("search.resource.type.file", ""));
		setResourceTypeAggregate(properties.getProperty("search.resource.type.aggregate", ""));
		setResourceTypeFolder(properties.getProperty("search.resource.type.folder", ""));
		setResourceTypeCollection(properties.getProperty("search.resource.type.collection", ""));
		populateCollectionFromProperty("search.resource.types", resourceTypes, properties, ",");
		populateCollectionFromProperty("search.resource.searchDefault", defaultResourceTypes, properties, ",");
		populateCollectionFromProperty("search.resource.collectionDefault", defaultCollectionResourceTypes, properties,
				",");

		Iterator<Map.Entry<Object, Object>> propIt = properties.entrySet().iterator();
		while (propIt.hasNext()) {
			Map.Entry<Object, Object> propEntry = propIt.next();
			String propertyKey = (String) propEntry.getKey();

			// Populate sort types
			if (propertyKey.indexOf("search.sort.type.") == 0) {
				String sortTypes[] = ((String) propEntry.getValue()).split(",");
				List<SortField> sortFields = new ArrayList<SortField>();
				for (String sortField : sortTypes) {
					sortFields.add(new SortField(sortField));
				}
				this.sortTypes.put(propertyKey.substring(propertyKey.lastIndexOf(".") + 1), sortFields);
			}
		}
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

	public int getQueryMaxLength() {
		return queryMaxLength;
	}

	public void setQueryMaxLength(int queryMaxLength) {
		this.queryMaxLength = queryMaxLength;
	}

	public int getDefaultPerPage() {
		return defaultPerPage;
	}

	public void setDefaultPerPage(int defaultPerPage) {
		this.defaultPerPage = defaultPerPage;
	}

	public int getDefaultCollectionsPerPage() {
		return defaultCollectionsPerPage;
	}

	public void setDefaultCollectionsPerPage(int defaultCollectionsPerPage) {
		this.defaultCollectionsPerPage = defaultCollectionsPerPage;
	}

	public int getMaxPerPage() {
		return maxPerPage;
	}

	public void setMaxPerPage(int maxPerPage) {
		this.maxPerPage = maxPerPage;
	}

	public HashSet<String> getSearchableFields() {
		return searchableFields;
	}

	public void setSearchableFields(HashSet<String> searchableFields) {
		this.searchableFields = searchableFields;
	}

	public HashSet<String> getFacetNames() {
		return facetNames;
	}

	public void setFacetNames(HashSet<String> facetNames) {
		this.facetNames = facetNames;
	}

	public HashSet<String> getFacetNamesStructureBrowse() {
		return facetNamesStructureBrowse;
	}

	public void setFacetNamesStructureBrowse(HashSet<String> facetNamesStructureBrowse) {
		this.facetNamesStructureBrowse = facetNamesStructureBrowse;
	}

	public List<String> getFacetDisplayOrder() {
		return facetDisplayOrder;
	}

	public void setFacetDisplayOrder(List<String> facetDisplayOrder) {
		this.facetDisplayOrder = facetDisplayOrder;
	}

	public HashMap<String, List<SortField>> getSortTypes() {
		return sortTypes;
	}

	public void setSortTypes(HashMap<String, List<SortField>> sortTypes) {
		this.sortTypes = sortTypes;
	}

	public String getSortReverse() {
		return sortReverse;
	}

	public void setSortReverse(String sortReverse) {
		this.sortReverse = sortReverse;
	}

	public String getSortNormal() {
		return sortNormal;
	}

	public void setSortNormal(String sortNormal) {
		this.sortNormal = sortNormal;
	}

	public Map<String, Class<?>> getFacetClasses() {
		return facetClasses;
	}

	public void setFacetClasses(Map<String, Class<?>> facetClasses) {
		this.facetClasses = facetClasses;
	}

	public HashSet<String> getRangeSearchableFields() {
		return rangeSearchableFields;
	}

	public void setRangeSearchableFields(HashSet<String> rangeSearchableFields) {
		this.rangeSearchableFields = rangeSearchableFields;
	}

	public Boolean getAllowPatronAccess() {
		return allowPatronAccess;
	}

	public void setAllowPatronAccess(Boolean allowPatronAccess) {
		this.allowPatronAccess = allowPatronAccess;
	}

	public HashSet<String> getAccessFields() {
		return accessFields;
	}

	public void setAccessFields(HashSet<String> accessFields) {
		this.accessFields = accessFields;
	}

	public HashSet<String> getAccessFilterableFields() {
		return accessFilterableFields;
	}

	public void setAccessFilterableFields(HashSet<String> accessFilterableFields) {
		this.accessFilterableFields = accessFilterableFields;
	}

	public boolean isResourceTypeContainer(String resourceType) {
		return (resourceTypeCollection.equals(resourceType) || resourceTypeFolder.equals(resourceType));
	}

	public HashSet<String> getResourceTypes() {
		return resourceTypes;
	}

	public void setResourceTypes(HashSet<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}

	public List<String> getDefaultResourceTypes() {
		return defaultResourceTypes;
	}

	public void setDefaultResourceTypes(List<String> defaultResourceTypes) {
		this.defaultResourceTypes = defaultResourceTypes;
	}

	public String getDefaultOperator() {
		return defaultOperator;
	}

	public void setDefaultOperator(String defaultOperator) {
		this.defaultOperator = defaultOperator;
	}

	public HashSet<String> getOperators() {
		return operators;
	}

	public void setOperators(HashSet<String> operators) {
		this.operators = operators;
	}

	public HashMap<String, String> getActions() {
		return actions;
	}

	public void setActions(HashMap<String, String> actions) {
		this.actions = actions;
	}

	public String actionName(String actionKey) {
		return this.actions.get(actionKey);
	}

	public String getActionName(String actionKey) {
		return this.actionName(actionKey);
	}

	public List<String> getDefaultCollectionResourceTypes() {
		return defaultCollectionResourceTypes;
	}

	public void setDefaultCollectionResourceTypes(List<String> defaultCollectionResourceTypes) {
		this.defaultCollectionResourceTypes = defaultCollectionResourceTypes;
	}

	public HashMap<String, String> getSearchStateParams() {
		return searchStateParams;
	}

	public void setSearchStateParams(HashMap<String, String> searchStateParams) {
		this.searchStateParams = searchStateParams;
	}

	public String searchStateParam(String key) {
		return this.searchStateParams.get(key);
	}

	public String getSearchStateParam(String key) {
		return this.searchStateParam(key);
	}

	public int getMaxFacetsPerGroup() {
		return maxFacetsPerGroup;
	}

	public void setMaxFacetsPerGroup(int maxFacetsPerGroup) {
		this.maxFacetsPerGroup = maxFacetsPerGroup;
	}

	public String getFacetSubfieldDelimiter() {
		return facetSubfieldDelimiter;
	}

	public void setFacetSubfieldDelimiter(String facetSubfieldDelimiter) {
		this.facetSubfieldDelimiter = facetSubfieldDelimiter;
	}

	public String getFacetTierDelimiter() {
		return facetTierDelimiter;
	}

	public void setFacetTierDelimiter(String facetTierDelimiter) {
		this.facetTierDelimiter = facetTierDelimiter;
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
		return getKey(searchFieldParams, name);
	}

	public String getSearchFieldKey(String name) {
		return searchFieldKey(name);
	}

	public HashMap<String, String> getSearchFieldParams() {
		return searchFieldParams;
	}

	public void setSearchFieldParams(HashMap<String, String> searchFieldParams) {
		this.searchFieldParams = searchFieldParams;
	}

	public HashMap<String, String> getSearchFieldLabels() {
		return searchFieldLabels;
	}

	public void setSearchFieldLabels(HashMap<String, String> searchFieldLabels) {
		this.searchFieldLabels = searchFieldLabels;
	}

	public int getPagesToDisplay() {
		return pagesToDisplay;
	}

	public void setPagesToDisplay(int pagesToDisplay) {
		this.pagesToDisplay = pagesToDisplay;
	}

	public HashMap<String, String> getSortDisplayNames() {
		return sortDisplayNames;
	}

	public void setSortDisplayNames(HashMap<String, String> sortDisplayNames) {
		this.sortDisplayNames = sortDisplayNames;
	}

	public List<String> getSortDisplayOrder() {
		return sortDisplayOrder;
	}

	public void setSortDisplayOrder(List<String> sortDisplayOrder) {
		this.sortDisplayOrder = sortDisplayOrder;
	}

	public String getResourceTypeFile() {
		return resourceTypeFile;
	}

	public void setResourceTypeFile(String resourceTypeFile) {
		this.resourceTypeFile = resourceTypeFile;
	}

	public String getResourceTypeAggregate() {
		return resourceTypeAggregate;
	}

	public void setResourceTypeAggregate(String resourceTypeAggregate) {
		this.resourceTypeAggregate = resourceTypeAggregate;
	}

	public String getResourceTypeFolder() {
		return resourceTypeFolder;
	}

	public void setResourceTypeFolder(String resourceTypeFolder) {
		this.resourceTypeFolder = resourceTypeFolder;
	}

	public String getResourceTypeCollection() {
		return resourceTypeCollection;
	}

	public void setResourceTypeCollection(String resourceTypeCollection) {
		this.resourceTypeCollection = resourceTypeCollection;
	}

	public HashSet<String> getCollectionBrowseFacetNames() {
		return collectionBrowseFacetNames;
	}

	public void setCollectionBrowseFacetNames(HashSet<String> collectionBrowseFacetNames) {
		this.collectionBrowseFacetNames = collectionBrowseFacetNames;
	}

	public int getDefaultListResultsPerPage() {
		return defaultListResultsPerPage;
	}

	public void setDefaultListResultsPerPage(int defaultListResultsPerPage) {
		this.defaultListResultsPerPage = defaultListResultsPerPage;
	}

	public HashSet<String> getDateSearchableFields() {
		return dateSearchableFields;
	}

	public void setDateSearchableFields(HashSet<String> dateSearchableFields) {
		this.dateSearchableFields = dateSearchableFields;
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
		return "SearchSettings [queryMaxLength=" + queryMaxLength + ", defaultOperator=" + defaultOperator
				+ ", operators=" + operators + ", defaultPerPage=" + defaultPerPage + ", defaultCollectionsPerPage="
				+ defaultCollectionsPerPage + ", defaultListResultsPerPage=" + defaultListResultsPerPage + ", maxPerPage="
				+ maxPerPage + ", pagesToDisplay=" + pagesToDisplay + ", maxNeighborResults=" + maxNeighborResults
				+ ", searchFieldParams=" + searchFieldParams + ", searchFieldLabels=" + searchFieldLabels
				+ ", searchableFields=" + searchableFields + ", rangeSearchableFields=" + rangeSearchableFields
				+ ", dateSearchableFields=" + dateSearchableFields + ", facetNames=" + facetNames
				+ ", collectionBrowseFacetNames=" + collectionBrowseFacetNames + ", facetDisplayOrder=" + facetDisplayOrder
				+ ", facetSubfieldDelimiter=" + facetSubfieldDelimiter + ", facetTierDelimiter=" + facetTierDelimiter
				+ ", facetsPerGroup=" + facetsPerGroup + ", maxFacetsPerGroup=" + maxFacetsPerGroup + ", accessFields="
				+ accessFields + ", accessFilterableFields=" + accessFilterableFields + ", resourceTypes=" + resourceTypes
				+ ", actions=" + actions + ", searchStateParams=" + searchStateParams + ", resourceTypeFile="
				+ resourceTypeFile + ", resourceTypeFolder=" + resourceTypeFolder + ", resourceTypeCollection="
				+ resourceTypeCollection + ", defaultResourceTypes=" + defaultResourceTypes
				+ ", defaultCollectionResourceTypes=" + defaultCollectionResourceTypes + ", sortTypes=" + sortTypes
				+ ", sortDisplayNames=" + sortDisplayNames + ", sortDisplayOrder=" + sortDisplayOrder + ", sortReverse="
				+ sortReverse + ", sortNormal=" + sortNormal + "]";
	}
}
