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
package edu.unc.lib.dl.cdr.services.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class TripleStoreQueryServiceMock implements TripleStoreQueryService {

	@Override
	public List<PID> fetchAllContents(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, PID> fetchChildSlugs(PID parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PathInfo> fetchChildPathInfo(PID parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PID> fetchChildContainers(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PID> fetchByPredicateAndLiteral(String predicateURI, String literal) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PID fetchByRepositoryPath(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PID fetchContainer(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PID fetchParentCollection(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PID> fetchObjectReferences(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResourceIndexModelUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isContainer(PID key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<URI> lookupContentModels(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupRepositoryPath(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PathInfo> lookupRepositoryPathInfo(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PID> lookupRepositoryAncestorPids(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> fetchAllTriples(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> lookupSinglePermission(PID pid, String permission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> lookupPermissions(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PID> lookupAllContainersAbove(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSourceData(PID pid, String datastreamID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowIndexing(PID pid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getSourceData(PID pid) {
		ArrayList<String> results = new ArrayList<String>();
		results.add(pid.getURI() + "/DATA_FILE");
		return results;
	}

	@Override
	public List<String> getSurrogateData(PID pid) {
		ArrayList<String> results = new ArrayList<String>();
		results.add(pid.getURI() + "/sourceData");
		return results;
	}

	@Override
	public List<List<String>> queryResourceIndex(String query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PID verify(PID key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> fetchAllCollectionPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupLabel(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupSlug(PID pid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookupSourceMimeType(PID pid) {
		return "";
	}

	@Override
	public List<PID> fetchPIDsSurrogateFor(PID pid) {
		return new ArrayList<PID>();
	}

	@Override
	public Map sendSPARQL(String query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map sendSPARQL(String query, String format) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOrphaned(PID key) {
		// TODO Auto-generated method stub
		return false;
	}

}
