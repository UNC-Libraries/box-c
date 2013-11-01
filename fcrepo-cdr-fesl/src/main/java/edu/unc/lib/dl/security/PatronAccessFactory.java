package edu.unc.lib.dl.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.errors.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class PatronAccessFactory {
	private static final Logger LOG = LoggerFactory.getLogger(PatronAccessFactory.class);
	
	private static final String STATE_ACTIVE = "info:fedora/fedora-system:def/model#Active";
	private static final String MULGARA_NULL = "http://mulgara.org/mulgara#null";
	
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
			Enumeration<URL> resources = this.getClass().getClassLoader().getResources("");
			LOG.warn("Found resources:");
			while (resources.hasMoreElements()) {
				LOG.warn("Resource:" + resources.nextElement().toString());
			}
			readFileAsString("/getPublishAndState.itql");
			java.io.InputStream inStream = this.getClass().getResourceAsStream("/getPublishAndState.itql");
			java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
			getPublishAndStateQuery = org.apache.commons.io.IOUtils.toString(inStreamReader);
		} catch (IOException e) {
			LOG.error("Failed to load queries", e);
		}
	}
	
	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		inStreamReader.close();
		inStream.close();
		return fileData.toString();
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
		String query = String.format(getPublishAndStateQuery, tripleStoreQueryService.getResourceIndexModelUri(), pid.getURI());
		LOG.warn("Querying: {}", query);
		List<List<String>> results = tripleStoreQueryService.queryResourceIndex(query);
		
		LOG.warn("Update cache for {} found {}", pid.getPid(), results);
		
		// Object was not found
		if (results.size() == 0)
			throw new ObjectNotFoundException("Failed to find object " + pid.getPid());
		
		String pidString = pid.getPid();
		
		String publish = results.get(0).get(0);
		if (publish == null || MULGARA_NULL.equals(publish) || "yes".equals(publish))
			pid2Publish.put(pidString, true);
		else pid2Publish.put(pidString, false);
		
		LOG.warn("Publish cache updated: ", pid2Publish);
		
		String state = results.get(0).get(1);
		if (state == null || !STATE_ACTIVE.equals(state))
			pid2StateActive.put(pidString, false);
		else pid2StateActive.put(pidString, true);
	}
	
	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
