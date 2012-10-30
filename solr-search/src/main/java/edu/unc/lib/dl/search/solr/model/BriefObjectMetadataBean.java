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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * Stores a single Solr tuple representing an object from a search result. Can be populated directly by Solrj's
 * queryResponse.getBeans.
 * 
 * @author bbpennel
 */
public class BriefObjectMetadataBean extends IndexDocumentBean {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(BriefObjectMetadataBean.class);
	private CutoffFacet ancestorPathFacet;
	private CutoffFacet path;
	private List<MultivaluedHierarchicalFacet> contentTypeFacet;
	private List<Datastream> datastreamObjects;
	// Inverted map of the roleGroup, clustering roles into buckets by group
	Map<String, Collection<String>> groupRoleMap;
	private long childCount;

	public BriefObjectMetadataBean() {
	}

	// TODO getDefaultWebData getDefaultWebObject getFilesizeByDatastream

	public String getIdWithoutPrefix() {
		int index = id.indexOf(":");
		if (index != -1) {
			return id.substring(index + 1);
		}
		return id;
	}

	public CutoffFacet getAncestorPathFacet() {
		return ancestorPathFacet;
	}

	@Field
	public void setAncestorPath(List<String> ancestorPaths) {
		super.setAncestorPath(ancestorPaths);
		this.ancestorPathFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH, ancestorPaths, 0);
	}

	/**
	 * Returns a HierarchicalFacet of the full path for this object, including the ancestor path and itself.
	 * 
	 * @return
	 */
	public CutoffFacet getPath() {
		if (path == null) {
			if (this.ancestorPath == null) {
				this.ancestorPathFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH, "1," + this.id + "," + this.title,
						0L);
			} else {
				path = new CutoffFacet(ancestorPathFacet);
				path.addNode(id, title);
			}
		}
		return path;
	}

	public List<MultivaluedHierarchicalFacet> getContentTypeFacet() {
		return contentTypeFacet;
	}

	@Field
	public void setContentType(ArrayList<String> contentTypes) {
		super.setContentType(contentTypes);
		this.contentTypeFacet = MultivaluedHierarchicalFacet.createMultivaluedHierarchicalFacets(
				SearchFieldKeys.CONTENT_TYPE, contentTypes);
	}

	public List<Datastream> getDatastreamObjects() {
		return datastreamObjects;
	}

	@Field
	public void setDatastream(List<String> datastream) {
		super.setDatastream(datastream);

		datastreamObjects = new ArrayList<Datastream>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean contains(Object o) {
				if (o instanceof String)
					return indexOf(new Datastream((String) o)) != -1;
				return indexOf(o) != -1;
			}
		};

		for (String value : datastream) {
			datastreamObjects.add(new Datastream(value));
		}
	}

	public Datastream getDatastream(String datastreamName) {
		if (datastream == null || this.datastreamObjects == null)
			return null;
		for (Datastream datastream : this.datastreamObjects) {
			if (datastream.getName().equals(datastreamName))
				return datastream;
		}
		return null;
	}

	@Override
	@Field
	public void setRoleGroup(List<String> roleGroup) {
		super.setRoleGroup(roleGroup);

		groupRoleMap = new HashMap<String, Collection<String>>();
		if (roleGroup != null) {
			for (String roleGroupPair : roleGroup) {
				String[] roleGroupData = roleGroupPair.split("\\|");
				if (roleGroupData.length == 2) {
					Collection<String> roles = groupRoleMap.get(roleGroupData[1]);
					if (roles == null) {
						roles = new ArrayList<String>();
						groupRoleMap.put(roleGroupData[1], roles);
					}
					roles.add(roleGroupData[0]);
				}
			}
		}
	}

	public Map<String, Collection<String>> getGroupRoleMap() {
		return groupRoleMap;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("id: " + id + "\n");
		sb.append("ancestorPath: " + ancestorPath + "\n");
		sb.append("ancestorNames: " + ancestorNames + "\n");
		sb.append("resourceType: " + resourceType + "\n");
		sb.append("displayOrder: " + displayOrder + "\n");
		sb.append("contentType: " + contentType + "\n");
		sb.append("datastream: " + datastream + "\n");
		sb.append("title: " + title + "\n");
		sb.append("abstractText: " + abstractText + "\n");
		sb.append("keyword: " + keyword + "\n");
		sb.append("subject: " + subject + "\n");
		sb.append("language: " + language + "\n");
		sb.append("creator: " + creator + "\n");
		sb.append("department: " + department + "\n");
		sb.append("dateCreated: " + dateCreated + "\n");
		sb.append("dateAdded: " + dateAdded + "\n");
		sb.append("dateUpdated: " + dateUpdated + "\n");
		sb.append("timestamp: " + timestamp + "\n");
		return sb.toString();
	}

	public CutoffFacetNode getParentCollectionObject() {
		if (ancestorPathFacet == null || parentCollection == null)
			return null;
		return (CutoffFacetNode) this.ancestorPathFacet.getNode(this.parentCollection);
	}

	public long getChildCount() {
		return childCount;
	}

	public void setChildCount(long childCount) {
		this.childCount = childCount;
	}
}
