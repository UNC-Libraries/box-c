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
package edu.unc.lib.dl.search.solr.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.model.ObjectPathEntry;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPathFactory {

	private static final Logger log = LoggerFactory.getLogger(ObjectPathFactory.class);

	private SolrSearchService search;
	private SolrSettings solrSettings;

	private int cacheSize;
	private long timeToLiveMilli = 5000L;

	private Map<String, PathParentBond> childToParent;
	// private final PathParentBond root = new PathParentBond();

	private List<String> bondFields;
	private String titleFieldName;
	private String typeFieldName;
	private String ancestorsFieldName;

	public ObjectPathFactory() {

	}

	@PostConstruct
	public void init() {
		Builder<String, PathParentBond> mapBuilder = new Builder<String, PathParentBond>();
		mapBuilder.maximumWeightedCapacity(cacheSize);
		this.childToParent = mapBuilder.build();

		titleFieldName = solrSettings.getFieldName(SearchFieldKeys.TITLE.name());
		typeFieldName = solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name());
		ancestorsFieldName = solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name());
		bondFields = Arrays.asList(titleFieldName, typeFieldName);
	}

	public String getName(String pid) {
		PathParentBond bond = getBond(pid);
		return bond != null ? bond.name : null;
	}

	public ObjectPath getPath(BriefObjectMetadata bom) {
		if (bom.getAncestorPathFacet() == null)
			return null;

		// Refresh the cache for the object being looked up if it is a container
		if (isContainer(bom.getResourceType())) {
			childToParent.put(bom.getId(), new PathParentBond(bom.getTitle(), true));
		}

		List<ObjectPathEntry> entries = new ArrayList<>();

		String parentPID = null;
		// Retrieve bonds for each node in the ancestor path
		for (HierarchicalFacetNode node : bom.getAncestorPathFacet().getFacetNodes()) {
			String pid = node.getSearchKey();
			PathParentBond bond = getBond(pid);
			bond.parent = parentPID;

			entries.add(new ObjectPathEntry(pid, bond.name, bond.isContainer));

			parentPID = pid;
		}

		return new ObjectPath(entries);
	}

	// Builds a list of path info, in order by tier
	public ObjectPath getPath(String pid) {

		List<ObjectPathEntry> entries = new ArrayList<>();

		String currentPID = pid;
		do {
			PathParentBond bond = getBond(pid);
			currentPID = bond.parent;

			entries.add(new ObjectPathEntry(pid, bond.name, bond.isContainer));
		} while (currentPID != null);

		return new ObjectPath(Lists.reverse(entries));
	}

	private PathParentBond getBond(String pid) {
		PathParentBond cacheBond = childToParent.get(pid);
		// Check if the cached values are still up to date
		if (cacheBond != null && System.currentTimeMillis() <= (cacheBond.retrievedAt + timeToLiveMilli)) {
			log.debug("Retrieved path information for {} from cache", pid);
			return cacheBond;
		}

		// Cache wasn't available, retrieve fresh data from solr
		try {
			Map<String, Object> fields = search.getFields(pid, bondFields);

			PathParentBond bond = new PathParentBond((String) fields.get(titleFieldName),
					isContainer((String) fields.get(typeFieldName)));

			// Cache the results for this bond
			childToParent.put(pid, bond);

			log.debug("Retrieved path information for {} from solr", pid);

			return bond;
		} catch (SolrServerException e) {
			log.error("Failed to get object path information for {}", pid, e);
		}
		return null;
	}

	private boolean isContainer(String resourceType) {
		return !"File".equals(resourceType);
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public void setTimeToLiveMilli(long timeToLiveMilli) {
		this.timeToLiveMilli = timeToLiveMilli;
	}

	public void setSearch(SolrSearchService search) {
		this.search = search;
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}

	// PID to title lookup
	// PID to parent pid
	public static class PathParentBond {
		public String parent;

		public String name;

		public boolean isContainer;

		public long lastIndexed;

		public long retrievedAt;

		public PathParentBond(String name, boolean isContainer) {
			this.name = name;
			this.isContainer = isContainer;
			retrievedAt = System.currentTimeMillis();
		}
	}
}
