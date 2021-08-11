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
package edu.unc.lib.boxc.search.solr.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;

/**
 *
 * @author bbpennel
 *
 */
public class HierarchicalBrowseResultResponse extends SearchResultResponse {
    protected static final Logger log = LoggerFactory.getLogger(HierarchicalBrowseResultResponse.class);

    private Set<String> matchingContainerPids = null;
    private Long rootCount;
    private ResultNode rootNode;

    public HierarchicalBrowseResultResponse() {
        super();
        matchingContainerPids = new HashSet<>();
        setResultList(new ArrayList<>());
    }

    public void setSearchResultResponse(SearchResultResponse response) {
        this.setFacetFields(response.getFacetFields());
        this.setResultCount(response.getResultCount());
        this.setGeneratedQuery(response.getGeneratedQuery());
        this.setResultList(response.getResultList());
        this.setSearchState(response.getSearchState());
    }

    public void removeContainersWithoutContents() {
        ListIterator<ContentObjectRecord> resultIt = this.getResultList().listIterator(this.getResultList().size());
        while (resultIt.hasPrevious()) {
            ContentObjectRecord briefObject = resultIt.previous();
            if (briefObject == null || briefObject.getResourceType() == null) {
                continue;
            }
            String resourceType = briefObject.getResourceType();
            if ((!briefObject.getCountMap().containsKey("child") || briefObject.getCountMap().get("child") == 0)
                    && !ResourceType.File.equals(resourceType)) {
                if (this.matchingContainerPids != null && this.matchingContainerPids.contains(briefObject.getId())) {
                    // The container was directly found by the users query, so leave it as is.
                } else {
                    log.debug("Removing container " + briefObject.getId()
                            + "from hierarchical result because it has no children");
                    resultIt.remove();
                }
            }
        }
    }

    public void populateMatchingContainerPids(SolrDocumentList containerList, String fieldName) {
        this.matchingContainerPids = new HashSet<>();
        for (SolrDocument container : containerList) {
            this.matchingContainerPids.add((String) container.getFirstValue(fieldName));

        }
    }

    /**
     * Appends item results to the end of the list
     *
     * @param itemResults
     */
    public void populateItemResults(List<ContentObjectRecord> itemResults) {
        getResultList().addAll(itemResults);
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

        Map<String, ResultNode> nodeMap = new HashMap<>();
        if (rootNode == null) {
            rootNode = new ResultNode(this.getResultList().get(0));
        }
        ResultNode parentNode = rootNode;
        nodeMap.put(parentNode.getMetadata().getId(), parentNode);

        for (int i = 1; i < this.getResultList().size(); i++) {
            ContentObjectRecord metadata = this.getResultList().get(i);

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

    public void addNodes(List<ContentObjectRecord> nodes) {

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
        this.matchingContainerPids = new HashSet<>(matchingContainerPids);
    }

    public ResultNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(ResultNode rootNode) {
        this.rootNode = rootNode;
    }

    public static class ResultNode {
        private ContentObjectRecord metadata;
        private List<ResultNode> children;
        boolean isTopLevel;

        public ResultNode() {
            this.children = new ArrayList<>();
        }

        public ResultNode(ContentObjectRecord metadata) {
            this();
            this.metadata = metadata;
        }

        public ContentObjectRecord getMetadata() {
            return metadata;
        }

        public void setMetadata(ContentObjectRecord metadata) {
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

        public ResultNode addChild(ContentObjectRecord metadata) {
            ResultNode newNode = new ResultNode(metadata);
            this.children.add(newNode);
            return newNode;
        }
    }
}