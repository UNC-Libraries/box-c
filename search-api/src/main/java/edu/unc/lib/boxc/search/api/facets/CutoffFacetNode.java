package edu.unc.lib.boxc.search.api.facets;

/**
 * Node within a hierarchical facet which supports cut off operations
 * @author bbpennel
 */
public interface CutoffFacetNode extends HierarchicalFacetNode {
    /**
     * @return the tier this node occupies within the hierarchy of the facet
     */
    Integer getTier();

}