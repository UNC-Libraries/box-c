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
package edu.unc.lib.dl.data.ingest.solr;

import java.net.URI;
import java.net.URISyntaxException;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public enum SolrUpdateAction {
	ADD("ADD", "Add/Update", "Adds or updates the entry for the specified object"),
	DELETE("DELETE", "Remove from Index", "Removes the index entry for the specified object"),
	COMMIT("COMMIT", "Commit", "Causes an immediate upload and commit of pending updates"), 
	RECURSIVE_ADD("RECURSIVE_ADD", "Update Path", 
			"Updates this pid and issues recursive add actions for all of its Fedora children"), 
	RECURSIVE_REINDEX("RECURSIVE_REINDEX", "In-place Reindex", 
			"Performs a recursive reindex of this object and all its children Updates this pid based off the originating structure, then cleans up any stale records."),
	RECURSIVE_DELETE("RECURSIVE_DELETE", "Delete Path from Index", 
			"Recursively deletes from the index an object and all of its children, based on the original structure"),
	DELETE_SOLR_TREE("DELETE_SOLR_TREE", "Delete Tree from Index", 
			"Deletes an object and all children that contained the object in their collection path"),
	CLEAN_REINDEX("CLEAN_REINDEX", "Clean Reindex", "Cleans out the path starting at the object specified and then reindexes it"),
	DELETE_CHILDREN_PRIOR_TO_TIMESTAMP("DELETE_CHILDREN_PRIOR_TO_TIMESTAMP", "Cleanup Outdated Records", 
			"Deletes the trees of all children of the starting node if they have not been updated since the given timestamp"), 
	CLEAR_INDEX("CLEAR_INDEX", "Delete Index", "Deletes everything from the index");
	
	private final String name;
	private final String label;
	private final String description;
	private URI uri;
	public static final String namespace = JDOMNamespaceUtil.CDR_MESSAGE_NS.getURI() + "/solr";
	
	SolrUpdateAction(String name, String label, String description){
		this.name = name;
		this.label = label;
		this.description = description;
		try {
			this.uri = new URI(JDOMNamespaceUtil.CDR_MESSAGE_NS.getURI() + "/solr/" + name);
		} catch (URISyntaxException e) {
			Error x = new ExceptionInInitializerError("Error creating URI for SolrUpdateAction " + name);
			x.initCause(e);
			throw x;
		}
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public URI getURI(){
		return this.uri;
	}
	
	public boolean equals(String value){
		return this.uri.toString().equals(value);
	}
	
	public String toString(){
		return this.uri.toString();
	}
	
	/**
	 * Finds an action that matches the full action uri provided.
	 * @param value
	 * @return
	 */
	public static SolrUpdateAction getAction(String value){
		if (value == null)
			return null;
		for (SolrUpdateAction action: values()){
			if (action.equals(value))
				return action;
		}
		return null;
	}
}