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

import edu.unc.lib.dl.schema.AddToXmlDbRequest;
import edu.unc.lib.dl.schema.AddToXmlDbResponse;
import edu.unc.lib.dl.service.XmlDbService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class AddToXmlDbEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());
	private XmlDbService xmlDbService;

	@PayloadRoot(localPart = Constants.ADD_TO_XML_DATABASE_REQUEST, namespace = Constants.NAMESPACE)
	public AddToXmlDbResponse addToXmlDb(AddToXmlDbRequest addRequest) {
		AddToXmlDbResponse response = new AddToXmlDbResponse();

		List<String> pids = addRequest.getPid();

		for (String pid : pids) {

			response.getPid().add(pid);

			if (logger.isDebugEnabled())
				logger.debug(pid);
		}

		xmlDbService.addToXmlDb(pids);

		response.setResponse("All pids added");

		return response;
	}

	public void setXmlDbService(XmlDbService xmlDbService) {
		this.xmlDbService = xmlDbService;
	}

}