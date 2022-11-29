package edu.unc.lib.boxc.search.solr.facets;

import org.apache.solr.client.solrj.response.FacetField;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;

/**
 * Stores a individual facet entry
 * @author bbpennel
 */
public class GenericFacet implements Cloneable, SearchFacet {
    //Name of the facet group to which this facet belongs.
    protected String fieldName;
    protected long count;
    protected String value;
    protected String displayValue;

    public GenericFacet() {
    }

    public GenericFacet(SearchFieldKey fieldKey, String facetString) {
        this(fieldKey.name(), facetString);
    }

    /**
     * Default constructor which takes the name of the facet and the string representing it.
     * @param fieldName name of the facet to which this entry belongs.
     * @param facetString string from which the attributes of the facet will be interpreted.
     */
    public GenericFacet(String fieldName, String facetString) {
        this.count = 0;
        this.fieldName = fieldName;
        this.value = facetString;
        this.displayValue = facetString;
    }

    public GenericFacet(FacetField.Count countObject) {
        this(countObject.getFacetField().getName(), countObject);
    }

    public GenericFacet(String fieldName, FacetField.Count countObject) {
        this.count = countObject.getCount();
        this.fieldName = fieldName;
        this.value = countObject.getName();
        this.displayValue = countObject.getName();
    }

    public GenericFacet(GenericFacet facet) {
        this.fieldName = facet.getFieldName();
        this.count = facet.getCount();
        this.value = facet.getValue();
        this.displayValue = facet.getDisplayValue();
    }


    @Override
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public String getSearchValue() {
        return value;
    }

    @Override
    public String getLimitToValue() {
        return value;
    }

    @Override
    public Object clone() {
        return new GenericFacet(this);
    }
}
