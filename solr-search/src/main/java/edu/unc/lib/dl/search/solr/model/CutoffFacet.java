/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package edu.unc.lib.dl.search.solr.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author bbpennel
 *
 */
public class CutoffFacet extends AbstractHierarchicalFacet {
    private static final Logger LOG = LoggerFactory.getLogger(CutoffFacet.class);

    // Maximum tier allowable in results
    private Integer cutoff;
    // Maximum tier allowable in facet results
    private Integer facetCutoff;

    public CutoffFacet(String fieldName, String facetString) {
        super(fieldName, facetString);
        LOG.debug("Instantiating cutoff facet for " + fieldName + " from " + facetString);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNode node = new CutoffFacetNode(this.value);
        this.facetNodes.add(node);

    }

    public CutoffFacet(String fieldName, String facetString, Long count) {
        super(fieldName, facetString, count);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNode node = new CutoffFacetNode(this.value);
        this.facetNodes.add(node);

    }

    public CutoffFacet(String fieldName, List<String> facetStrings, long count) {
        super(fieldName, null, count);
        for (String facetString: facetStrings) {
            CutoffFacetNode node = new CutoffFacetNode(facetString);
            this.facetNodes.add(node);
        }
        this.sortTiers();
    }

    public CutoffFacet(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        this.value = this.extractCutoffs(this.value);
        CutoffFacetNode node = new CutoffFacetNode(this.value);
        this.facetNodes.add(node);
    }

    public CutoffFacet(CutoffFacet facet) {
        super((GenericFacet)facet);
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

    public void sortTiers() {
        Collections.sort(this.facetNodes, new Comparator<HierarchicalFacetNode>() {
            public int compare(HierarchicalFacetNode node1, HierarchicalFacetNode node2) {
                return ((CutoffFacetNode)node1).getTier() - ((CutoffFacetNode)node2).getTier();
            }
        });
    }

    public void addNode(HierarchicalFacetNode node) {
        facetNodes.add(node);
    }

    public void addNode(String searchValue) {
        int highestTier = getHighestTier();
        CutoffFacetNode node = new CutoffFacetNode(searchValue, highestTier + 1);
        this.facetNodes.add(node);
    }

    public HierarchicalFacetNode getNode(String searchKey) {
        for (HierarchicalFacetNode node: this.facetNodes) {
            if (((CutoffFacetNode)node).getSearchKey().equals(searchKey)) {
                return node;
            }
        }
        return null;
    }

    public CutoffFacetNode getHighestTierNode() {
        if (this.facetNodes == null || this.facetNodes.size() == 0) {
            return null;
        }

        CutoffFacetNode lastNode = (CutoffFacetNode)this.facetNodes.get(this.facetNodes.size() - 1);
        if ("*".equals(lastNode.getSearchKey())) {
            if (this.facetNodes.size() == 1) {
                return null;
            }
            return (CutoffFacetNode)this.facetNodes.get(this.facetNodes.size() - 2);
        }
        return lastNode;
    }

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
        CutoffFacetNode lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return null;
        }
        return lastNode.getSearchKey();
    }

    @Override
    public String getSearchValue() {
        CutoffFacetNode lastNode = this.getHighestTierNode();
        if (lastNode == null) {
            return null;
        }
        return lastNode.getSearchValue();
    }

    @Override
    public String getPivotValue() {
        CutoffFacetNode lastNode = this.getHighestTierNode();
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
        CutoffFacetNode lastNode = (CutoffFacetNode)this.facetNodes.get(this.facetNodes.size() - 1);
        return lastNode.getLimitToValue();
    }

    public Integer getCutoff() {
        return cutoff;
    }

    public void setCutoff(Integer cutoff) {
        this.cutoff = cutoff;
    }

    public Integer getFacetCutoff() {
        return facetCutoff;
    }

    public void setFacetCutoff(Integer facetCutoff) {
        this.facetCutoff = facetCutoff;
    }

    @Override
    public Object clone() {
        return new CutoffFacet(this);
    }
}
