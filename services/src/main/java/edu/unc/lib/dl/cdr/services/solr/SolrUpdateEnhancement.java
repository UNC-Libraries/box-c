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

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.text.FullTextEnhancementService;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Enhancement issues solr update messages for items that have been modified by the service stack.
 * 
 * @author bbpennel 
 */
public class SolrUpdateEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateEnhancement.class);
	SolrUpdateEnhancementService service = null;
	EnhancementMessage message;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called Solr update service for " + pid.getPid());
		
		IndexingActionType action = IndexingActionType.ADD;
		
		//Perform a single item update
		if (message.getCompletedServices().contains(FullTextEnhancementService.class.getName())) {
			action = IndexingActionType.UPDATE_FULL_TEXT;
		} else if (message.getCompletedServices().contains(TechnicalMetadataEnhancementService.class.getName())
				|| message.getCompletedServices().contains(ImageEnhancementService.class.getName())
				|| message.getCompletedServices().contains(ThumbnailEnhancementService.class.getName())) {
			action = IndexingActionType.UPDATE_DATASTREAMS;
		}
		
		SolrUpdateRequest updateRequest = new SolrUpdateRequest(pid.getPid(), action);
		service.getMessageDirector().direct(updateRequest);
		
		return result;
	}

	public SolrUpdateEnhancement(SolrUpdateEnhancementService service, EnhancementMessage message) {
		super(message.getPid());
		this.service = service;
		this.message = message;
	}
}
