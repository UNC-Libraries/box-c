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

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dl.util.IndexingActionType.ADD;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_DATASTREAMS;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_FULL_TEXT;

import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
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
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
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

	private final SolrSearchService searchService;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called Solr update service for {}", pid.getPidAsString());

		IndexingActionType action = ADD;

		try {
			List<String> completedServices = message.getCompletedServices();
			// Perform a single item update
			if (completedServices.contains(FullTextEnhancementService.class.getName())) {
				action = UPDATE_FULL_TEXT;
			} else if (completedServices.contains(TechnicalMetadataEnhancementService.class.getName())
					|| completedServices.contains(ImageEnhancementService.class.getName())
					|| completedServices.contains(ThumbnailEnhancementService.class.getName())) {

				action = UPDATE_DATASTREAMS;

				// Check if this object is the default web object for another item, and update that item's datastreams if so
				List<PID> dwoFor = service.getTripleStoreQueryService().fetchByPredicateAndLiteral(
						defaultWebObject.toString(), pid);
				if (dwoFor != null && dwoFor.size() > 0) {
					for (PID dwoPID : dwoFor) {
						if (searchService.exists(dwoPID.getPidAsString())) {
							service.getMessageDirector().direct(new SolrUpdateRequest(dwoPID.getPidAsString(), UPDATE_DATASTREAMS));
						}
					}
				}
			}

			long start = System.currentTimeMillis();
			// Make sure the record is in solr before trying to do a partial update
			if (!action.equals(ADD) && !searchService.exists(pid.getPidAsString())) {
				LOG.debug("Partial update for {} is not applicable, reverting to full update", pid.getPidAsString());
				action = ADD;
			}
			LOG.info("Checked for record in {}", (System.currentTimeMillis() - start));
		} catch (SolrServerException e) {
			LOG.error("Failed to check for the existense of {}", pid.getPidAsString(), e);
		}

		SolrUpdateRequest updateRequest = new SolrUpdateRequest(pid.getPidAsString(), action);
		service.getMessageDirector().direct(updateRequest);

		return result;
	}

	public SolrUpdateEnhancement(SolrUpdateEnhancementService service, EnhancementMessage message) {
		super(message.getPid());
		this.service = service;
		this.message = message;
		this.searchService = service.getSolrSearchService();
	}
}
