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
import java.util.Arrays;
import java.util.List;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Generates surrogate thumbnail images of items with data_files of image format types.
 * 
 * @author Gregory Jansen, bbpennel
 * 
 */
public class ThumbnailEnhancementService extends AbstractDatastreamEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(ThumbnailEnhancementService.class);
	public static final String enhancementName = "Thumbnail Generation";

	private List<String> applicableNoDSQueries;
	private List<String> applicableStaleDSQueries;

	public ThumbnailEnhancementService() {
		super();
	}

	public void init() {
		try {
			this.findStaleCandidatesQuery = this.readFileAsString("thumbnail-stale-candidates.sparql");
			this.lastAppliedQuery = this.readFileAsString("thumbnail-last-applied.sparql");
			this.findCandidatesQueries = Arrays.asList(this.readFileAsString("thumbnail-candidates-no-ds.sparql"),
					this.readFileAsString("thumbnail-candidates-stale-ds.sparql"),
					this.readFileAsString("thumbnail-candidates-no-surrogate-ds.sparql"),
					this.readFileAsString("thumbnail-candidates-stale-surrogate-ds.sparql"));
			for (int i = 0; i < this.findCandidatesQueries.size(); i++) {
				this.findCandidatesQueries.set(
						i,
						String.format(this.findCandidatesQueries.get(i),
								this.tripleStoreQueryService.getResourceIndexModelUri()));
			}

			this.isApplicableQueries = Arrays.asList(readFileAsString("thumbnail-applicable-no-ds.sparql"),
					readFileAsString("thumbnail-applicable-stale-ds.sparql"),
					readFileAsString("thumbnail-applicable-no-surrogate-ds.sparql"),
					readFileAsString("thumbnail-applicable-stale-surrogate-ds.sparql"));

			this.applicableNoDSQueries = Arrays.asList(this.isApplicableQueries.get(0), this.isApplicableQueries.get(2));
			this.applicableStaleDSQueries = Arrays
					.asList(this.isApplicableQueries.get(1), this.isApplicableQueries.get(3));

		} catch (IOException e) {
			LOG.error("Failed to read service query", e);
		}
	}

	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		String action = message.getQualifiedAction();
		// Shortcuts based on the particular message received
		// If the message indicates the target was just ingested, then we only need to check if the thumb DS exists
		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return this.askQueries(this.applicableNoDSQueries, message);
		// If a datastream was modified then check to see if the thumbs are stale
		if (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
				|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action))
			return this.askQueries(this.applicableStaleDSQueries, message);

		return super.isApplicable(message);
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

	@Override
	public String getName() {
		return enhancementName;
	}
}
