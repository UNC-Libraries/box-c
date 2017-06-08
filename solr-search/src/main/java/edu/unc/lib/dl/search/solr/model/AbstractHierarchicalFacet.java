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
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class AbstractHierarchicalFacet extends GenericFacet implements Cloneable {
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

    public void addNode(HierarchicalFacetNode node) {
        facetNodes.add(node);
    }

    public List<HierarchicalFacetNode> getFacetNodes() {
        return facetNodes;
    }

    public void setFacetNodes(List<HierarchicalFacetNode> facetNodes) {
        this.facetNodes = facetNodes;
    }

    public abstract String getSearchKey();
    public abstract String getPivotValue();

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
