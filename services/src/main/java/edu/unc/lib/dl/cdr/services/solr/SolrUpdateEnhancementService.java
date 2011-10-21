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
import edu.unc.lib.dl.cdr.services.exception.RecoverableServiceException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

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
		String action = pid.getAction();
		return JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isApplicable(PIDMessage pid) {
		// Get lastModified from Fedora
		LOG.debug("isApplicable called with " + pid);
		if (pid.getMessage() != null){
			String action = pid.getAction();

			// Actions which will automatically indicate that an update is applicable.
			if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
					|| JMSMessageUtil.CDRActions.REORDER.equals(action) || JMSMessageUtil.CDRActions.REINDEX.equals(action)
					|| JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)
					|| JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.equals(action)) {
				LOG.debug("isApplicable due to message type " + action);
				return true;
			}

			// Actions which should double check that the item hasn't already been
			// updated before determining applicability.
			String datastream = pid.getDatastream();
			String relation = pid.getRelation();
			if (!(((JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.equals(action)
					|| JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.equals(action))
					&& ContentModelHelper.CDRProperty.thumb.getURI().toString().equals(relation))
					|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
					|| ((JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
					|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action))
					&& (ContentModelHelper.Datastream.DATA_FILE.getName().equals(datastream)
							|| ContentModelHelper.Datastream.DC.getName().equals(datastream)
							|| ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName().equals(datastream))))) {

				return false;
			}
		}
		
		// Get dateUpdated from Solr
		try {
			Date solrDateModified = (Date) solrUpdateService.getSolrDataAccessLayer().getField(pid.getPIDString(),
					"dateUpdated");
			if (solrDateModified == null) {
				LOG.debug("isApplicable due to solrDateModified being null");
				return true;
			} else {
				String solrDateModifiedString = org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(solrDateModified);
				
				if (pid.getTimestamp() == null || pid.getTimestamp().length() == 0){
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
							return false;
						//Compare Solr updated timestamp to Fedora's.  If Solr is older, than need to update.
						fedoraDateModified = (String) ((Map) bindings.get(0).get("modifiedDate")).get("value");
						if (solrDateModifiedString.compareTo(fedoraDateModified) < 0) {
							return true;
						}
					} catch (IOException e) {
						throw new Error(e);
					}
				} else {
					//Message has a updated timestamp, so compare the solr date to when the message's event took place.
					if (solrDateModifiedString.compareTo(pid.getTimestamp()) > 0){
						LOG.debug("Message timestamp: " + pid.getTimestamp() + " | Solr date: " + solrDateModifiedString);
						return false;
					} else {
						LOG.debug("Message timestamp: " + pid.getTimestamp() + " | Solr date: " + solrDateModifiedString);
						return true;
					}
				}
			}
		} catch (SolrServerException e){
			throw new RecoverableServiceException("Error determining isApplicable for SolrUpdateEnhancement.", e);
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
