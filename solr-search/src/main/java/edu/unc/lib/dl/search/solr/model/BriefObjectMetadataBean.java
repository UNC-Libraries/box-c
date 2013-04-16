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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Stores a single Solr tuple representing an object from a search result. Can be populated directly by Solrj's
 * queryResponse.getBeans.
 * 
 * @author bbpennel
 */
public class BriefObjectMetadataBean extends IndexDocumentBean implements BriefObjectMetadata {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(BriefObjectMetadataBean.class);
	protected CutoffFacet ancestorPathFacet;
	protected CutoffFacet path;
	protected List<MultivaluedHierarchicalFacet> contentTypeFacet;
	protected List<Datastream> datastreamObjects;
	// Inverted map of the roleGroup, clustering roles into buckets by group
	Map<String, Collection<String>> groupRoleMap;
	protected Map<String, Long> countMap;
	protected ObjectAccessControlsBean accessControlBean;
	protected Map<String, List<String>> relationsMap;
	private List<Tag> tags;

	public BriefObjectMetadataBean() {
		countMap = new HashMap<String, Long>(2);
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
	
	public void setAncestorPathFacet(CutoffFacet ancestorPathFacet) {
		this.ancestorPathFacet = ancestorPathFacet;
	}

	@Field
	public void setAncestorPath(List<String> ancestorPaths) {
		super.setAncestorPath(ancestorPaths);
		this.ancestorPathFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPaths, 0);
	}

	/**
	 * Returns a HierarchicalFacet of the full path for this object, including the ancestor path and itself.
	 * 
	 * @return
	 */
	@Override
	public CutoffFacet getPath() {
		if (path == null) {
			if (this.ancestorPath == null) {
				this.path = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "1," + this.id + "," + this.title,
						0L);
			} else {
				path = new CutoffFacet(ancestorPathFacet);
				path.addNode(id, title);
			}
		}
		return path;
	}

	@Override
	public List<MultivaluedHierarchicalFacet> getContentTypeFacet() {
		return contentTypeFacet;
	}

	@Field
	public void setContentType(ArrayList<String> contentTypes) {
		super.setContentType(contentTypes);
		this.contentTypeFacet = MultivaluedHierarchicalFacet.createMultivaluedHierarchicalFacets(
				SearchFieldKeys.CONTENT_TYPE.name(), contentTypes);
	}

	@Override
	public List<Datastream> getDatastreamObjects() {
		return datastreamObjects;
	}
	
	@Override
	public Datastream getDatastreamObject(String datastreamName) {
		if (datastreamName == null || this.datastreamObjects == null)
			return null;
		
		String[] datastreamParts = datastreamName.split("/", 2);
		String pid;
		if (datastreamParts.length > 1) {
			pid = datastreamParts[0];
			if (pid.equals(this.id)) {
				pid = null;
			}
			datastreamName = datastreamParts[1];
		} else {
			pid = null;
		}
		
		for (Datastream datastream: this.datastreamObjects) {
			if (datastream.equals(datastreamName) && (pid == null || pid.equals(datastream.getOwner().getPid())))
				return datastream;
		}
		return null;
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

	@Override
	public Map<String, Collection<String>> getGroupRoleMap() {
		return groupRoleMap;
	}

	@Override
	public ObjectAccessControlsBean getAccessControlBean() {
		if (this.accessControlBean == null && this.roleGroup != null) {
			this.accessControlBean = new ObjectAccessControlsBean(pid, this.roleGroup);
		}
		return this.accessControlBean;
	}
	
	@Override
	@Field
	public void setRelations(List<String> relations) {
		super.setRelations(relations);
		
		this.relationsMap = new HashMap<String, List<String>>(this.relations.size());
		for (String relation: this.relations) {
			if (relation == null)
				continue;
			String[] rdfParts = relation.split("\\|");
			
			List<String> values = this.relationsMap.get(rdfParts[0]);
			if (values == null) {
				values = new ArrayList<String>();
				this.relationsMap.put(rdfParts[0], values);
			}
			values.add(rdfParts[1]);
		}
	}
	
	public List<String> getRelation(String relationName) {
		return this.relationsMap.get(relationName);
	}
	
	@Override
	public Datastream getDefaultWebData() {
		if (this.relationsMap == null)
			return null;
		List<String> defaultWebDataValues = this.relationsMap.get(ContentModelHelper.CDRProperty.defaultWebData.getPredicate());
		if (defaultWebDataValues == null)
			return null;
		String defaultWebData = defaultWebDataValues.get(0);
		if (defaultWebData == null)
			return null;
		return this.getDatastreamObject(defaultWebData);
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

	@Override
	public CutoffFacetNode getParentCollectionObject() {
		if (ancestorPathFacet == null || parentCollection == null)
			return null;
		return (CutoffFacetNode) this.ancestorPathFacet.getNode(this.parentCollection);
	}

	@Override
	public Map<String, Long> getCountMap() {
		return this.countMap;
	}

	public void setCountMap(Map<String, Long> countMap) {
		this.countMap = countMap;
	}

	@Override
	public List<Tag> getTags() {
		if (this.tags == null)
			return null;
		return Collections.unmodifiableList(this.tags);
	}

	@Override
	public void addTag(Tag t) {
		if (this.tags == null)
			this.tags = new ArrayList<Tag>();
		this.tags.add(t);
	}
}
