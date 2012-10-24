package edu.unc.lib.dl.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.fcrepo.server.errors.ObjectNotFoundException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;


/**
 * A factory for getting the list of ancestors for a pid. This will be implemented with a small cache.
 * @author count0
 *
 */
public class AncestorFactory {
	private WeakHashMap<String, String> child2Parent = new WeakHashMap<String, String>(256);

	private TripleStoreQueryService tripleStoreQueryService = null;

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
	
	
	/**
	 * Get the lineage for a particular pid, starting with immediate parent.
	 * @param pid
	 * @return a list of all the parents in order
	 */
	public List<PID> getAncestry(String pid) throws ObjectNotFoundException {
		if(!child2Parent.containsKey(pid)) {
			updateCache(pid);
		}
		List<PID> result = new ArrayList<PID>();
		for(String parent = child2Parent.get(pid); parent != null; parent = child2Parent.get(parent)) {
			result.add(new PID(parent));
		}
		return result;
	}
	
	/**
	 * Destroy the cached pointers to a parent when it is edited.
	 * @param pid
	 */
	public void invalidate(String pid) {
		child2Parent.remove(pid);
		List<String> purge = new ArrayList<String>();
		for(Map.Entry<String, String> entry : child2Parent.entrySet()) {
			 if(entry.getValue().equals(pid)) {
				 purge.add(entry.getKey());
			 }
		}
		for(String child : purge) {
			child2Parent.remove(child);
		}
	}

	private void updateCache(String pid) throws ObjectNotFoundException {
		List<PID> topdown = getTripleStoreQueryService().lookupRepositoryAncestorPids(new PID(pid));
		String childPid = pid;
		Collections.reverse(topdown);
		if(topdown.size() > 0) {
			for(PID parent : topdown) {
				child2Parent.put(childPid, parent.getPid());
			}
		}
	}
}
