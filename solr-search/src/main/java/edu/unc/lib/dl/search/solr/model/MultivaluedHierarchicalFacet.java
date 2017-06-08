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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;

/**
 * 
 * @author bbpennel
 *
 */
public class MultivaluedHierarchicalFacet extends AbstractHierarchicalFacet {
    private static final Logger log = LoggerFactory.getLogger(MultivaluedHierarchicalFacet.class);

    public MultivaluedHierarchicalFacet(String fieldName, String facetString) {
        super(fieldName, facetString);
        this.populateFacetNodes(facetString);
    }

    public MultivaluedHierarchicalFacet(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        this.populateFacetNodes(this.value);
    }

    private void populateFacetNodes(String facetString) {
        if (facetString == null) {
            return;
        }

        String[] tiers = MultivaluedHierarchicalFacetNode.extractFacetParts.split(facetString);
        if (tiers.length == 0) {
            throw new InvalidHierarchicalFacetException("Empty facet string");
        }

        // Create facet nodes for each tier in the facet string, skipping a leading blank node if present
        for (int i = "".equals(tiers[0]) ? 1 : 0; i < tiers.length; i++) {
            this.facetNodes.add(new MultivaluedHierarchicalFacetNode(tiers, i));
        }
    }

    public static List<MultivaluedHierarchicalFacet> createMultivaluedHierarchicalFacets(String fieldName,
            List<String> facetValues) {
        Map<String, MultivaluedHierarchicalFacet> facetMap = new LinkedHashMap<String, MultivaluedHierarchicalFacet>();
        for (String facetValue : facetValues) {
            try {
                MultivaluedHierarchicalFacetNode node = new MultivaluedHierarchicalFacetNode(facetValue);
                String firstTier = node.getTiers().get(0);

                MultivaluedHierarchicalFacet matchingFacet = facetMap.get(firstTier);
                if (matchingFacet == null) {
                    matchingFacet = new MultivaluedHierarchicalFacet(fieldName);
                    facetMap.put(firstTier, matchingFacet);
                }
                matchingFacet.addNode(node);
            } catch (InvalidHierarchicalFacetException e) {
                log.warn("Invalid hierarchical facet", e);
            }
        }
        for (MultivaluedHierarchicalFacet facet : facetMap.values()) {
            facet.sortTiers();
        }

        return new ArrayList<MultivaluedHierarchicalFacet>(facetMap.values());
    }

    public MultivaluedHierarchicalFacet(String fieldName) {
        super(fieldName);
    }

    public MultivaluedHierarchicalFacet(MultivaluedHierarchicalFacet facet) {
        super(facet);
        this.displayValue = facet.getDisplayValue();

        for (HierarchicalFacetNode node: facet.getFacetNodes()) {
            MultivaluedHierarchicalFacetNode newNode = new MultivaluedHierarchicalFacetNode(node.getFacetValue());
            this.facetNodes.add(newNode);
        }
    }

    public void sortTiers() {
        Collections.sort(this.facetNodes, new Comparator<HierarchicalFacetNode>() {
            @Override
            public int compare(HierarchicalFacetNode node1, HierarchicalFacetNode node2) {
                return ((MultivaluedHierarchicalFacetNode)node1).getTiers().size()
                        - ((MultivaluedHierarchicalFacetNode)node2).getTiers().size();
            }
        });
    }

    private MultivaluedHierarchicalFacetNode getLastNode() {
        return (MultivaluedHierarchicalFacetNode) this.facetNodes
                .get(this.facetNodes.size() - 1);
    }

    public HierarchicalFacetNode getNode(String searchKey) {
        for (HierarchicalFacetNode node: this.facetNodes) {
            if (node.getSearchKey().equals(searchKey)) {
                return node;
            }
        }
        return null;
    }

    public HierarchicalFacetNode getNodeBySearchValue(String searchValue) {
        for (HierarchicalFacetNode node: this.facetNodes) {
            if (node.getSearchValue().equals(searchValue)) {
                return node;
            }
        }
        return null;
    }

    public boolean contains(MultivaluedHierarchicalFacet facet) {
        if (facet.facetNodes.size() > this.facetNodes.size()) {
            return false;
        }

        for (int i = 0; i < facet.facetNodes.size(); i++) {
            if (!facet.facetNodes.get(i).getSearchKey().equals(this.facetNodes.get(i).getSearchKey())) {
                return false;
            }
        }
        return true;
    }

    public void setDisplayValues(MultivaluedHierarchicalFacet facet) {
        int startingCount = this.facetNodes.size();
        String targetJoined = this.getLastNode().joinTiers(false);
        for (HierarchicalFacetNode node: facet.getFacetNodes()) {
            MultivaluedHierarchicalFacetNode targetNode = (MultivaluedHierarchicalFacetNode)
                    this.getNodeBySearchValue(node.getSearchValue());
            if (targetNode != null) {
                log.debug("Adding in display value " + node.getDisplayValue());
                targetNode.setDisplayValue(node.getDisplayValue());
            } else {
                String joined = ((MultivaluedHierarchicalFacetNode)node).joinTiers(false);
                if (targetJoined.indexOf(joined) == 0) {
                    log.debug("Adding in missing node " + node.getSearchValue() + "," + node.getDisplayValue());
                    this.facetNodes.add((HierarchicalFacetNode)node.clone());
                }
            }
        }
        // If the number of nodes has changed, then resort
        if (this.facetNodes.size() != startingCount) {
            this.sortTiers();
        }
    }

    @Override
    public String getSearchKey() {
        return getLastNode().getSearchKey();
    }

    @Override
    public String getSearchValue() {
        return getLastNode().getSearchValue();
    }

    @Override
    public String getDisplayValue() {
        return getLastNode().getDisplayValue();
    }

    @Override
    public String getPivotValue() {
        return getLastNode().getPivotValue();
    }

    @Override
    public String getLimitToValue() {
        return getLastNode().getLimitToValue();
    }

    @Override
    public Object clone() {
        return new MultivaluedHierarchicalFacet(this);
    }
}
