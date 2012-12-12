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
package edu.unc.lib.dl.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.fcrepo.server.errors.ObjectNotFoundException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ParentBond;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * A factory for getting the list of ancestors for a pid. This will be
 * implemented with a small cache.
 * 
 * @author count0
 * 
 */
public class AncestorFactory {
	private WeakHashMap<String, ParentBond> child2Parent = new WeakHashMap<String, ParentBond>(
			256);

	private TripleStoreQueryService tripleStoreQueryService = null;

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	/**
	 * Get the role inheritance lineage for a particular pid, starting with immediate parent.
	 * 
	 * @param pid
	 * @return a list of all the parents in order
	 */
	public List<PID> getInheritanceList(PID pido) throws ObjectNotFoundException {
		String pid = pido.getPid();
		List<PID> result = new ArrayList<PID>();
		while(true) {
			if(!child2Parent.containsKey(pid)) updateCache(pid); // not cached
			ParentBond bond = child2Parent.get(pid);
			if(bond == null || !bond.inheritsRoles) { // no more parents or not inheriting further
				break;
			} else {
				result.add(new PID(bond.parentPid)); // add inheriting parent
				pid = bond.parentPid; // set up loop
			}
		}
		return result;
	}
	
	public ParentBond getParentBond(PID pid) throws ObjectNotFoundException {
		if(!child2Parent.containsKey(pid.getPid())) updateCache(pid.getPid()); // not cached
		return child2Parent.get(pid.getPid());
	}

	/**
	 * Destroy the cached pointers to a parent when it is edited.
	 * 
	 * @param parentPID
	 */
	public void invalidateBondsToChildren(PID parentPID) {
		child2Parent.remove(parentPID.getPid());
		Iterator<Map.Entry<String, ParentBond>> sweep = child2Parent.entrySet().iterator();
		while(sweep.hasNext()) {
			Map.Entry<String, ParentBond> entry = sweep.next();
			if (entry.getValue().parentPid.equals(parentPID.getPid())) {
				child2Parent.remove(entry.getKey());
			}
		}
	}
	
	public void invalidateBondToParent(PID childPID) {
		child2Parent.remove(childPID);
	}

	private void updateCache(String pid) throws ObjectNotFoundException {
		List<ParentBond> lineage = getTripleStoreQueryService()
				.lookupRepositoryAncestorInheritance(new PID(pid));
		String childPid = pid;
		for (ParentBond bond : lineage) {
			child2Parent.put(childPid, bond);
			childPid = bond.parentPid;
		}
	}
}
