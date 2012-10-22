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

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.ResourceType;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Indexing filter which extracts and stores hierarchical path information for the object being processed. It also sets
 * incidental fields that affect the hierarchy representation or are determined from it, including the parent
 * collection, rollup identifier, and content models. It uses either the objects FOXML and its previously cached parent
 * history (in the case of recursive reindexing) or queries the path information.
 * 
 * Sets: ancestorPath, ancestorNames, parentCollection, rollup, contentModel, label, resourceType
 * 
 * @author bbpennel
 * 
 */
public class SetPathFilter extends AbstractIndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetPathFilter.class);

	private String ancestorInfoQuery;
	private XPath contentModelXpath;
	
	private PID collectionsPID;

	public SetPathFilter() {
		try {
			contentModelXpath = XPath.newInstance("fedModel:hasModel/@rdf:resource");
			contentModelXpath.addNamespace(JDOMNamespaceUtil.RDF_NS);
			contentModelXpath.addNamespace(JDOMNamespaceUtil.FEDORA_MODEL_NS);
		} catch (JDOMException e) {
			log.error("Failed to initialize queries", e);
		}

		try {
			this.ancestorInfoQuery = this.readFileAsString("getAncestorInfo.itql");
		} catch (IOException e) {
			log.error("Unable to find query file", e);
		}
	}

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		DocumentIndexingPackage parentDIP = dip.getParentDocument();
		if (parentDIP != null && parentDIP.getDocument() != null && parentDIP.getDocument().getAncestorPath() != null) {
			// Must have parentDocuments and content models for this node
			buildFromParentDocuments(dip);
		} else {
			// If there is no parent information available, then build the hierarchy from scratch.
			buildFromQuery(dip);
		}
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
		// Rollup content models by pid
		for (List<String> row : results) {
			if (row.get(0).equals(previousPID)) {
				currentNode.contentModels.add(row.get(2));
			} else {
				previousPID = row.get(0);
				currentNode = new PathNode(row);
				pathNodes.add(currentNode);
			}
		}

		// Create the ancestorPath, which contains the path up to be not including the node being indexed
		List<String> ancestorPath = new ArrayList<String>(pathNodes.size() - 1);
		idb.setAncestorPath(ancestorPath);

		PathNode nearestCollection = null;
		PathNode firstAggregate = null;
		StringBuilder ancestorNames = new StringBuilder();
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
				ancestorPath.add(this.buildTier(++depth, node.pid, node.label));
				this.buildAncestorNames(ancestorNames, node.label);
			} else {
				// Store the last node in ancestor names only if it is a container type
				if (!ResourceType.Item.equals(node.resourceType))
					this.buildAncestorNames(ancestorNames, node.label);
			}
		}

		// Store the completed ancestorNames field
		idb.setAncestorNames(ancestorNames.toString());

		// Set the rollup ID for this document, depending on if it was in an aggregate
		if (firstAggregate == null) {
			idb.setRollup(idb.getId());
		} else {
			idb.setRollup(firstAggregate.pid.getPid());
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
		dip.setResourceType(currentNode.resourceType);
		dip.setLabel(currentNode.label);
	}

	private void buildFromParentDocuments(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();

		DocumentIndexingPackage parentDIP = dip.getParentDocument();
		if (parentDIP.getDocument().getAncestorPath().size() == 0 && !collectionsPID.equals(parentDIP.getPid())) {
			throw new IndexingException("Parent document " + parentDIP.getPid().getPid()
					+ " did not contain ancestor information for object " + dip.getPid().getPid());
		}

		Element relsExt = dip.getRelsExt();
		try {
			// Retrieve and store content models from the FOXML
			List<?> cmResults = this.contentModelXpath.selectNodes(relsExt);
			List<String> contentModels = new ArrayList<String>(cmResults.size());
			for (Object cmObject : cmResults) {
				contentModels.add(((Attribute) cmObject).getValue());
			}
			idb.setContentModel(contentModels);

			// Store the resourceType for this object
			ResourceType resourceType = ResourceType.getResourceTypeByContentModels(idb.getContentModel());
			idb.setResourceType(resourceType.name());
			dip.setResourceType(resourceType);

			// Set this items ancestor path to its parents ancestor path plus the parent itself.
			List<String> parentAncestors = parentDIP.getDocument().getAncestorPath();
			List<String> ancestorPath = new ArrayList<String>(parentAncestors.size() + 1);
			ancestorPath.addAll(parentAncestors);
			ancestorPath.add(this.buildTier(parentAncestors.size() + 1, parentDIP.getPid(), parentDIP.getLabel()));
			idb.setAncestorPath(ancestorPath);

			// If this object isn't any item, then add itself to its ancestorNames
			if (!ResourceType.Item.equals(resourceType)) {
				idb.setAncestorNames(this.buildAncestorNames(new StringBuilder(parentDIP.getDocument().getAncestorNames()),
						dip.getLabel()).toString());
			} else {
				idb.setAncestorNames(parentDIP.getDocument().getAncestorNames());
			}

			// Use the parents rollup if it isn't just its ID
			if (parentDIP.getPid().getPid().equals(parentDIP.getDocument().getRollup())) {
				idb.setRollup(parentDIP.getDocument().getRollup());
			} else {
				// If the immediate parent was an aggregate, use its ID as this items rollup
				if (ResourceType.Aggregate.equals(parentDIP.getResourceType())) {
					idb.setRollup(parentDIP.getPid().getPid());
				} else {
					idb.setRollup(idb.getId());
				}
			}

			// If the parent is a collection, then use it as this items parent collection
			if (ResourceType.Collection.equals(parentDIP.getResourceType())) {
				idb.setParentCollection(parentDIP.getPid().getPid());
			} else {
				// Otherwise, use whatever the parent had set as its collection
				idb.setParentCollection(parentDIP.getDocument().getParentCollection());
			}
		} catch (JDOMException e) {
			throw new IndexingException("Error while attempting to retrieve content models for " + dip.getPid().getPid(),
					e);
		}
	}

	private String buildTier(int depth, PID pid, String label) {
		StringBuilder ancestorTier = new StringBuilder();
		ancestorTier.append(depth).append(',').append(pid.getPid()).append(',').append(label.replaceAll(",", "\\\\,"));
		return ancestorTier.toString();
	}

	private StringBuilder buildAncestorNames(StringBuilder ancestorNames, String label) {
		return ancestorNames.append('/').append(label.replaceAll("\\/", "\\\\/"));
	}

	public void setAncestorInfoQuery(String ancestorInfoQuery) {
		this.ancestorInfoQuery = ancestorInfoQuery;
	}

	public void setCollectionsPID(PID collectionsPID) {
		this.collectionsPID = collectionsPID;
	}

	private static class PathNode {
		PID pid;
		String label;
		List<String> contentModels;
		ResourceType resourceType;

		public PathNode(List<String> row) {
			// $p $pid $slug $label $contentModel
			this.pid = new PID(row.get(0));
			this.label = row.get(1);
			this.contentModels = new ArrayList<String>();
			this.contentModels.add(row.get(2));
		}
	}
}
