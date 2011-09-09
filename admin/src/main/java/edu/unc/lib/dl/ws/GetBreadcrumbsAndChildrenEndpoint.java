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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenRequest;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.service.ConstituentService;
import edu.unc.lib.dl.service.SearchService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Endpoint
public class GetBreadcrumbsAndChildrenEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());
	private SearchService searchService;
	private ConstituentService constituentService;
	private TripleStoreQueryService tripleStoreQueryService;

	@PayloadRoot(localPart = Constants.GET_BREADCRUMBS_AND_CHILDREN_REQUEST, namespace = Constants.NAMESPACE)
	public GetBreadcrumbsAndChildrenResponse addToSearch(GetBreadcrumbsAndChildrenRequest request) {
		GetBreadcrumbsAndChildrenResponse response = new GetBreadcrumbsAndChildrenResponse();

		IrUrlInfo irUrlInfo = request.getIrUrlInfo();
		String baseUrl = request.getBaseUrl();
		PID pid = new PID(request.getPid()); // tripleStoreQueryService.fetchByRepositoryPath(irUrlInfo.getFedoraUrl());
		
		// logger.debug("decoded url: "+irUrlInfo.getDecodedUrl()+" fedora url: "+irUrlInfo.getFedoraUrl()+" uri: "+irUrlInfo.getUri()+" url: "+irUrlInfo.getUrl());
		
		PathInfoResponse pathInfoResponse = constituentService.getBreadcrumbs(pid.getPid());
		List<PathInfoDao> list = new ArrayList<PathInfoDao>(pathInfoResponse.getPaths().size());
		
		if ((pathInfoResponse.getPaths().size() - 1) > 0) {
			
			for (int i = 1; i < pathInfoResponse.getPaths().size(); i++) {
				PathInfoDao pidao = new PathInfoDao();

				pidao.setLabel(pathInfoResponse.getPaths().get(i).getLabel());
				pidao.setPid(pathInfoResponse.getPaths().get(i).getPid());
				pidao.setPath(baseUrl + "?id=" + pidao.getPid());
					//	+ pathInfoResponse.getPaths().get(i).getPath());
				
				list.add(pidao);
			}
		}

		
		response.getBreadcrumbs().addAll(list);
				
		List<PathInfoDao> children = searchService.getChildrenFromSolr(pid.getPid(), request.getAccessGroups(), baseUrl);
		
		response.getChildren().addAll(children);
		
		return response;
	}

	
	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public ConstituentService getConstituentService() {
		return constituentService;
	}

	public void setConstituentService(ConstituentService constituentService) {
		this.constituentService = constituentService;
	}


	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}


	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}