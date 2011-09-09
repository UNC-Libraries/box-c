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
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.JMSMessageUtil;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Generates surrogate thumbnail images of items with data_files of image format types.
 * @author Gregory Jansen, bbpennel
 *
 */
public class ThumbnailEnhancementService extends AbstractIrodsObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(ThumbnailEnhancementService.class);

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
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#findCandidateObjects (int)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<PID> findCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		List<PID> result = new ArrayList<PID>();
		String query = null;
		try {
			if (priorToDate == null) {
				query = this.readFileAsString("thumbnail-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(),
						ContentModelHelper.Datastream.THUMB_SMALL.getName(), maxResults);
			} else {
				query = this.readFileAsString("thumbnail-stale-candidates.sparql");
				query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(),
						ContentModelHelper.Datastream.THUMB_SMALL.getName(), priorToDate, maxResults);
			}
		} catch (IOException e) {
			LOG.error("Failed to retrieve candidates for ThumbnailEnhancementService", e);
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
	public Enhancement<Element> getEnhancement(PIDMessage pid) {
		return new ThumbnailEnhancement(this, pid);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#isApplicable(edu .unc.lib.dl.fedora.PID)
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public boolean isApplicable(PIDMessage pid) throws EnhancementException {
		LOG.debug("isApplicable called with " + pid);
		if (pid.getMessage() != null) {
			LOG.debug("isApplicable called with message:\n " + new XMLOutputter().outputString(pid.getMessage()));
		}
		if (pid.getMessage() != null) {
			String action = pid.getMessage().getRootElement().getChildTextTrim("title", JDOMNamespaceUtil.ATOM_NS);
			String datastream = JMSMessageUtil.getDatastream(pid.getMessage());
			String relationship = JMSMessageUtil.getPredicate(pid.getMessage());
			// if the message does not indicate a change to an applicable
			// datastream, then done
			if (!(
					((JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action) || JMSMessageUtil.FedoraActions.ADD_DATASTREAM
					.equals(action) || JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE
					.equals(action)) && (ContentModelHelper.Datastream.DATA_FILE.getName().equals(datastream) || ContentModelHelper.Datastream.RELS_EXT.getName().equals(datastream) ))
					||
					JMSMessageUtil.FedoraActions.INGEST.equals(action)
					||
					((JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP
					.equals(action) || JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.equals(action)) && (ContentModelHelper.CDRProperty.sourceData
					.getURI().toString().equals(relationship) || ContentModelHelper.CDRProperty.hasSurrogate.getURI()
					.toString().equals(relationship)))
					)) {
				return false;
			}
		}
		boolean needsThumb = false;
		boolean isThumbForOthers = false;
		String query = null;
		try {
			// replace model URI and PID tokens
			query = this.readFileAsString("thumbnail-applicable.sparql");
			query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), pid.getPID()
					.getURI(), "http://cdr.unc.edu/definitions/1.0/base-model.xml#thumb");
			Map<String, Object> result = this.getTripleStoreQueryService().sendSPARQL(query);
			LOG.debug("checking if Applicable");
			if (Boolean.TRUE.equals(result.get("boolean"))) {
				needsThumb = true;
			}
		} catch (IOException e) {
			LOG.error("isApplicable failed for ThumbnailEnhancementService " + pid.getPIDString(), e);
			throw new EnhancementException(e);
		}

		// replace model URI and PID tokens
		List<PID> haveThisSurrogate = this.getTripleStoreQueryService().fetchPIDsSurrogateFor(pid.getPID());
		if (haveThisSurrogate.size() > 0)
			isThumbForOthers = true;

		return needsThumb || isThumbForOthers;
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
}
