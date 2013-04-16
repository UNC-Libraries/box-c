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
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Enhancement issues solr update messages for items that have been modified by the service stack.
 * 
 * @author bbpennel 
 */
public class SolrUpdateEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateEnhancement.class);
	SolrUpdateEnhancementService service = null;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called Solr update service for " + pid.getPid());
		
		//Perform a single item update
		service.getMessageDirector().direct(new SolrUpdateRequest(pid.getPid(), IndexingActionType.ADD));
		
		return result;
	}

	public SolrUpdateEnhancement(SolrUpdateEnhancementService service, PID pid) {
		super(pid);
		this.service = service;
	}
}
