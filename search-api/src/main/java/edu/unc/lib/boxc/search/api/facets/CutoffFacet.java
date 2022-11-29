package edu.unc.lib.boxc.search.api.facets;

/**
 * A hierarchical facet which supports cut off operations. Cut off operations allow for filtering of
 * result sets to ranges of depth within the facet.
 *
 * For example, if there existed records distributed between 6 tiers of a hierarchical cut off
 * facet, a search result could be limited to records which have tiers 3 through 5 of the facet
 * by filtering with the value of the 3rd tier, and a cutoff value of 5.
 *
 * @author bbpennel
 */
public interface CutoffFacet extends HierarchicalFacet {
    public static final String SUBFIELD_DELIMITER = ",";

    /**
     * @return The hierarchy node with the highest tier value
     */
    CutoffFacetNode getHighestTierNode();

    /**
     * @return highest tier value held by a node in this facet
     */
    int getHighestTier();

    /**
     * @return upper cutoff tier, objects above this tier should not be returned in search results.
     */
    Integer getCutoff();

    /**
     * @param cutoff Cutoff value to set
     */
    void setCutoff(Integer cutoff);

    /**
     * @return Tier of values which should be returned in a facet result, as compared to a search result.
     */
    Integer getFacetCutoff();

}