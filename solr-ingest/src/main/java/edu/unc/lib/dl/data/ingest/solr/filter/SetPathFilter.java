package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.ResourceType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetPathFilter extends AbstractIndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetPathFilter.class);

	private TripleStoreQueryService tsqs;
	private String ancestorInfoQuery;
	private XPath contentModelXpath;

	public SetPathFilter() {
		try {
			contentModelXpath = XPath.newInstance("/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='RELS-EXT']/"
					+ "*[local-name() = 'datastreamVersion']/*[local-name() = 'xmlContent']/*[local-name() = 'RDF']/"
					+ "*[local-name() = 'Description']");
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

		if (dip.getParentDocuments() == null || dip.getParentDocuments().size() == 0) {
			buildFromQuery(dip);
		} else {
			// Must have parentDocuments and content models for this node
			buildFromParentDocuments(dip);
		}
	}

	private void buildFromQuery(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		// Parent documents are not already cached, so query for the full path
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
				StringBuilder ancestorTier = new StringBuilder();
				ancestorTier.append(++depth).append(',').append(node.pid.getPid()).append(',')
						.append(node.label.replaceAll(",", "\\\\,"));
				ancestorPath.add(ancestorTier.toString());
				ancestorNames.append('/').append(node.label.replaceAll("\\/", "\\\\/"));
			} else {
				// Store the last node in ancestor names only if it is a container type
				if (!ResourceType.Item.equals(node.resourceType))
					ancestorNames.append('/').append(node.label.replaceAll("\\/", "\\\\/"));
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
	}

	private void buildFromParentDocuments(DocumentIndexingPackage dip) throws IndexingException {

	}

	public void setTripleStoreQueryService(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}

	public void setAncestorInfoQuery(String ancestorInfoQuery) {
		this.ancestorInfoQuery = ancestorInfoQuery;
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
