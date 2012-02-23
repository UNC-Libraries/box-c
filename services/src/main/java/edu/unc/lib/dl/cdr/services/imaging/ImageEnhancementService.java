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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Enhancement service used for construction of jp2 derived images.
 *
 * @author Gregory Jansen, bbpennel
 */
public class ImageEnhancementService extends AbstractIrodsObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(ImageEnhancementService.class);

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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<PID> findCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		List<PID> result = new ArrayList<PID>();
		String query = null;
		try {
			if (priorToDate == null){
				query = this.readFileAsString("image-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(),
						maxResults);
			} else {
				query = this.readFileAsString("image-stale-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(),
						priorToDate, maxResults);
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

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new ImageEnhancement(this, message);
	}
	
	@Override
	public boolean prefilterMessage(EnhancementMessage eMessage) throws EnhancementException {
		PIDMessage message = (PIDMessage)eMessage;
		String action = message.getQualifiedAction();
		
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action))
			return true;
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action))
			return this.getClass().getName().equals(message.getServiceName());
		
		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return true;
		
		if (!(JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action) 
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)))
			return false;
		String datastream = message.getDatastream();
		
		return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isApplicable(edu. unc.lib.dl.fedora.PID)
	 */
	@SuppressWarnings({ "unchecked"})
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		LOG.debug("isApplicable called with " + message.getTargetID());

		String query = null;
		try {
			// replace model URI and PID tokens
			query = this.readFileAsString("image-applicable.sparql");
			query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), message.getPid()
					.getURI(), "http://cdr.unc.edu/definitions/1.0/base-model.xml#derivedJP2");
		} catch (IOException e) {
			throw new EnhancementException(e);
		}
		Map<String, Object> result = this.getTripleStoreQueryService().sendSPARQL(query);
		LOG.debug("checking if Applicable");
		if (Boolean.TRUE.equals(result.get("boolean"))) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isStale(edu.unc.lib .dl.fedora.PID)
	 */
	@Override
	public boolean isStale(PID pid) {
		// TODO Auto-generated method stub
		return false;
	}
}
