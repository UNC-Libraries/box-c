package edu.unc.lib.boxc.search.api.facets;

import java.util.List;

/**
 * A facet which supports an ordered hierarchy of facet values within one field for the same record
 *
 * @author bbpennel
 */
public interface HierarchicalFacet extends SearchFacet {

    /**
     * Add a facet node as the last node in the hierarchy
     * @param node
     */
    void addNode(HierarchicalFacetNode node);

    /**
     * @return List of all hierarchy nodes in this facet
     */
    List<HierarchicalFacetNode> getFacetNodes();

    /**
     * @param searchKey
     * @return Facet node with search key matching the provided value
     */
    default HierarchicalFacetNode getNodeBySearchKey(String searchKey) {
        for (HierarchicalFacetNode node: getFacetNodes()) {
            if (node.getSearchKey().equals(searchKey)) {
                return node;
            }
        }
        return null;
    }

    /**
     * @param searchValue
     * @return facet node with search value matching the provided value
     */
    default HierarchicalFacetNode getNodeBySearchValue(String searchValue) {
        for (HierarchicalFacetNode node: getFacetNodes()) {
            if (node.getSearchValue().equals(searchValue)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Raw search value for this entire facet entry, as compared to the search key for an individual node.
     * The key for this facet node without all the extra syntax needed to search for it
     * @return
     */
    String getSearchKey();

    /**
     * Value used to restrict facet results to just the children of this facet
     * @return
     */
    String getPivotValue();

}