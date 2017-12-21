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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.ContentModelHelper;

/**
 *
 * @author bbpennel
 *
 */
public class HierarchicalBrowseResultResponse extends SearchResultResponse {
    protected static final Logger log = LoggerFactory.getLogger(HierarchicalBrowseResultResponse.class);

    private Map<String, Long> subcontainerCounts;
    private Set<String> matchingContainerPids = null;
    private Long rootCount;
    private ResultNode rootNode;

    public HierarchicalBrowseResultResponse() {
        super();
        subcontainerCounts = new HashMap<String, Long>();
        matchingContainerPids = new HashSet<String>();
    }

    public void setSearchResultResponse(SearchResultResponse response) {
        this.setFacetFields(response.getFacetFields());
        this.setResultCount(response.getResultCount());
        this.setGeneratedQuery(response.getGeneratedQuery());
        this.setResultList(response.getResultList());
        this.setSearchState(response.getSearchState());
    }

    public Map<String, Long> getSubcontainerCounts() {
        return subcontainerCounts;
    }

    public void setSubcontainerCounts(Map<String, Long> subcontainerCounts) {
        this.subcontainerCounts = subcontainerCounts;
    }

    public void populateSubcontainerCounts(List<FacetField> facetFields) {
        subcontainerCounts = new HashMap<String, Long>();
        for (FacetField facetField : facetFields) {
            if (facetField.getValues() != null) {
                for (FacetField.Count facetValue : facetField.getValues()) {
                    log.debug("Popsub|" + facetValue.getName() + ":" + facetValue.getCount());
                    int index = facetValue.getName().indexOf(",");
                    index = facetValue.getName().indexOf(",", index + 1);
                    if (index != -1) {
                        subcontainerCounts.put(facetValue.getName().substring(0, index), facetValue.getCount());
                    }
                }
            }
        }
    }

    public void removeContainersWithoutContents() {
        ListIterator<BriefObjectMetadata> resultIt = this.getResultList().listIterator(this.getResultList().size());
        while (resultIt.hasPrevious()) {
            BriefObjectMetadata briefObject = resultIt.previous();
            if (briefObject == null || briefObject.getContentModel() == null) {
                continue;
            }
            if ((!briefObject.getCountMap().containsKey("child") || briefObject.getCountMap().get("child") == 0)
                    && briefObject.getContentModel().contains(ContentModelHelper.Model.CONTAINER.toString())) {
                if (this.matchingContainerPids != null && this.matchingContainerPids.contains(briefObject.getId())) {
                    // The container was directly found by the users query, so leave it as is.
                } else {
                    log.debug("Removing container " + briefObject.getId()
                            + "from hierarchical result because it has no children");
                    resultIt.remove();
                    // If an item is being filtered out, then decrement the counts for it and all its ancestors in
                    // subcontainer counts
                    if (briefObject.getAncestorPathFacet() != null
                            && briefObject.getAncestorPathFacet().getFacetNodes() != null) {
                        for (HierarchicalFacetNode facetTier : briefObject.getAncestorPathFacet().getFacetNodes()) {
                            String tierIdentifier = facetTier.getSearchValue();
                            Long count = this.subcontainerCounts.get(tierIdentifier);
                            if (count != null) {
                                this.subcontainerCounts.put(tierIdentifier, count - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public void populateMatchingContainerPids(SolrDocumentList containerList, String fieldName) {
        this.matchingContainerPids = new HashSet<String>();
        for (SolrDocument container : containerList) {
            this.matchingContainerPids.add((String) container.getFirstValue(fieldName));

        }
    }

    /**
     * Appends item results to the end of the list and adds them as children of the root.
     *
     * @param itemResults
     */
    public void populateItemResults(List<BriefObjectMetadata> itemResults) {
        this.getResultList().addAll(itemResults);
    }

    /**
     * Generates a tree representation of the current result set and stores its root.
     *
     * Assumes the first result is the root node Assumes that the result set is sorted such that if a parent is present,
     * it will always appear before its children
     */
    public void generateResultTree() {
        if (this.getResultList() == null || this.getResultList().size() == 0) {
            return;
        }

        Map<String, ResultNode> nodeMap = new HashMap<String, ResultNode>();
        ResultNode parentNode = new ResultNode(this.getResultList().get(0));
        nodeMap.put(parentNode.getMetadata().getId(), parentNode);
        this.rootNode = parentNode;

        for (int i = 1; i < this.getResultList().size(); i++) {
            BriefObjectMetadata metadata = this.getResultList().get(i);

            // Find the closest parent record
            for (int j = metadata.getAncestorPathFacet().getFacetNodes().size() - 1; j >= 0; j--) {
                parentNode = nodeMap.get(metadata.getAncestorPathFacet().getFacetNodes().get(j).getSearchKey());
                if (parentNode != null) {
                    break;
                }
            }
            // Couldn't find any parent record, skip this item
            if (parentNode == null) {
                continue;
            }

            ResultNode currentNode = new ResultNode(metadata);
            parentNode.getChildren().add(currentNode);
            nodeMap.put(metadata.getId(), currentNode);
        }
    }

    public int getChildNodeIndex(String pid) {
        if (pid == null) {
            return -1;
        }
        for (int i = 0; i < rootNode.getChildren().size(); i++) {
            ResultNode childNode = rootNode.getChildren().get(i);
            if (childNode.getMetadata().getPid().getId().equals(pid)) {
                return i;
            }
        }
        return -1;
    }

    public ResultNode findNode(String pid) {
        return findNode(pid, this.rootNode);
    }

    private ResultNode findNode(String pid, ResultNode node) {
        if (node.getMetadata().getPid().getId().equals(pid)) {
            return node;
        }
        for (ResultNode childNode: node.getChildren()) {
            ResultNode found = findNode(pid, childNode);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void addNodes(List<BriefObjectMetadata> nodes) {

    }

    public Long getRootCount() {
        return rootCount;
    }

    public void setRootCount(Long rootCount) {
        this.rootCount = rootCount;
    }

    public Set<String> getMatchingContainerPids() {
        return matchingContainerPids;
    }

    public void setMatchingContainerPids(Set<String> matchingContainerPids) {
        this.matchingContainerPids = matchingContainerPids;
    }

    public void setMatchingContainerPids(List<String> matchingContainerPids) {
        this.matchingContainerPids = new HashSet<String>(matchingContainerPids);
    }

    public ResultNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(ResultNode rootNode) {
        this.rootNode = rootNode;
    }

    public static class ResultNode {
        private BriefObjectMetadata metadata;
        private List<ResultNode> children;
        boolean isTopLevel;

        public ResultNode() {
            this.children = new ArrayList<ResultNode>();
        }

        public ResultNode(BriefObjectMetadata metadata) {
            this();
            this.metadata = metadata;
        }

        public BriefObjectMetadata getMetadata() {
            return metadata;
        }

        public void setMetadata(BriefObjectMetadata metadata) {
            this.metadata = metadata;
        }

        public List<ResultNode> getChildren() {
            return children;
        }

        public void setChildren(List<ResultNode> children) {
            this.children = children;
        }

        public Boolean getIsTopLevel() {
            if (metadata.getAncestorNames() != null && (metadata.getAncestorPath() == null
                    || metadata.getAncestorPath().size() == 0)) {
                return true;
            }
            return null;
        }

        public void setIsTopLevel(boolean isTopLevel) {
            this.isTopLevel = isTopLevel;
        }

        public ResultNode addChild(BriefObjectMetadata metadata) {
            ResultNode newNode = new ResultNode(metadata);
            this.children.add(newNode);
            return newNode;
        }
    }
}