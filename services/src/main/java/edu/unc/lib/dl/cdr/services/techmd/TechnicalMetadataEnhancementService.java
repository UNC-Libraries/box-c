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
package edu.unc.lib.dl.cdr.services.techmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * This service will enhance repository objects by extracting technical metadata from source data. It will store
 * technical metadata in a MD_TECHNICAL data stream. It will add a techData relationship between the object and this new
 * data stream.
 * 
 * @author Gregory Jansen
 * 
 */
public class TechnicalMetadataEnhancementService extends AbstractIrodsObjectEnhancementService {

	private static final Logger LOG = LoggerFactory.getLogger(TechnicalMetadataEnhancementService.class);

	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		return this.findCandidateObjects(maxResults, priorToDate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#findCandidateObjects (int)
	 */
	@Override
	public List<PID> findCandidateObjects(int maxResults) throws EnhancementException {
		return this.findCandidateObjects(maxResults, null);
	}

	/*
	 * Finds objects that are candidates for technical metadata enhancement. Only returns PIDs that have a cdr:sourceData
	 * data stream where there is no cdr:techData or cdr:techData is younger than cdr:sourceData.
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#findCandidateObjects (int)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<PID> findCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		List<PID> result = new ArrayList<PID>();
		String query = null;
		try {
			if (priorToDate == null) {
				query = this.readFileAsString("techmd-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), maxResults);
			} else {
				query = this.readFileAsString("techmd-stale-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), priorToDate, maxResults);
			}
		} catch (IOException e) {
			throw new EnhancementException(e);
		}
		List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query).get("results"))
				.get("bindings");
		for (Map binding : bindings) {
			String pidURI = (String) ((Map) binding.get("pid")).get("value");
			result.add(new PID(pidURI));
		}
		LOG.debug(result.toString());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#getEnhancementTask (edu.unc.lib.dl.fedora.PID)
	 */
	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new TechnicalMetadataEnhancement(this, message.getPid());
	}
	
	@Override
	public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
		String action = message.getQualifiedAction();
		
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action))
			return true;
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action))
			return this.getClass().getName().equals(message.getServiceName());
		
		// If its not a Fedora message at this point, then its not going to match anything else
		if (!(message instanceof FedoraEventMessage))
			return false;
		
		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return true;
		
		if (!(JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action) 
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)))
			return false;
		String datastream = ((FedoraEventMessage)message).getDatastream();
		
		return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
	}

	/*
	 * Tests to see if this enhancement is applicable for the object with the supplied PID. Checks to make sure that the
	 * object has a cdr:sourceData data stream and either there is no cdr:techData or cdr:techData is younger than
	 * cdr:sourceData.
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isApplicable(edu .unc.lib.dl.fedora.PID)
	 */
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		LOG.debug("isApplicable called with " + message);
		
		String query = null;
		try {
			// replace model URI and PID tokens
			query = this.readFileAsString("techmd-applicable.sparql");
			query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), message.getPid()
					.getURI());
		} catch (IOException e) {
			throw new EnhancementException(e);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> result = this.getTripleStoreQueryService().sendSPARQL(query);
		LOG.debug("checking if Applicable");
		if (Boolean.TRUE.equals(result.get("boolean"))) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * For the moment technical metadata is never considered old or stale. Later on we might consider it stale each time
	 * more capable tools are brought online.
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isStale(edu.unc. lib.dl.fedora.PID)
	 */
	@Override
	public boolean isStale(PID pid) {
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
		String query = null;
		try {
			// replace model URI and PID tokens
			query = this.readFileAsString("techmd-last-applied.sparql");
			query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), pid
					.getURI());
		} catch (IOException e) {
			throw new EnhancementException(e);
		}
		@SuppressWarnings("unchecked")
		List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query).get("results"))
				.get("bindings");
		if (bindings.size() == 0)
			return null;
		
		EnhancementApplication lastApplied = new EnhancementApplication();
		String lastModified = (String) ((Map) bindings.get(0).get("lastModified")).get("value");
		lastApplied.setLastAppliedFromISO8601(lastModified);
		lastApplied.setPid(pid);
		lastApplied.setEnhancementClass(this.getClass());
		
		return lastApplied;
	}

}
