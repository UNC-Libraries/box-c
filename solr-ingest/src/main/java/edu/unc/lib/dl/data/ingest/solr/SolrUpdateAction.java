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

public enum SolrUpdateAction {
	ADD,
	DELETE,
	COMMIT, // Causes an immediate upload and commit of pending updates  
	RECURSIVE_ADD, //Updates this pid and issues recursiveAdd actions for all of its Fedora children
	RECURSIVE_REINDEX, //Updates this pid, issues recursiveReindex for all its Fedora children, and issues recursiveDelete for any children that were not updated
	RECURSIVE_DELETE, //Recursively deletes and object and all its children of an object from solr, based on Fedora contains relations 
	DELETE_SOLR_TREE, //Deletes an object and all children that contained the object in their collection path.
	CLEAN_REINDEX, //Performs a full delete followed by a full reindex.
	DELETE_CHILDREN_PRIOR_TO_TIMESTAMP, //Deletes the trees of all children of the starting node if they have not been updated since the given timestamp
	CLEAR_INDEX //Deletes everything in the index
}