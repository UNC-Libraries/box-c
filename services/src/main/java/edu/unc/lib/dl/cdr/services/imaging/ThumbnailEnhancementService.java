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
package edu.unc.lib.dl.cdr.services.imaging;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.AbstractXMLEventMessage;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Generates surrogate thumbnail images of items with data_files of image format types.
 * 
 * @author Gregory Jansen, bbpennel
 * 
 */
public class ThumbnailEnhancementService extends AbstractIrodsObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(ThumbnailEnhancementService.class);
	public static final String enhancementName = "Thumbnail Generation";
	
	private String isApplicableQuery;
	private String lastAppliedQuery;
	private String findCandidatesQuery;
	private String findStaleCandidatesQuery;
	
	public ThumbnailEnhancementService() {
		super();
		
		try {
			this.isApplicableQuery = this.readFileAsString("thumbnail-applicable.sparql");
			this.findCandidatesQuery = this.readFileAsString("thumbnail-candidates.sparql");
			this.findStaleCandidatesQuery = this.readFileAsString("thumbnail-stale-candidates.sparql");
			this.lastAppliedQuery = this.readFileAsString("thumbnail-last-applied.sparql");
		} catch (IOException e) {
			LOG.error("Failed to read service query", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		return (List<PID>)this.findCandidateObjects(maxResults, priorToDate, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PID> findCandidateObjects(int maxResults) throws EnhancementException {
		return (List<PID>)this.findCandidateObjects(maxResults, null, false);
	}
	
	@Override
	public int countCandidateObjects() throws EnhancementException {
		return (Integer)this.findCandidateObjects(-1, null, true);
	}

	public Object findCandidateObjects(int maxResults, String priorToDate, boolean countQuery) throws EnhancementException {
		String query = null;
		String limitClause = "";
		if (maxResults >= 0 && !countQuery) {
			limitClause = "LIMIT " + maxResults; 
		}
		if (priorToDate == null) {
			query = String.format(this.findCandidatesQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(),
					ContentModelHelper.Datastream.THUMB_SMALL.getName(), limitClause);
		} else {
			query = String.format(this.findStaleCandidatesQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(),
					ContentModelHelper.Datastream.THUMB_SMALL.getName(), priorToDate, limitClause);
		}
		return this.executeCandidateQuery(query, countQuery);
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new ThumbnailEnhancement(this, message.getPid());
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

		if (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
				|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)) {
			String datastream = ((FedoraEventMessage) message).getDatastream();
			return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
		}

		if (!(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.equals(action) || JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP
				.equals(action))) {
			return false;
		}

		String relationship = ((FedoraEventMessage) message).getRelationPredicate();
		return ContentModelHelper.CDRProperty.sourceData.equals(relationship)
				|| ContentModelHelper.CDRProperty.hasSurrogate.equals(relationship);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isApplicable(edu .unc.lib.dl.fedora.PID)
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		LOG.debug("isApplicable called with " + message);
		if (LOG.isDebugEnabled() && message instanceof AbstractXMLEventMessage
				&& ((AbstractXMLEventMessage) message).getMessageBody() != null) {
			LOG.debug("isApplicable called with message:\n "
					+ new XMLOutputter().outputString(((AbstractXMLEventMessage) message).getMessageBody()));
		}

		// replace model URI and PID tokens
		String query = String.format(this.isApplicableQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(), message.getPid()
				.getURI(), "http://cdr.unc.edu/definitions/1.0/base-model.xml#thumb");
		Map<String, Object> result = this.getTripleStoreQueryService().sendSPARQL(query);
		LOG.debug("checking if Applicable");
		if (Boolean.TRUE.equals(result.get("boolean"))) {
			// Needs thumb for itself
			return true;
		}

		List<PID> haveThisSurrogate = this.getTripleStoreQueryService().fetchPIDsSurrogateFor(message.getPid());
		if (haveThisSurrogate.size() > 0) {
			// Needs thumb as the surrogate for another object
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isStale(edu.unc. lib.dl.fedora.PID)
	 */
	@Override
	public boolean isStale(PID pid) {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
		// replace model URI and PID tokens
		String query = String.format(this.lastAppliedQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(), pid
				.getURI());
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
	
	@Override
	public String getName() {
		return enhancementName;
	}
}
