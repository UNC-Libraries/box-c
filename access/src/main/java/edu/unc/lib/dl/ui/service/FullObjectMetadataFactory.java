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
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

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
		return getFoxmlViewXML(idRequest, 1);
	}
	
	public static Document getFoxmlViewXML(SimpleIdRequest idRequest, int retries){
		try {
			Document foxml = dataService.getFoxmlViewXML(idRequest.getId());
			for (int i=0; i < retries; i++){
				if (foxml.getRootElement().getContent().size() == 0){
					foxml = dataService.getFoxmlViewXML(idRequest.getId());
				}
			}
			if (foxml.getRootElement().getContent().size() == 0){
				throw new ResourceNotFoundException("Failed to retrieve FOXML for object " + idRequest.getId());
			}
			return foxml;
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


