package edu.unc.lib.boxc.search.api.facets;

/**
 * 
 * @author bbpennel
 *
 */
public interface HierarchicalFacetNode extends Cloneable {
    /**
     * Display value for the current node
     * @return
     */
    public String getDisplayValue();
    /**
     * Raw search value, the key for this facet node without all the extra syntax needed to search for it
     * @return
     */
    public String getSearchKey();
    /**
     * Value used to perform a search for items matching this facet value, with all necessary syntax
     * @return
     */
    public String getSearchValue();
    /**
     * Full value representing this facet node.  Generally this is the fully formatted value the node is derived from.
     * @return
     */
    public String getFacetValue();

    /**
     * Value used to restrict facet results to just the children of this facet node
     * @return
     */
    public String getPivotValue();
    /**
     * Value used to restrict search results to items matching this node, but not its children
     * @return
     */
    public String getLimitToValue();

    public Object clone();
}
