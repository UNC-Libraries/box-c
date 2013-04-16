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
package edu.unc.lib.dl.cdr.services.solr;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException.Severity;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Service which determines when to update individual items in Solr.
 * 
 * @author bbpennel
 */
public class SolrUpdateEnhancementService extends AbstractSolrObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateEnhancementService.class);
	public static final String enhancementName = "Solr Update";

	private String isApplicableQuery;
	
	public SolrUpdateEnhancementService(){
		super();
	}
	
	public void init() {
		try {
			this.isApplicableQuery = this.readFileAsString("solr-update-applicable.sparql");
		} catch (IOException e) {
			LOG.error("Failed to read service query", e);
		}
	}
	
	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) {
		// All simple objects are candidates for loading into solr since we likely
		// don't
		// want to have to run double queries for every pid.
		return null;
	}

	@Override
	public List<PID> findCandidateObjects(int maxResults, int offset) {
		// All simple objects are candidates for loading into solr since we likely
		// don't
		// want to have to run double queries for every pid.
		return null;
	}

	@Override
	public int countCandidateObjects() throws EnhancementException {
		return 0;
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new SolrUpdateEnhancement(this, message.getPid());
	}

	@Override
	public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(message.getQualifiedAction()))
			return this.getClass().getName().equals(message.getServiceName());
		// Returns true if at least one other service passed prefilter
		// It is okay for ingest messages to pass here since if they are still orphaned they are not indexed.
		return message.getFilteredServices() != null && message.getFilteredServices().size() > 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		// Get lastModified from Fedora
		LOG.debug("isApplicable called with " + message);

		// Get dateUpdated from Solr
		try {
			Date solrDateModified = (Date) this.solrSearchService.getField(message.getTargetID(), "dateUpdated");
			if (solrDateModified == null) {
				LOG.debug("isApplicable due to solrDateModified being null");
				return true;
			} else {
				String solrDateModifiedString = org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(
						solrDateModified);

				// This message was not created as a result of the record being changed, so need to get Fedora updated
				// timestamp
				String fedoraDateModified = null;
				// replace model URI and PID tokens
				String query = String.format(this.isApplicableQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(), message
						.getPid().getURI());

				List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query)
						.get("results")).get("bindings");
				// Couldn't find the date modified, item likely no longer exists.
				if (bindings.size() == 0)
					return true;
				// Compare Solr updated timestamp to Fedora's. If Solr is older, than need to update.
				fedoraDateModified = (String) ((Map) bindings.get(0).get("modifiedDate")).get("value");
				if (solrDateModifiedString.compareTo(fedoraDateModified) < 0) {
					return true;
				}
			}
		} catch (SolrServerException e) {
			throw new EnhancementException("Error determining isApplicable for SolrUpdateEnhancement.", e,
					Severity.RECOVERABLE);
		}
		return false;
	}

	@Override
	public boolean isStale(PID pid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
		EnhancementApplication lastApplied = null;
		try {
			Date timestamp = (Date) this.solrSearchService.getField(pid.getPid(), "TIMESTAMP");
			lastApplied = new EnhancementApplication();
			lastApplied.setLastApplied(timestamp);
			lastApplied.setPid(pid);
			lastApplied.setEnhancementClass(this.getClass());
		} catch (SolrServerException e) {
			throw new EnhancementException(e);
		}
		return lastApplied;
	}

	@Override
	public String getName() {
		return enhancementName;
	}
}
