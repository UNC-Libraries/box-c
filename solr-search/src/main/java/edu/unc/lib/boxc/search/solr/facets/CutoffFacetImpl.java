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
 package edu.unc.lib.boxc.search.solr.facets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.CutoffFacetNode;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacetNode;

/**
 *
 * @author bbpennel
 *
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
