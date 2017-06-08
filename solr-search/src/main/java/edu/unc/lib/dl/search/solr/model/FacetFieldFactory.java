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
package edu.unc.lib.dl.search.solr.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.exception.InvalidFacetException;
import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * 
 * @author bbpennel
 *
 */
public class FacetFieldFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FacetFieldFactory.class);

    private SearchSettings searchSettings;
    private SolrSettings solrSettings;

    public GenericFacet createFacet(String fieldKey, String facetValue) {
        Class<?> facetClass = searchSettings.getFacetClasses().get(fieldKey);
        if (facetClass == null) {
            facetClass = GenericFacet.class;
        }
        try {
            Constructor<?> constructor = facetClass.getConstructor(String.class, String.class);
            Object newFacet = constructor.newInstance(fieldKey, facetValue);
            /*if (newFacet == null)
                throw new Exception();*/
            return (GenericFacet) newFacet;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InvalidHierarchicalFacetException) {
                throw (InvalidHierarchicalFacetException)e.getCause();
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

    public FacetFieldList createFacetFieldList(List<FacetField> facetFields) {
        if (facetFields == null) {
            return null;
        }
        Map<String, String> fieldNameMappings = solrSettings.getFieldNameToKey();

        FacetFieldList facetFieldList = new FacetFieldList();
        for (FacetField facetField : facetFields) {
            String fieldName = fieldNameMappings.get(facetField.getName());
            if (facetField.getValueCount() > 0) {
                facetFieldList.add(createFacetFieldObject(fieldName, facetField));
            }
        }
        return facetFieldList;
    }

    public FacetFieldObject createFacetFieldObject(String fieldKey, FacetField facetField) {
        List<GenericFacet> values = new ArrayList<GenericFacet>();

        // Generate list of facet values from Solr facet fields if they are provided.
        if (facetField != null) {
            Class<?> facetClass = searchSettings.getFacetClasses().get(fieldKey);
            if (facetClass == null) {
                facetClass = GenericFacet.class;
            }
            try {
                Constructor<?> constructor = facetClass.getConstructor(String.class, FacetField.Count.class);
                if (facetField != null) {
                    for (FacetField.Count value : facetField.getValues()) {
                        values.add((GenericFacet) constructor.newInstance(fieldKey, value));
                    }
                }
            } catch (Exception e) {
                throw new InvalidFacetException(
                        "An exception occurred while attempting to instantiate a new facet field object for "
                                + fieldKey, e);
            }
        }

        return new FacetFieldObject(fieldKey, values);
    }

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
