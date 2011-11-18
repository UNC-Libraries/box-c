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
package edu.unc.lib.dl.services;

import java.util.Collection;

import edu.unc.lib.dl.fedora.PID;

public interface SolrIndexService {

	/**
	 * Adds new objects to the search index, or updates objects already indexed.
	 * 
	 * @param pids
	 *           a collection of pids to index
	 */
	public abstract boolean add(Collection<PID> ids);

	/**
	 * Removes objects from the search index.
	 * 
	 * @param pids
	 *           a collection of PIDs to remove from search.
	 */
	public abstract boolean remove(Collection<PID> ids);

	/**
	 * Removes everything from the index and re-adds everything found in Fedora.
	 */
	public abstract boolean reindexEverything();

}