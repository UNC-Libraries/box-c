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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.exception.OrphanedObjectException;
import edu.unc.lib.dl.data.ingest.solr.exception.UnsupportedContentModelException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ResourceType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Indexing filter which extracts and stores hierarchical path information for the object being processed. It also sets
 * incidental fields that affect the hierarchy representation or are determined from it, including the parent
 * collection, rollup identifier, and content models. It uses either the objects FOXML and its previously cached parent
 * history (in the case of recursive reindexing) or queries the path information.
 *
 * Sets: ancestorPath, ancestorNames, parentCollection, rollup, isPart, contentModel, label, resourceType
 *
 * @author bbpennel
 *
 */
public class SetPathFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetPathFilter.class);

	private TripleStoreQueryService tsqs;
	
	private String ancestorInfoQuery;

	private PID collectionsPid;

	public SetPathFilter() {
		try {
			this.ancestorInfoQuery = this.readFileAsString("getAncestorInfo.itql");
		} catch (IOException e) {
			log.error("Unable to find query file", e);
		}
	}

	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		if (dip.hasParentDocument()) {
			DocumentIndexingPackage parentDIP = dip.getParentDocument();
			if (parentDIP.getDocument() != null && parentDIP.getDocument().getAncestorPath() != null) {
				// Must have parentDocuments and content models for this node
				buildFromParentDocuments(dip);
				return;
			}
		}
		
		// If there is no parent information available, then build the hierarchy from scratch.
		buildFromQuery(dip);
	}

	private void buildFromQuery(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		// Parent documents are not already cached, so query for the full path
		log.debug("Retrieving path information for " + dip.getPid().getPid() + " from triple store.");
		String query = String.format(ancestorInfoQuery, tsqs.getResourceIndexModelUri(), dip.getPid().getURI());
		List<List<String>> results = tsqs.queryResourceIndex(query);
		// Abandon ship if we couldn't get a path for this object.
		if (results.size() == 0) {
			throw new IndexingException("Object " + dip.getPid() + " could not be found");
		}
		List<PathNode> pathNodes = new ArrayList<PathNode>(results.size());
		PathNode currentNode = null;
		String previousPID = null;
		boolean orphaned = true;
		// Rollup content models by pid
		for (List<String> row : results) {
			String currentPid = row.get(1);
			if (orphaned && collectionsPid.getURI().equals(currentPid)) {
				orphaned = false;
			}
			if (currentPid.equals(previousPID)) {
				currentNode.contentModels.add(row.get(2));
			} else {
				previousPID = currentPid;
				currentNode = new PathNode(row);
				pathNodes.add(currentNode);
			}
		}
		if (orphaned && !collectionsPid.getPid().equals(dip.getPid()))
			throw new OrphanedObjectException("Object " + dip.getPid() + " is orphaned");

		// Sort the path nodes since they aren't guaranteed to be in order
		this.sortPathNodes(dip.getPid().getPid(), pathNodes);
		// Move the currentNode pointer to the last item in the sorted list
		currentNode = pathNodes.get(pathNodes.size() - 1);

		// Create the ancestorPath, which contains the path up to be not including the node being indexed
		List<String> ancestorPath = new ArrayList<String>(pathNodes.size() - 1);
		idb.setAncestorPath(ancestorPath);

		PathNode nearestCollection = null;
		PathNode firstAggregate = null;
		StringBuilder ancestorIds = new StringBuilder();
		int depth = 0;
		for (PathNode node : pathNodes) {
			node.resourceType = ResourceType.getResourceTypeByContentModels(node.contentModels);

			// Stop checking for collections and aggregates if we're inside an aggregate
			if (firstAggregate == null) {
				if (ResourceType.Collection.equals(node.resourceType)) {
					nearestCollection = node;
				} else if (ResourceType.Aggregate.equals(node.resourceType)) {
					firstAggregate = node;
				}
			}

			// Generate and store the current tiers ancestorPath value, except for the last tier
			if (depth < pathNodes.size() - 1) {
				ancestorPath.add(this.buildTier(++depth, node.pid));
				ancestorIds.append('/').append(node.pid);
			} else {
				// Store the last node in ancestor names only if it is a container type
				if (!ResourceType.File.equals(node.resourceType))
					ancestorIds.append('/').append(node.pid);
			}
		}

		// Store the completed ancestorNames field
		idb.setAncestorIds(ancestorIds.toString());

		// If this item is in an aggregate object then it should rollup as part of its parent
		if (firstAggregate != null && !firstAggregate.pid.equals(idb.getPid())) {
			idb.setRollup(firstAggregate.pid.getPid());
			idb.setIsPart(Boolean.TRUE);
			log.debug("From query, parent is in an aggregate: " + idb.getRollup());
		} else {
			idb.setRollup(idb.getId());
			idb.setIsPart(Boolean.FALSE);
			log.debug("From query, normal rollup: " + idb.getRollup());
		}

		// Store the parent collection if we found one.
		if (nearestCollection == null) {
			idb.setParentCollection(null);
		} else {
			idb.setParentCollection(nearestCollection.pid.getPid());
		}

		// Since we have already generated the content models and resource type for this item, store them as side effects
		idb.setContentModel(currentNode.contentModels);
		idb.setResourceType(currentNode.resourceType.name());
		idb.setResourceTypeSort(currentNode.resourceType.getDisplayOrder());
		dip.setResourceType(currentNode.resourceType);
	}

	/**
	 * Performs an in-place selection sort of the nodes, sorting by child to parent relationship
	 *
	 * @param endPID The pid for the node at the end of the chain
	 * @param pathNodes
	 */
	private void sortPathNodes(String endPID, List<PathNode> pathNodes) {
		// i is the index of the next swap destination, starting from the end and working backwards
		for (int i = pathNodes.size() - 1; i > 0; i--) {
			// Seek the node containing the next end id (either the previous parent or the starting id)
			for (int j = i; j >= 0; j--) {
				if (pathNodes.get(j).pid.getPid().equals(endPID)) {
					PathNode swap = pathNodes.get(j);
					// If the node isn't already where it belongs, swap it into place
					if (j != i) {
						pathNodes.set(j, pathNodes.get(i));
						pathNodes.set(i, swap);
					}
					// Get the next end node id
					endPID = swap.parentPID.getPid();
					break;
				}
			}
		}
	}

	private void buildFromParentDocuments(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();

		DocumentIndexingPackage parentDIP= dip.getParentDocument();
		if (parentDIP.getDocument().getAncestorPath().size() == 0 && !collectionsPid.equals(parentDIP.getPid())) {
			throw new IndexingException("Parent document " + parentDIP.getPid().getPid()
					+ " did not contain ancestor information for object " + dip.getPid().getPid());
		}

		Map<String, List<String>> triples = dip.getTriples();

		// Retrieve and store content models, either from rels-ext or stored
		List<String> cmResults = triples.get(ContentModelHelper.FedoraProperty.hasModel.toString());
		if (cmResults != null) {
			List<String> contentModels = new ArrayList<String>(cmResults.size());
			contentModels.addAll(cmResults);
			idb.setContentModel(contentModels);
		}

		// Store the resourceType for this object
		ResourceType resourceType = ResourceType.getResourceTypeByContentModels(idb.getContentModel());
		if (resourceType == null)
			throw new UnsupportedContentModelException("Cannot build path for object " + dip.getPid().getPid()
					+ " because it did not have a valid content model assigned.");
		idb.setResourceType(resourceType.name());
		dip.setResourceType(resourceType);
		idb.setResourceTypeSort(resourceType.getDisplayOrder());

		// Set this items ancestor path to its parents ancestor path plus the parent itself.
		List<String> parentAncestors = parentDIP.getDocument().getAncestorPath();
		List<String> ancestorPath = new ArrayList<String>(parentAncestors.size() + 1);
		ancestorPath.addAll(parentAncestors);
		ancestorPath.add(this.buildTier(parentAncestors.size() + 1, parentDIP.getPid()));
		idb.setAncestorPath(ancestorPath);

		StringBuilder ancestorIds = new StringBuilder(parentDIP.getDocument().getAncestorIds());
		// If this object isn't an item, then add itself to its ancestorNames
		if (!ResourceType.File.equals(resourceType)) {
			ancestorIds.append('/').append(dip.getPid().getPid());
		}
		idb.setAncestorIds(ancestorIds.toString());

		// If the parent has a rollup other than its own id, that means it is nested inside an aggregate, so it inherits
		if (!parentDIP.getPid().getPid().equals(parentDIP.getDocument().getRollup())) {
			if (parentDIP.getDocument().getRollup() == null) {
				idb.setRollup(idb.getId());
				idb.setIsPart(Boolean.FALSE);
				log.debug("From parent, parent rollup is null: " + idb.getRollup());
			} else {
				idb.setRollup(parentDIP.getDocument().getRollup());
				idb.setIsPart(Boolean.TRUE);
				log.debug("From parent, parent is in an aggregate: " + idb.getRollup());
			}
		} else {
			// If the immediate parent was an aggregate, use its ID as this items rollup
			if (ResourceType.Aggregate.equals(parentDIP.getResourceType())) {
				idb.setRollup(parentDIP.getPid().getPid());
				idb.setIsPart(Boolean.TRUE);
				log.debug("From parent, parent is an aggregate: " + idb.getRollup());
			} else {
				idb.setRollup(idb.getId());
				idb.setIsPart(Boolean.FALSE);
				log.debug("From parent, normal rollup: " + idb.getRollup());
			}
		}

		// If the parent is a collection, then use it as this items parent collection
		if (ResourceType.Collection.equals(parentDIP.getResourceType())) {
			idb.setParentCollection(parentDIP.getPid().getPid());
		} else {
			// Otherwise, use whatever the parent had set as its collection
			idb.setParentCollection(parentDIP.getDocument().getParentCollection());
		}
	}

	private String buildTier(int depth, PID pid) {
		StringBuilder ancestorTier = new StringBuilder();
		ancestorTier.append(depth).append(',').append(pid.getPid().replaceAll(",", "\\\\,"));
		return ancestorTier.toString();
	}

	public void setAncestorInfoQuery(String ancestorInfoQuery) {
		this.ancestorInfoQuery = ancestorInfoQuery;
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}

	public void setTsqs(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}
	
	private String readFileAsString(String filePath) throws java.io.IOException {
		return IOUtils.toString(this.getClass().getResourceAsStream(filePath));
	}

	private static class PathNode {
		PID parentPID;
		PID pid;
		List<String> contentModels;
		ResourceType resourceType;

		public PathNode(List<String> row) {
			// $p $pid $label $contentModel
			this.parentPID = new PID(row.get(0));
			this.pid = new PID(row.get(1));
			this.contentModels = new ArrayList<String>();
			this.contentModels.add(row.get(2));
		}
	}
}
