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
	ADD("ADD"),
	DELETE("DELETE"),
	COMMIT("COMMIT"), // Causes an immediate upload and commit of pending updates  
	RECURSIVE_ADD("RECURSIVE_ADD"), //Updates this pid and issues recursiveAdd actions for all of its Fedora children
	RECURSIVE_REINDEX("RECURSIVE_REINDEX"), //Updates this pid, issues recursiveReindex for all its Fedora children, and issues recursiveDelete for any children that were not updated
	RECURSIVE_DELETE("RECURSIVE_DELETE"), //Recursively deletes and object and all its children of an object from solr, based on Fedora contains relations 
	DELETE_SOLR_TREE("DELETE_SOLR_TREE"), //Deletes an object and all children that contained the object in their collection path.
	CLEAN_REINDEX("CLEAN_REINDEX"), //Performs a full delete followed by a full reindex.
	DELETE_CHILDREN_PRIOR_TO_TIMESTAMP("DELETE_CHILDREN_PRIOR_TO_TIMESTAMP"), //Deletes the trees of all children of the starting node if they have not been updated since the given timestamp
	CLEAR_INDEX("CLEAR_INDEX"); //Deletes everything in the index
	
	private final String name;
	private URI uri;
	public static final String namespace = JDOMNamespaceUtil.CDR_MESSAGE_NS + "/solr/";
	
	SolrUpdateAction(String name){
		this.name = name;
		try {
			setUri(name);
		} catch (URISyntaxException e) {
			Error x = new ExceptionInInitializerError("Error creating URI for SolrUpdateAction " + name);
			x.initCause(e);
			throw x;
		}
	}
	
	public void setUri(String name) throws URISyntaxException{
		this.uri = new URI(namespace + "/" + name);
	}
	
	public String getName(){
		return this.name;
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