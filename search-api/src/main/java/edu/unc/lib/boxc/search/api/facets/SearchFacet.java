package edu.unc.lib.boxc.search.api.facets;

/**
 * 
 * @author bbpennel
 *
 */
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
     * Returns the value for limiting results to this facet, formatted such that
     * this facet type can parse it into a new SearchFacet. Used as the search
     * value which appears in API calls, but not necessarily the format expected
     * by the search index.
     *
     * @return
     */
    public String getLimitToValue();
}
