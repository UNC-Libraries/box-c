package edu.unc.lib.dl.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.errors.ObjectNotFoundException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * A factory for getting the list of ancestors for a pid. This will be
 * implemented with a small cache.
 * 
 * @author count0
 * 
 */
public class EmbargoFactory {
	private Map<PID, String> pid2Date = new HashMap<PID, String>(256);
	
	private boolean cacheValid = false;

	private TripleStoreQueryService tripleStoreQueryService = null;

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
	
	public List<String> getActiveEmbargoDates(Set<PID> pids) throws ObjectNotFoundException {
		List<String>  result = new ArrayList<String>();
		if(!cacheValid) updateCache();
		for(PID pid : pids) {
			if(pid2Date.containsKey(pid)) {
				result.add(pid2Date.get(pid));
			}
		}
		return result;
	}

	/**
	 * Call this method when any embargo triple is changed.
	 * 
	 * @param pid
	 */
	public void invalidate() {
		this.cacheValid = false;
	}

	private synchronized void updateCache() throws ObjectNotFoundException {
		this.pid2Date.clear();
		this.pid2Date.putAll(this.tripleStoreQueryService.fetchActiveEmbargoes());
	}
}
