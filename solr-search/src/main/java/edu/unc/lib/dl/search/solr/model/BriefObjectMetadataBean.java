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

import edu.unc.lib.dl.fedora.PID;
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
	private List<Datastream> datastream;
	// Inverted map of the roleGroup, clustering roles into buckets by group
	Map<String,Collection<String>> groupRoleMap;
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
				this.ancestorPathFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH, "1," + this.id + "," + this.title, 0);
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
		this.contentType = contentTypes;
		this.contentTypeFacet = MultivaluedHierarchicalFacet.createMultivaluedHierarchicalFacets(SearchFieldKeys.CONTENT_TYPE, contentTypes);
	}

	public List<Datastream> getDatastreamObjects() {
		return datastream;
	}

	@Field
	public void setDatastream(String[] datastream) {
		ArrayList<Datastream> datastreams = new ArrayList<Datastream>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean contains(Object o) {
				if (o instanceof String)
					return indexOf(new Datastream((String)o)) != -1;
				return indexOf(o) != -1;
			}
		};
		
		for (String value : datastream) {
			datastreams.add(new Datastream(value));
		}
		this.datastream = datastreams;
	}
	
	@Override
	@Field
	public void setRoleGroup(List<String> roleGroup) {
		this.setRoleGroup(roleGroup);
		
		groupRoleMap = new HashMap<String,Collection<String>>();
		if (roleGroup != null){
			for (String roleGroupPair: roleGroup) {
				String[] roleGroupData = roleGroupPair.split("\\\\|");
				Collection<String> roles = groupRoleMap.get(roleGroupData[1]);
				if (roles == null) {
					roles = new ArrayList<String>();
					groupRoleMap.put(roleGroupData[1], roles);
				}
				roles.add(roleGroupData[0]);
			}
		}
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

	public String getParentCollection() {
		return parentCollection;
	}

	@Field
	public void setParentCollection(String parentCollection) {
		this.parentCollection = parentCollection;
	}
	
	public CutoffFacetNode getParentCollectionObject() {
		if (ancestorPath == null || parentCollection == null)
			return null;
		return (CutoffFacetNode)this.ancestorPathFacet.getNode(this.parentCollection);
	}

	public long getChildCount() {
		return childCount;
	}

	public void setChildCount(long childCount) {
		this.childCount = childCount;
	}

	public static class Datastream {
		private PID owner;
		private String name;
		private long filesize;
		private String mimetype;
		private String extension;
		private String category;

		public Datastream(String datastream) {
			if (datastream == null)
				throw new IllegalArgumentException("Datastream value must not be null");
			
			String[] dsParts = datastream.split("\\\\|");
			this.name = dsParts[0];
			try {
				this.filesize = Long.parseLong(dsParts[1]);
			} catch (NumberFormatException e) {
				this.filesize = 0;
			}
			this.mimetype = dsParts[2];
			this.extension = dsParts[3];
			this.category = dsParts[4];
			if (dsParts[5].length() > 0) {
				this.owner = new PID(dsParts[5]);
			} else {
				this.owner = null;
			}
		}
		
		@Override
		public boolean equals(Object object) {
			if (object == null)
				return false;
			if (object instanceof Datastream) {
				Datastream rightHand = (Datastream)object;
				// Equal if names match and either pids are null or both match
				return name.equals(rightHand.name) && (rightHand.owner == null || owner == null || owner.equals(rightHand.owner));
			}
			if (object instanceof String) {
				String rightHandString = (String)object;
				if (rightHandString.equals(this.name))
					return true;
				Datastream rightHand = new Datastream(rightHandString);
				return this.equals(rightHand);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		public String getName() {
			return name;
		}

		public PID getOwner() {
			return owner;
		}

		public long getFilesize() {
			return filesize;
		}

		public String getMimetype() {
			return mimetype;
		}

		public String getExtension() {
			return extension;
		}

		public String getCategory() {
			return category;
		}
	}
}
