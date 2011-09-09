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
package edu.unc.lib.dl.ws;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.AddToSearchRequest;
import edu.unc.lib.dl.schema.AddToSearchResponse;
import edu.unc.lib.dl.service.GatherRelsExtInformationService;
import edu.unc.lib.dl.service.SearchService;
import edu.unc.lib.dl.service.XmlDbService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class AddToSearchEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());
	private GatherRelsExtInformationService gatherRelsExtInformationService;
	private SearchService searchService;
	private XmlDbService xmlDbService;

	@PayloadRoot(localPart = Constants.ADD_TO_SEARCH_REQUEST, namespace = Constants.NAMESPACE)
	public AddToSearchResponse addToSearch(AddToSearchRequest addRequest) {
		AddToSearchResponse response = new AddToSearchResponse();

		List<String> pids = addRequest.getPid();

		response.getPid().addAll(pids);
		response.setResponse("Submitted");

		searchService.addToSearch(pids);

		return response;
	}

	public void setGatherRelsExtInformationService(
			GatherRelsExtInformationService gatherRelsExtInformationService) {
		this.gatherRelsExtInformationService = gatherRelsExtInformationService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setXmlDbService(XmlDbService xmlDbService) {
		this.xmlDbService = xmlDbService;
	}

}
