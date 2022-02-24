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
package edu.unc.lib.boxc.search.solr.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.api.exceptions.InvalidFacetException;
import edu.unc.lib.boxc.search.api.exceptions.InvalidHierarchicalFacetException;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;

/**
 * Factory for constructing SearchFacet and FacetField objects
 *
 * @author bbpennel
 *
 */
public class FacetFieldFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FacetFieldFactory.class);

    private SearchSettings searchSettings;
    private SolrSettings solrSettings;

    /**
     * Create an instance of a SearchFacet. The implementation is determined based off of the configured
     * class for the fieldKey
     * @param fieldKey
     * @param facetValue value of the facet
     * @return
     */
    public SearchFacet createFacet(String fieldKey, String facetValue) {
        Class<?> facetClass = SearchSettings.FACET_CLASS_MAP.get(fieldKey);
        if (facetClass == null) {
            facetClass = GenericFacet.class;
        }
        try {
            Constructor<?> constructor = facetClass.getConstructor(String.class, String.class);
            Object newFacet = constructor.newInstance(fieldKey, facetValue);
            return (GenericFacet) newFacet;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InvalidHierarchicalFacetException) {
                throw (InvalidHierarchicalFacetException) e.getCause();
            }
            throw new InvalidFacetException(
                    "An exception occurred while attempting to instantiate a new facet field object for "
                            + fieldKey + " " + facetValue, e);
        } catch (Exception e) {
            LOG.debug(e.getClass().getName());
            throw new InvalidFacetException(
                    "An exception occurred while attempting to instantiate a new facet field object for "
                            + fieldKey + " " + facetValue, e);
        }

    }

    /**
     * Instantiates a new FacetFieldList populated with FacetFieldObjects constructed from the provided
     * list of solr FacetFields which have counts greater than 0.
     * @param facetFields
     * @return List of FacetFieldObjects from a facet query result
     */
    public FacetFieldList createFacetFieldList(List<FacetField> facetFields) {
        if (facetFields == null) {
            return null;
        }
        FacetFieldList facetFieldList = new FacetFieldList();
        for (FacetField facetField : facetFields) {
            String fieldName = SearchFieldKey.getByName(facetField.getName()).name();
            if (facetField.getValueCount() > 0) {
                facetFieldList.add(createFacetFieldObject(fieldName, facetField));
            }
        }
        return facetFieldList;
    }

    /**
     * Instantiate a FacetFieldObject from a solr facet field, or with an empty list of values if no
     * facetField is provided.
     * @param fieldKey
     * @param facetField
     * @return
     */
    public FacetFieldObject createFacetFieldObject(String fieldKey, FacetField facetField) {
        List<SearchFacet> values = new ArrayList<SearchFacet>();

        // Generate list of facet values from Solr facet fields if they are provided.
        if (facetField != null) {
            Class<?> facetClass = SearchSettings.FACET_CLASS_MAP.get(fieldKey);
            if (facetClass == null) {
                facetClass = GenericFacet.class;
            }
            try {
                Constructor<?> constructor = facetClass.getConstructor(String.class, FacetField.Count.class);
                for (FacetField.Count value : facetField.getValues()) {
                    values.add((GenericFacet) constructor.newInstance(fieldKey, value));
                }
            } catch (Exception e) {
                throw new InvalidFacetException(
                        "An exception occurred while attempting to instantiate a new facet field object for "
                                + fieldKey, e);
            }
        }

        return new FacetFieldObject(fieldKey, values);
    }

    /**
     * Updates the provided FacetFieldList to fill in empty facet entries for any facet names which are not present
     * @param facetFieldList
     * @param allFacetNames
     */
    public void addMissingFacetFieldObjects(FacetFieldList facetFieldList, Collection<String> allFacetNames) {
        for (String facetName : allFacetNames) {
            if (!facetFieldList.contains(facetName)) {
                facetFieldList.add(createFacetFieldObject(facetName, null));
            }
        }
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }
}
