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
/**
 * 
 */

package edu.unc.lib.dl.ws;

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.FixityReplicationObject;
import edu.unc.lib.dl.service.FixityReplicationService;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class FixityReplicationEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());

	private FixityReplicationService fixityReplicationService;

	@PayloadRoot(localPart = Constants.FIXITY_REPLICATION_OBJECT, namespace = Constants.NAMESPACE)
	public FixityReplicationObject fixityReplication(FixityReplicationObject request) {

		return fixityReplicationService.fixityReplication(request);
	}

	public FixityReplicationService getFixityReplicationService() {
		return fixityReplicationService;
	}

	public void setFixityReplicationService(FixityReplicationService fixityReplicationService) {
		this.fixityReplicationService = fixityReplicationService;
	}
}