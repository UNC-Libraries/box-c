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
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.schema.GetAllCollectionPathsRequest;
import edu.unc.lib.dl.schema.GetAllCollectionPathsResponse;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Endpoint
public class GetAllCollectionPathsEndpoint {

    private final Logger logger = Logger.getLogger(GetAllCollectionPathsEndpoint.class);

	private TripleStoreQueryService tripleStoreQueryService;

	@PayloadRoot(localPart = Constants.GET_ALL_COLLECTION_PATHS_REQUEST, namespace = Constants.NAMESPACE)
    public GetAllCollectionPathsResponse getSearchResult(
	    GetAllCollectionPathsRequest getPathsRequest) {
		
		List<String> temp = tripleStoreQueryService.fetchAllCollectionPaths();
	
		GetAllCollectionPathsResponse response = new GetAllCollectionPathsResponse();
		
		response.getPaths().addAll(temp);
		
	return response;
}


    public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
