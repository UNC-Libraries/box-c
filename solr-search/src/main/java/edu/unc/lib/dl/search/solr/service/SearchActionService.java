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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Service class which parses and performs any number of actions on a provided SearchState object.
 * @author bbpennel
 */
@Component
public class SearchActionService {
    private final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    private FacetFieldFactory facetFieldFactory;
    @Autowired
    private FacetFieldUtil facetFieldUtil;

    public SearchActionService() {
    }

    /**
     * Main action execution loop.  Performs a list of actions on a given search state.
     * @param searchState
     * @param parameters
     * @return
     */
    public SearchState executeActions(SearchState searchState, Map<String,String[]> parameters) {
        if (searchState == null || parameters == null || parameters.size() == 0) {
            return searchState;
        }

        for (Entry<String,String[]> parameter: parameters.entrySet()) {
            String actionName = parameter.getKey();
            String[] actionValues = parameter.getValue();
            if (actionName == null || actionValues == null) {
                continue;
            }
            int index = actionName.indexOf("a.");
            if (index != 0) {
                continue;
            }
            actionName = actionName.substring(index + 2);
            LOG.debug("Executing: " + actionName);
            if (actionName.equals(searchSettings.actionName("SET_FACET"))) {
                setFacet(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_FACET"))) {
                removeField(searchState.getFacets(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("SET_SEARCH_FIELD"))) {
                setField(searchState.getSearchFields(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("ADD_SEARCH_FIELD"))) {
                addField(searchState.getSearchFields(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_SEARCH_FIELD"))) {
                removeField(searchState.getSearchFields(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("SET_RANGE_FIELD"))) {
                setRangeFields(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_RANGE_FIELD"))) {
                removeField(searchState.getRangeFields(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("SET_FACET_LIMIT"))) {
                setFacetLimit(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_FACET_LIMIT"))) {
                removeField(searchState.getFacetLimits(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("SET_FACET_SELECT"))) {
                setFacetSelect(searchState, actionValues);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_FACET_SELECT"))) {
                removeFacetSelect(searchState);
            } else if (actionName.equals(searchSettings.actionName("NEXT_PAGE"))) {
                nextPage(searchState);
            } else if (actionName.equals(searchSettings.actionName("PREVIOUS_PAGE"))) {
                previousPage(searchState);
            } else if (actionName.equals(searchSettings.actionName("SET_START_ROW"))) {
                setStartRow(searchState, actionValues);
            } else if (actionName.equals(searchSettings.actionName("SET_ROWS_PER_PAGE"))) {
                setRow(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_ROWS_PER_PAGE"))) {
                removeRow(searchState);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("SET_RESOURCE_TYPE"))) {
                setResourceType(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_RESOURCE_TYPE"))) {
                removeResourceType(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("RESET_NAVIGATION"))) {
                resetNavigation(searchState, actionValues);
            }
        }

        return searchState;
    }

    private void setRangeFields(SearchState searchState, String[] values) {
        for (String value: values) {
            String[] valueArray = value.split(":", 2);
            if (valueArray.length != 2) {
                continue;
            }
            String key = searchSettings.searchFieldKey(valueArray[0]);
            if (key == null) {
                continue;
            }
            try {
                searchState.getRangeFields().put(key, new SearchState.RangePair(valueArray[1]));
            } catch (ArrayIndexOutOfBoundsException e) {
                LOG.debug("Invalid range field " + valueArray[1]);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setField(Map collection, String[] values) {
        for (String value: values) {
            String[] valueArray = value.split(":", 2);
            if (valueArray.length != 2) {
                continue;
            }
            String key = searchSettings.searchFieldKey(valueArray[0]);
            if (key == null) {
                continue;
            }
            collection.put(key, valueArray[1]);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addField(Map collection, String[] values) {
        for (String value: values) {
            String[] valueArray = value.split(":", 2);
            if (valueArray.length != 2) {
                continue;
            }
            String key = searchSettings.searchFieldKey(valueArray[0]);
            if (key == null) {
                continue;
            }
            String previousValue = (String)collection.get(key);
            if (previousValue == null) {
                previousValue = valueArray[1];
            } else {
                previousValue += " " + valueArray[1];
            }
            collection.put(key, previousValue);
        }
    }

    @SuppressWarnings("rawtypes")
    private void removeField(Map collection, String[] fieldList) {
        for (String fieldName : fieldList) {
            collection.remove(searchSettings.searchFieldKey(fieldName));
        }
    }

    private void setFacet(SearchState searchState, String[] values) {
        for (String value : values) {
            String[] valueArray = value.split(":", 2);
            if (valueArray.length != 2) {
                continue;
            }
            String key = searchSettings.searchFieldKey(valueArray[0]);
            if (key == null) {
                continue;
            }
            String fieldValue = valueArray[1];
            if (fieldValue == null || fieldValue.length() == 0) {
                continue;
            }
            searchState.getFacets().put(key, facetFieldFactory.createFacet(key, fieldValue));
        }
    }

    private void setFacetLimit(SearchState searchState, String[] values) {
        for (String value : values) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
                String[] valueArray = value.split(":", 2);
                String key = searchSettings.searchFieldKey(valueArray[0]);
                if (key == null) {
                    continue;
                }
                facetFieldUtil.setFacetLimit(key, Integer.parseInt(valueArray[1]), searchState);
            } catch (NumberFormatException e) {
                LOG.error("Failed to perform set facet limit action: " + value);
            } catch (UnsupportedEncodingException e) {
                LOG.error("Unsupported character encoding while setting facet limit", e);
            }
        }
    }

    private void setFacetSelect(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        searchState.setFacetsToRetrieve(new ArrayList<String>(Arrays.asList(values[0].split(","))));
    }

    private void removeFacetSelect(SearchState searchState) {
        searchState.setFacetsToRetrieve(null);
    }

    private void nextPage(SearchState searchState) {
        if (searchState.getRowsPerPage() == null) {
            return;
        }
        searchState.setStartRow(searchState.getStartRow() + searchState.getRowsPerPage());
    }

    private void previousPage(SearchState searchState) {
        if (searchState.getRowsPerPage() == null) {
            return;
        }
        searchState.setStartRow(searchState.getStartRow() - searchState.getRowsPerPage());
    }

    private void setStartRow(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        try {
            setStartRow(searchState, Integer.parseInt(values[0]));
        } catch (NumberFormatException e) {
            setStartRow(searchState, 0);
        }
    }

    private void setStartRow(SearchState searchState, int startRow) {
        searchState.setStartRow(startRow);
    }

    private void setRow(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        try {
            setRow(searchState, Integer.parseInt(values[0]));
        } catch (NumberFormatException e) {
            // Ignore invalid number
        }
    }

    private void setRow(SearchState searchState, int row) {
        searchState.setRowsPerPage(row);
    }

    private void removeRow(SearchState searchState) {
        searchState.setRowsPerPage(null);
    }

    private void setResourceType(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        String[] resourceTypes = values[0].split(",");
        searchState.setResourceTypes(new ArrayList<String>(Arrays.asList(resourceTypes)));
    }

    private void removeResourceType(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        String[] resourceTypes = values[0].split(",");
        searchState.getResourceTypes().removeAll(Arrays.asList(resourceTypes));
    }

    private void resetNavigation(SearchState searchState, String[] values) {
        if (values.length == 0) {
            return;
        }
        String mode = values[0];
        if (mode.equals("search")) {
            searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getFacetNames()));
            searchState.setRowsPerPage(searchSettings.defaultPerPage);
            searchState.setResourceTypes(null);
        } else if (mode.equals("collections")) {
            searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getCollectionBrowseFacetNames()));
            searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
            ArrayList<String> resourceTypes = new ArrayList<String>();
            resourceTypes.add(searchSettings.getResourceTypeCollection());
            searchState.setResourceTypes(resourceTypes);
        } else if (mode.equals("structure")) {
            searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getFacetNamesStructureBrowse()));
            searchState.setRowsPerPage(searchSettings.defaultPerPage);
            searchState.setResourceTypes(null);
        }
        searchState.setStartRow(0);
    }

    public SearchSettings getSearchSettings() {
        return searchSettings;
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    public void setFacetFieldFactory(FacetFieldFactory facetFieldFactory) {
        this.facetFieldFactory = facetFieldFactory;
    }
}
