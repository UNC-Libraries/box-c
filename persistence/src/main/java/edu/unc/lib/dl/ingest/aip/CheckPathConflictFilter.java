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
package edu.unc.lib.dl.ingest.aip;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Looks for objects in the repository with matching repository paths. Conflict information is then passed to a merge
 * strategy class that performs required merging logic or throws a MergeException.
 * 
 * @author count0
 * 
 */
public class CheckPathConflictFilter implements AIPIngestFilter {
	private static final Log log = LogFactory.getLog(CheckPathConflictFilter.class);

	private TripleStoreQueryService tripleStoreQueryService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.ingest.IngestFilter#doFilter(edu.unc.lib.dl.ingest.IngestContextImpl)
	 */
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException {
		RDFAwareAIPImpl rdfaip = null;
		if (aip instanceof RDFAwareAIPImpl) {
			rdfaip = (RDFAwareAIPImpl) aip;
		} else {
			rdfaip = new RDFAwareAIPImpl(aip);
		}
		filter(rdfaip);
		return rdfaip;
	}

	private void filter(RDFAwareAIPImpl rdfaip) throws AIPException {
		Map<String, PID> ingestPath2PidMap = this.makeIngestPath2PIDMap(rdfaip);

		Map<String, PID> repoPath2PidMap = getConflictingObjects(ingestPath2PidMap);
		if (repoPath2PidMap.size() > 0) {
			StringBuffer message = new StringBuffer("Objects in SIP conflict with these existing object paths:");
			for (String path : repoPath2PidMap.keySet()) {
				message.append("\n").append(path);
			}
			throw new AIPException(message.toString());
		}
	}

	private Map<String, PID> getConflictingObjects(Map<String, PID> inDocs) throws AIPException {
		Map<String, PID> result = new HashMap<String, PID>();
		for (String path : inDocs.keySet()) {
			PID conflict = this.getTripleStoreQueryService().fetchByRepositoryPath(path);
			if (conflict != null) {
				// yeah there's a conflict
				result.put(path, conflict);
				log.debug("MATCH: " + path);
			}
		}
		return result;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	private Map<String, PID> makeIngestPath2PIDMap(RDFAwareAIPImpl rdfaip) throws AIPException {
		Map<String, PID> result = new HashMap<String, PID>();
		for (PID pid : rdfaip.getPIDs()) {
			String path = makePath(pid, rdfaip);
			if (path == null) {
				log.error("Could not get repository path for an object.");
				throw new AIPException("Could not get repository path for an object.");
			}
			if (result.containsKey(path)) {
				String msg = "Duplicate object found in SIP: " + path;
				log.error(msg);
				throw new AIPException(msg);
			}
			result.put(path, pid);
			log.debug(pid + "|" + path);
		}
		return result;
	}

	private String makePath(PID pid, RDFAwareAIPImpl rdfaip) {
		StringBuffer result = new StringBuffer();
		Stack<String> slugs = new Stack<String>();

		// while there's another object above, push slug
		PID step = pid;
		PID top = null;
		do {
			String slug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), step,
					ContentModelHelper.CDRProperty.slug.getURI());
			slugs.push(slug);
			top = step;
			step = JRDFGraphUtil.getPIDRelationshipSubject(rdfaip.getGraph(),
					ContentModelHelper.Relationship.contains.getURI(), step);
		} while (step != null);

		// got all the parents in the graph, make result
		String containerPath = getTripleStoreQueryService()
				.lookupRepositoryPath(rdfaip.getContainerPlacement(top).parentPID);
		if (containerPath.endsWith("/")) {
			containerPath = containerPath.substring(0, containerPath.length() - 1);
		}
		result.append(containerPath);
		while (!slugs.isEmpty()) {
			result.append("/").append(slugs.pop());
		}
		return result.toString();
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

}
