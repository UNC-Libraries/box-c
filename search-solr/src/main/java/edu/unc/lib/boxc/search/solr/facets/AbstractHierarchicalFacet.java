package edu.unc.lib.boxc.search.solr.facets;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;

import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacetNode;

/**
 *
 * @author bbpennel
 *
 */
public abstract class AbstractHierarchicalFacet extends GenericFacet implements Cloneable, HierarchicalFacet {
    protected List<HierarchicalFacetNode> facetNodes;

    protected AbstractHierarchicalFacet() {
        facetNodes = new ArrayList<HierarchicalFacetNode>();
        this.count = 0;
    }

    protected AbstractHierarchicalFacet(String fieldName) {
        this();
        this.fieldName = fieldName;
    }

    protected AbstractHierarchicalFacet(String fieldName, String facetValue) {
        this();
        this.fieldName = fieldName;
        this.value = facetValue;
    }

    protected AbstractHierarchicalFacet(String fieldName, String facetValue, long count) {
        this();
        this.fieldName = fieldName;
        this.value = facetValue;
        this.count = count;
    }

    protected AbstractHierarchicalFacet(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        facetNodes = new ArrayList<HierarchicalFacetNode>();
    }

    protected AbstractHierarchicalFacet(GenericFacet facet) {
        super(facet);
        facetNodes = new ArrayList<HierarchicalFacetNode>();
    }

    @Override
    public void addNode(HierarchicalFacetNode node) {
        facetNodes.add(node);
    }

    @Override
    public List<HierarchicalFacetNode> getFacetNodes() {
        return facetNodes;
    }

    public void setFacetNodes(List<HierarchicalFacetNode> facetNodes) {
        this.facetNodes = facetNodes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder('[');
        boolean first = true;
        for (HierarchicalFacetNode node : this.facetNodes) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append('"').append(node.getSearchValue()).append('|')
                    .append(node.getDisplayValue()).append('|')
                    .append(node.getFacetValue()).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
