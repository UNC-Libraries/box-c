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

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.DataRequest;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.FedoraDataRequest;
import edu.unc.lib.dl.schema.FedoraDataResponse;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class GetDataEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());
	private DataService dataService;

	@PayloadRoot(localPart = Constants.GET_DATA_REQUEST, namespace = Constants.NAMESPACE)
	public DataResponse getData(DataRequest dataRequest) {

		return dataService.getData(dataRequest);
	}

	@PayloadRoot(localPart = Constants.GET_FEDORA_DATA_REQUEST, namespace = Constants.NAMESPACE)
	public FedoraDataResponse getData(FedoraDataRequest dataRequest) {

		return dataService.getFedoraDataUrl(dataRequest);
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}
}
