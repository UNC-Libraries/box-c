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
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

/**
 * Service which determines when to update individual items in Solr.
 *
 * @author bbpennel
 */
public class SolrUpdateEnhancementService extends AbstractSolrObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateEnhancementService.class);

	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) {
		// All simple objects are candidates for loading into solr since we likely
		// don't
		// want to have to run double queries for every pid.
		return null;
	}

	@Override
	public List<PID> findCandidateObjects(int maxResults) {
		// All simple objects are candidates for loading into solr since we likely
		// don't
		// want to have to run double queries for every pid.
		return null;
	}

	@Override
	public Enhancement<Element> getEnhancement(PIDMessage pid) {
		return new SolrUpdateEnhancement(this, pid);
	}

	@Override
	public boolean prefilterMessage(PIDMessage pid) throws EnhancementException {
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(pid.getQualifiedAction()))
			return this.getClass().getName().equals(pid.getServiceName());
		//Returns true if at least one other service passed prefilter
		//It is okay for ingest messages to pass here since if they are still orphaned they are not indexed.
		return pid.getFilteredServices() != null && pid.getFilteredServices().size() > 0;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override

	public boolean isApplicable(PIDMessage pid) throws EnhancementException {
		// Get lastModified from Fedora
		LOG.debug("isApplicable called with " + pid);
		
		// Get dateUpdated from Solr
		try {
			Date solrDateModified = (Date) this.solrDataAccessLayer.getField(pid.getPIDString(),
					"dateUpdated");
			if (solrDateModified == null) {
				LOG.debug("isApplicable due to solrDateModified being null");
				return true;
			} else {
				String solrDateModifiedString = org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(solrDateModified);
				
				//This message was not created as a result of the record being changed, so need to get Fedora updated timestamp
				String query = null;
				String fedoraDateModified = null;
				try {
					// replace model URI and PID tokens
					query = super.readFileAsString("solr-update-applicable.sparql");
					query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), pid.getPID()
							.getURI());

					List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query).get("results"))
							.get("bindings");
					// Couldn't find the date modified, item likely no longer exists.
					if (bindings.size() == 0)
						return true;
					//Compare Solr updated timestamp to Fedora's.  If Solr is older, than need to update.
					fedoraDateModified = (String) ((Map) bindings.get(0).get("modifiedDate")).get("value");
					if (solrDateModifiedString.compareTo(fedoraDateModified) < 0) {
						return true;
					}
				} catch (IOException e) {
					throw new EnhancementException(e, Severity.UNRECOVERABLE);
				}
			}
		} catch (SolrServerException e){
			throw new EnhancementException("Error determining isApplicable for SolrUpdateEnhancement.", e, Severity.RECOVERABLE);
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
}
