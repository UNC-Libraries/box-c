package edu.unc.lib.boxc.search.solr.facets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;
import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.CutoffFacetNode;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacetNode;

/**
 * Implementation of a hierarchical facet which supports cut off operations. Cut off operations
 * allow for filtering of result sets to ranges of depth within the facet. An example cut off
 * facet would be the Ancestor Path field.
 *
 * A cutoff facet value follows the format:
 *     <tier>,<value>
 * For example, to filter to all objects which are descendants of object "5bfe6a08-67d9-4d90-9e50-eeaf86aad37e":
 *     2,5bfe6a08-67d9-4d90-9e50-eeaf86aad37e
 * Where the parent object is at the second tier of the hierarchy.
 *
 * In order to instead limit to a single tier of children, the following value may be used:
 *     2,5bfe6a08-67d9-4d90-9e50-eeaf86aad37e!3
 * Where the !3 component will cause the cut off value of the facet to be set to 3 in queries.
 *
 * In order to limit to immediate children of any container at a particular tier, use the following:
 *     1,*!2
 * Which should return records that have any tier 2 value.
 *
 * @author bbpennel
 */
public class CutoffFacetImpl extends AbstractHierarchicalFacet implements CutoffFacet {
    private static final Logger LOG = LoggerFactory.getLogger(CutoffFacetImpl.class);

    // Maximum tier allowable in results
    private Integer cutoff;
    // Maximum tier allowable in facet results
    private Integer facetCutoff;

    public CutoffFacetImpl(String fieldName, String facetString) {
        super(fieldName, facetString);
        LOG.debug("Instantiating cutoff facet for " + fieldName + " from " + facetString);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(this.value);
        this.facetNodes.add(node);

    }

    public CutoffFacetImpl(String fieldName, String facetString, Long count) {
        super(fieldName, facetString, count);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(this.value);
        this.facetNodes.add(node);

    }

    /**
     * Instantiate a CutoffFacet from an ordered list of ancestor PIDs
     * @param fieldName
     * @param tierPids
     */
    public CutoffFacetImpl(String fieldName, List<PID> tierPids) {
        super(fieldName, (String) null);
        for (int i = 0; i < tierPids.size(); i++) {
            var facetString = (i + 1) + "," + tierPids.get(i).getId();
            CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(facetString);
            this.facetNodes.add(node);
        }
    }

    public CutoffFacetImpl(String fieldName, List<String> facetStrings, long count) {
        super(fieldName, null, count);
        for (String facetString: facetStrings) {
            CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(facetString);
            this.facetNodes.add(node);
        }
        this.sortTiers();
    }

    public CutoffFacetImpl(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(this.value);
        this.facetNodes.add(node);
    }

    public CutoffFacetImpl(CutoffFacetImpl facet) {
        super(facet);
        this.cutoff = facet.getCutoff();
        this.facetCutoff = facet.getFacetCutoff();
        for (HierarchicalFacetNode node: facet.getFacetNodes()) {
            this.facetNodes.add((HierarchicalFacetNode)node.clone());
        }
    }

    private String extractCutoffs(String facetValue) {
        if (facetValue == null) {
            return null;
        }
        String[] facetParts = facetValue.split("!");
        if (facetParts.length >= 2) {
            try {
                this.cutoff = new Integer(facetParts[1]);
            } catch (NumberFormatException e) {
                // Was not a cut off value, ignore
            }
        }

        if (facetParts.length >= 3) {
            try {
                this.facetCutoff = new Integer(facetParts[2]);
            } catch (NumberFormatException e) {
                // Was not a cut off value, ignore
            }
        }
        return facetParts[0];
    }

    private void sortTiers() {
        Collections.sort(this.facetNodes, new Comparator<HierarchicalFacetNode>() {
            @Override
            public int compare(HierarchicalFacetNode node1, HierarchicalFacetNode node2) {
                return ((CutoffFacetNode)node1).getTier() - ((CutoffFacetNode)node2).getTier();
            }
        });
    }

    public void addNode(String searchValue) {
        int highestTier = getHighestTier();
        CutoffFacetNodeImpl node = new CutoffFacetNodeImpl(searchValue, highestTier + 1);
        this.facetNodes.add(node);
    }

    @Override
    public CutoffFacetNodeImpl getHighestTierNode() {
        if (this.facetNodes == null || this.facetNodes.size() == 0) {
            return null;
        }

        CutoffFacetNodeImpl lastNode = (CutoffFacetNodeImpl)this.facetNodes.get(this.facetNodes.size() - 1);
        if ("*".equals(lastNode.getSearchKey())) {
            if (this.facetNodes.size() == 1) {
                return null;
            }
            return (CutoffFacetNodeImpl)this.facetNodes.get(this.facetNodes.size() - 2);
        }
        return lastNode;
    }

    @Override
    public int getHighestTier() {
        CutoffFacetNode lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return -1;
        }
        return lastNode.getTier();
    }

    @Override
    public String getDisplayValue() {
        return getSearchKey();
    }

    @Override
    public String getSearchKey() {
        CutoffFacetNodeImpl lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return null;
        }
        return lastNode.getSearchKey();
    }

    @Override
    public String getSearchValue() {
        CutoffFacetNodeImpl lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return null;
        }
        return lastNode.getSearchValue();
    }

    @Override
    public String getPivotValue() {
        CutoffFacetNodeImpl lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return null;
        }
        return lastNode.getPivotValue();
    }

    @Override
    public String getLimitToValue() {
        if (this.facetNodes.size() == 0) {
            return null;
        }
        CutoffFacetNodeImpl lastNode = (CutoffFacetNodeImpl)this.facetNodes.get(this.facetNodes.size() - 1);
        return lastNode.getLimitToValue();
    }

    @Override
    public Integer getCutoff() {
        return cutoff;
    }

    @Override
    public void setCutoff(Integer cutoff) {
        this.cutoff = cutoff;
    }

    @Override
    public Integer getFacetCutoff() {
        return facetCutoff;
    }

    public void setFacetCutoff(Integer facetCutoff) {
        this.facetCutoff = facetCutoff;
    }

    @Override
    public Object clone() {
        return new CutoffFacetImpl(this);
    }
}
