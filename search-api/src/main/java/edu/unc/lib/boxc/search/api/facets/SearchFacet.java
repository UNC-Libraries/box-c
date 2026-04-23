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
    String getFieldName();

    /**
     * Returns the number of results matching this facet
     *
     * @return
     */
    long getCount();

    /**
     * Returns the string value assigned to this facet
     *
     * @return
     */
    String getValue();

    /**
     * Returns the value used for displaying the facets value to users
     *
     * @return
     */
    String getDisplayValue();

    /**
     * Returns the value used for searching for this facet
     *
     * @return
     */
    String getSearchValue();

    /**
     * Returns the value for limiting results to this facet, formatted such that
     * this facet type can parse it into a new SearchFacet. Used as the search
     * value which appears in API calls, but not necessarily the format expected
     * by the search index.
     *
     * @return
     */
    String getLimitToValue();
}
