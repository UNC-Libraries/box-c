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

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Enhancement service used for construction of jp2 derived images.
 * 
 * @author Gregory Jansen, bbpennel
 */
public class ImageEnhancementService extends AbstractDatastreamEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(ImageEnhancementService.class);
	public static final String enhancementName = "Image Derivative Generation";

	public ImageEnhancementService() {
		super();
	}

	public void init() {
		try {
			this.findCandidatesQueries = Arrays.asList(this.readFileAsString("image-candidates-no-ds.sparql"),
					this.readFileAsString("image-candidates-stale-ds.sparql"));
			for (int i = 0; i < this.findCandidatesQueries.size(); i++) {
				this.findCandidatesQueries.set(
						i,
						String.format(this.findCandidatesQueries.get(i),
								this.tripleStoreQueryService.getResourceIndexModelUri()));
			}

			this.isApplicableQueries = Arrays.asList(readFileAsString("image-applicable-no-ds.sparql"),
					readFileAsString("image-applicable-stale-ds.sparql"));
			this.applicableNoDSQuery = this.isApplicableQueries.get(0);
			this.applicableStaleDSQuery = this.isApplicableQueries.get(1);

			this.findStaleCandidatesQuery = this.readFileAsString("image-stale-candidates.sparql");
			this.lastAppliedQuery = this.readFileAsString("image-last-applied.sparql");
		} catch (IOException e) {
			LOG.error("Failed to read service query", e);
		}
	}
	
	@Override
	public boolean isApplicable(EnhancementMessage message) throws EnhancementException {
		String action = message.getQualifiedAction();
		// Shortcuts based on the particular message received
		// If the message indicates the target was just ingested, then we only need to check if the DS exists
		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return this.askQuery(this.applicableNoDSQuery, message);
		// If a datastream was modified then check to see if the DS is stale
		if (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
				|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action))
			return this.askQuery(this.applicableStaleDSQuery, message);

		return super.isApplicable(message);
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new ImageEnhancement(this, message.getPid());
	}

	@Override
	public String getName() {
		return enhancementName;
	}
}
