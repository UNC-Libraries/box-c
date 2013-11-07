package edu.unc.lib.dl.security;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.errors.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * A factory for retrieving information pertaining to patron level access, including publication status and object state
 * 
 * @author bbpennel
 * 
 */
public class PatronAccessFactory {
	private static final Logger log = LoggerFactory.getLogger(PatronAccessFactory.class);

	private static final String STATE_ACTIVE = "info:fedora/fedora-system:def/model#Active";

	private Map<String, Boolean> pid2Publish;
	private Map<String, Boolean> pid2StateActive;
	private TripleStoreQueryService tripleStoreQueryService = null;
	private String getPublishAndStateQuery;

	public PatronAccessFactory() {
		Builder<String, Boolean> mapBuilder = new Builder<String, Boolean>();
		mapBuilder.maximumWeightedCapacity(256);
		this.pid2Publish = mapBuilder.build();
		this.pid2StateActive = mapBuilder.build();

		try {
			// Cache the query used for retrieving publication and object state
			java.io.InputStream inStream = this.getClass().getResourceAsStream("/getPublishAndState.itql");
			java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
			getPublishAndStateQuery = org.apache.commons.io.IOUtils.toString(inStreamReader);
		} catch (IOException e) {
			log.error("Failed to load queries", e);
		}
	}

	public Boolean isPublished(PID pid) throws ObjectNotFoundException {
		String pidString = pid.getPid();
		if (!this.pid2Publish.containsKey(pidString)) {
			updateCache(pid);
		}
		return this.pid2Publish.get(pidString);
	}

	public Boolean isStateActive(PID pid) throws ObjectNotFoundException {
		String pidString = pid.getPid();
		if (!this.pid2StateActive.containsKey(pidString)) {
			updateCache(pid);
		}
		return this.pid2StateActive.get(pidString);
	}

	private void updateCache(PID pid) throws ObjectNotFoundException {
		long start = System.currentTimeMillis();
		String query = String.format(getPublishAndStateQuery, tripleStoreQueryService.getResourceIndexModelUri(),
				pid.getURI());
		List<List<String>> results = tripleStoreQueryService.queryResourceIndex(query);

		if (log.isDebugEnabled())
			log.debug("Update cache for {} found {}", pid.getPid(), results);

		// Object was not found
		if (results.size() == 0)
			throw new ObjectNotFoundException("Failed to find object " + pid.getPid());

		// Since published may or may not be present, we may receive multiple entries for it
		String pidString = pid.getPid();
		boolean published = true;
		for (List<String> entry : results) {
			if ("no".equals(entry.get(0))) {
				published = false;
				break;
			}
		}
		pid2Publish.put(pidString, published);

		log.debug("Publication cache for {} set to {}", pidString, published);

		// State will always be present, so we can use the first value
		String state = results.get(0).get(1);
		if (state == null || !STATE_ACTIVE.equals(state))
			pid2StateActive.put(pidString, false);
		else
			pid2StateActive.put(pidString, true);

		log.debug("Patron Access Cache updated in {}ms", (System.currentTimeMillis() - start));
	}

	public void invalidate(PID pid) {
		String pidString = pid.getPid();
		log.debug("Invalidating patron access information for {}", pidString);
		this.pid2Publish.remove(pidString);
		this.pid2StateActive.remove(pidString);
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
