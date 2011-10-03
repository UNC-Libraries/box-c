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
package edu.unc.lib.dl.ui.service;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;

/**
 * Retrieves metadata for an objects full record description.
 * @author bbpennel
 */
public class FullObjectMetadataFactory {
	private static final Logger LOG = LoggerFactory.getLogger(FullObjectMetadataFactory.class);
	
	private static FedoraDataService dataService;
	
	public FullObjectMetadataFactory(){
	}
	
	public static Document getFoxmlViewXML(SimpleIdRequest idRequest){
		try {
			return dataService.getFoxmlViewXML(idRequest.getId());
		} catch (Exception e){
			LOG.error("Failed to retrieve XML object for " + idRequest.getId(), e);
		}
		return null;
	}

	public FedoraDataService getDataService() {
		return dataService;
	}

	public void setDataService(FedoraDataService dataService) {
		FullObjectMetadataFactory.dataService = dataService;
	}
}


