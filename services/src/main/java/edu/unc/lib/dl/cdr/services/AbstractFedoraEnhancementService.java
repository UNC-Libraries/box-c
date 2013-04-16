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
package edu.unc.lib.dl.cdr.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.LabeledPID;
import edu.unc.lib.dl.cdr.services.processing.MessageDirector;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public abstract class AbstractFedoraEnhancementService implements ObjectEnhancementService, ApplicationContextAware {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractFedoraEnhancementService.class);

	protected TripleStoreQueryService tripleStoreQueryService = null;
	protected ManagementClient managementClient = null;
	protected boolean active = false;
	protected List<String> findCandidatesQueries;
	protected String findStaleCandidatesQuery;
	protected List<String> isApplicableQueries;

	private ApplicationContext applicationContext;

	@Override
	public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(message.getQualifiedAction())) {
			return true;
		}

		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(message.getQualifiedAction())
				&& this.getClass().getName().equals(message.getServiceName())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public MessageDirector getMessageDirector() {
		return this.applicationContext.getBean(MessageDirector.class);
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		return (List<PID>) this.findCandidateObjects(maxResults, 0, priorToDate, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PID> findCandidateObjects(int maxResults, int offset) throws EnhancementException {
		return (List<PID>) this.findCandidateObjects(maxResults, offset, null, false);
	}

	@Override
	public int countCandidateObjects() throws EnhancementException {
		return (Integer) this.findCandidateObjects(-1, 0, null, true);
	}

	public Object findCandidateObjects(int maxResults, int offset, String priorToDate, boolean countQuery)
			throws EnhancementException {
		if (priorToDate == null) {
			return this.executeCandidateQueries(this.findCandidatesQueries, countQuery, maxResults, offset);
		} else {
			String limitClause = "";
			if (maxResults >= 0 && !countQuery)
				limitClause = "LIMIT " + maxResults;
			return this.executeCandidateQuery(String.format(this.findStaleCandidatesQuery, this.getTripleStoreQueryService()
					.getResourceIndexModelUri(), priorToDate, limitClause) + limitClause, countQuery);
		}
	}

	@SuppressWarnings("unchecked")
	protected Object executeCandidateQueries(List<String> queries, boolean count, int limit, int offset) {
		int resultCount = 0;
		List<PID> results = new MaxSizeList<PID>(limit);
		for (String queryOriginal: queries) {
			String query = queryOriginal;
			if (!count)
				query += " LIMIT " + limit;
			query += " OFFSET " + offset;
			Object result = this.executeCandidateQuery(query, count);
			if (count) {
				resultCount += ((Integer)result).intValue();
			} else {
				List<PID> queryResults = (List<PID>) result;
				results.addAll(queryResults);
				if (results.size() >= limit)
					return results;
			}
		}
		
		if (count)
			return resultCount;
		return results;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object executeCandidateQuery(String query, boolean countQuery) {
		String format = "json";//((countQuery) ? "count/json" : "json");
		Map results = this.getTripleStoreQueryService().sendSPARQL(query, format);
		List<Map> bindings = (List<Map>) ((Map) results.get("results")).get("bindings");

		if (LOG.isDebugEnabled())
			LOG.debug(results.toString());
		if (countQuery) {
			// TODO Mulgara doesn't support count queries in SPARQL, will need to redo for other triple stores
			return bindings.size();
			/*Map binding = bindings.get(0);
			int count = Integer.parseInt((String) ((Map) binding.get("count")).get("value"));
			return count;*/
		} else {
			List<PID> result = new ArrayList<PID>();
			for (Map binding : bindings) {
				String pidURI = (String) ((Map) binding.get("pid")).get("value");
				String label = (String) ((Map) binding.get("label")).get("value");
				result.add(new LabeledPID(pidURI, label));
			}

			return result;
		}
	}
	
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		// Automatically isApplicable if the message is specifically asking for this service.
		String action = message.getQualifiedAction();
		if ((JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action) || JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action))
				&& this.getClass().getName().equals(message.getServiceName()))
			return true;
		
		return askQueries(this.isApplicableQueries, message);
	}
	
	protected boolean askQueries(List<String> queries, EnhancementMessage message) {
		for (String query: queries)
			if (askQuery(query, message))
				return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean askQuery(String query, EnhancementMessage message) {
		query = String.format(query,
				this.tripleStoreQueryService.getResourceIndexModelUri(), message.getPid().getURI());
		Map<String, Object> result = this.getTripleStoreQueryService().sendSPARQL(query);
		return (Boolean.TRUE.equals(result.get("boolean")));
	}

	/**
	 * @param filePath
	 *           name of file to open. The file can reside anywhere in the classpath
	 */
	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
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
	
	protected class MaxSizeList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;
		private int limit = 10;

		public MaxSizeList(int limit) {
			this.limit = limit;
		}
		
		@Override
		public boolean add(E element) {
			if (this.size() >= limit) return true;
			return super.add(element);
		}

		@Override
		public void add(int index, E element) {
			if (this.size() >= limit) return;
			super.add(index, element);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			if (c.size() + this.size() < limit)
				return super.addAll(c);
			Iterator<? extends E> it = c.iterator();
			while (it.hasNext()) {
				this.add(it.next());
				if (this.size() == limit)
					return true;
			}
			return false;
		}
	}
}
