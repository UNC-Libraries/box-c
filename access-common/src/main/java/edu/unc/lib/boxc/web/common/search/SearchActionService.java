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
package edu.unc.lib.boxc.web.common.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

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
            if (actionName.equals(searchSettings.actionName("SET_FACET_LIMIT"))) {
                setFacetLimit(searchState, actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("REMOVE_FACET_LIMIT"))) {
                removeField(searchState.getFacetLimits(), actionValues);
                setStartRow(searchState, 0);
            } else if (actionName.equals(searchSettings.actionName("NEXT_PAGE"))) {
                nextPage(searchState);
            } else if (actionName.equals(searchSettings.actionName("PREVIOUS_PAGE"))) {
                previousPage(searchState);
            } else if (actionName.equals(searchSettings.actionName("SET_START_ROW"))) {
                setStartRow(searchState, actionValues);
            }
        }

        return searchState;
    }

    @SuppressWarnings("rawtypes")
    private void removeField(Map collection, String[] fieldList) {
        for (String fieldName : fieldList) {
            collection.remove(searchSettings.searchFieldKey(fieldName));
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

    public SearchSettings getSearchSettings() {
        return searchSettings;
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }
}
