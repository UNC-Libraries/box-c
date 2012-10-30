package edu.unc.lib.dl.search.solr.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;

import edu.unc.lib.dl.search.solr.exception.InvalidFacetException;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class FacetFieldFactory {
	private SearchSettings searchSettings;
	private SolrSettings solrSettings;

	public GenericFacet createFacet(String fieldKey, String facetValue) {
		Class<?> facetClass = searchSettings.getFacetClasses().get(fieldKey);
		try {
			Constructor<?> constructor = facetClass.getConstructor(String.class, String.class);
			return (GenericFacet) constructor.newInstance(fieldKey, facetValue);
		} catch (Exception e) {
			throw new InvalidFacetException(
					"An exception occurred while attempting to instantiate a new facet field object for " + fieldKey + " "
							+ facetValue, e);
		}

	}

	public FacetFieldList createFacetFieldList(List<FacetField> facetFields) {
		if (facetFields == null)
			return null;
		Map<String, String> fieldNameMappings = solrSettings.getFieldNamesInverted();

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
						"An exception occurred while attempting to instantiate a new facet field object for " + fieldKey, e);
			}
		}

		return new FacetFieldObject(fieldKey, values);
	}

	public void addMissingFacetFieldObjects(FacetFieldList facetFieldList, List<String> allFacetNames) {
		for (String facetName : allFacetNames) {
			if (!facetFieldList.contains(facetName))
				facetFieldList.add(createFacetFieldObject(facetName, null));
		}
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}
}
