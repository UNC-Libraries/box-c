package edu.unc.lib.dl.search.solr.model;

public interface SearchFacet {

	/**
	 * Returns the name of the field this facet is representing
	 * 
	 * @return
	 */
	public String getFieldName();

	/**
	 * Returns the number of results matching this facet
	 * 
	 * @return
	 */
	public long getCount();

	/**
	 * Returns the string value assigned to this facet
	 * 
	 * @return
	 */
	public String getValue();

	/**
	 * Returns the value used for displaying the facets value to users
	 * 
	 * @return
	 */
	public String getDisplayValue();

	/**
	 * Returns the value used for searching for this facet
	 * 
	 * @return
	 */
	public String getSearchValue();

	/**
	 * Returns the value for limiting results to this facet
	 * 
	 * @return
	 */
	public String getLimitToValue();
}
