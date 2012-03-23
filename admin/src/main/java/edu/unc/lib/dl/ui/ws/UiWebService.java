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
package edu.unc.lib.dl.ui.ws;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.SoapFaultClientException;

import edu.unc.lib.dl.schema.AddToSearchRequest;
import edu.unc.lib.dl.schema.AddToSearchResponse;
import edu.unc.lib.dl.schema.BasicQueryRequest;
import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.schema.CollectionsRequest;
import edu.unc.lib.dl.schema.ContainerQuery;
import edu.unc.lib.dl.schema.CreateCollectionObject;
import edu.unc.lib.dl.schema.DataRequest;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DeleteObjectsRequest;
import edu.unc.lib.dl.schema.DeleteObjectsResponse;
import edu.unc.lib.dl.schema.FedoraDataRequest;
import edu.unc.lib.dl.schema.FedoraDataResponse;
import edu.unc.lib.dl.schema.GetAllCollectionPathsRequest;
import edu.unc.lib.dl.schema.GetAllCollectionPathsResponse;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenRequest;
import edu.unc.lib.dl.schema.GetBreadcrumbsAndChildrenResponse;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.GetChildrenResponse;
import edu.unc.lib.dl.schema.GetFromXmlDbRequest;
import edu.unc.lib.dl.schema.GetFromXmlDbResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IdQueryRequest;
import edu.unc.lib.dl.schema.ImageViewRequest;
import edu.unc.lib.dl.schema.ImageViewResponseList;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ItemInfoRequest;
import edu.unc.lib.dl.schema.ItemInfoResponse;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;
import edu.unc.lib.dl.schema.MediatedSubmitIngestObject;
import edu.unc.lib.dl.schema.MetsSubmitIngestObject;
import edu.unc.lib.dl.schema.FixityReplicationObject;
import edu.unc.lib.dl.schema.MoveObjectRequest;
import edu.unc.lib.dl.schema.MoveObjectResponse;
import edu.unc.lib.dl.schema.OverviewDataRequest;
import edu.unc.lib.dl.schema.OverviewDataResponse;
import edu.unc.lib.dl.schema.PathInfoRequest;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.schema.ReindexSearchRequest;
import edu.unc.lib.dl.schema.ReindexSearchResponse;
import edu.unc.lib.dl.schema.RemoveFromSearchRequest;
import edu.unc.lib.dl.schema.RemoveFromSearchResponse;
import edu.unc.lib.dl.schema.UpdateIngestObject;
import edu.unc.lib.dl.schema.UserGroupDAO;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UtilityMethods;

// TODO: make this a singleton
public class UiWebService extends WebServiceGatewaySupport {
	protected final Log logger = LogFactory.getLog(getClass());
	private DataService dataService;
	private UtilityMethods utilityMethods;

	public BasicQueryResponseList getCollections(String userid) {

		CollectionsRequest request = new CollectionsRequest();

		request.setUserid(userid);

		BasicQueryResponseList response = null;

		response = (BasicQueryResponseList) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	private BasicQueryResponseList query(String query, String restriction, String inside,
			String userid, int start, int rows, String all) {

		BasicQueryRequest request = new BasicQueryRequest();

		request.setQuery(query);
		request.setRestriction(restriction);
		request.setUserid(userid);
		request.setStart(start);
		request.setRows(rows);
		request.setInside(inside);
		request.setAll(all);

		BasicQueryResponseList response = null;

		try {
			response = (BasicQueryResponseList) getWebServiceTemplate()
					.marshalSendAndReceive(this.getDefaultUri(), request);
		} catch (SoapFaultClientException e) {
			response = new BasicQueryResponseList();

			e.printStackTrace();
		}

		return response;
	}

	public BasicQueryResponseList basicQuery(String query,
			String restriction, String inside, String userid, int start, int rows, String all) {
		return query(query, restriction, inside, userid, start, rows, all);
	}

	public OverviewDataResponse getSupplimentalOverviewData(String id, boolean fileCount) {
		OverviewDataRequest request = new OverviewDataRequest();
		OverviewDataResponse response = new OverviewDataResponse();
		request.setId(id);
		request.setFileCount(fileCount);
		
		try {
			response = (OverviewDataResponse) getWebServiceTemplate().marshalSendAndReceive(
					this.getDefaultUri(), request);
		} catch (Exception e) {
			logger.debug("Overview data request failure", e);
		}

		return response;
	}

	
	public Id getIdFromRequest(HttpServletRequest request, String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		// TODO: This could be a problem if things change as UtilityMethods is
		// initialized in dlservice-servlet.xml
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		return getIdFromIrUrlInfo(irUrlInfo, userid);
	}

	public Id getIdFromIrUrlInfo(IrUrlInfo irUrlInfo, String userid) {
		Id response = null;

		IdQueryRequest request = new IdQueryRequest();

		request.setIrUrlInfo(irUrlInfo);
		request.setUserid(userid);

		try {
			response = (Id) getWebServiceTemplate().marshalSendAndReceive(
					this.getDefaultUri(), request);
		} catch (Exception e) {
			logger.debug("bad url requested", e);
		}

		return response;
	}

	public GetChildrenResponse getChildren(GetChildrenRequest request) {

		// Object object =
		// getWebServiceTemplate().marshalSendAndReceive(this.getDefaultUri(),
		// request);
		//		
		// if(object instanceof java.lang.StackTraceElement){
		// logger.debug(((java.lang.StackTraceElement) object).toString());
		// }
		//		
		// return (GetChildrenResponse) object;

		return (GetChildrenResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);
	}

	public List getCollectionPaths(String userid) {
		GetAllCollectionPathsRequest request = new GetAllCollectionPathsRequest();
		request.setUserid(userid);

		GetAllCollectionPathsResponse response = (GetAllCollectionPathsResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response.getPaths();
	}

	public boolean isContainer(IrUrlInfo irUrlInfo) {
		ContainerQuery request = new ContainerQuery();
		request.setIrUrlInfo(irUrlInfo);

		request = (ContainerQuery) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return request.isContainer();
	}

	public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest request) {
		DeleteObjectsResponse deleteObjectsResponse = (DeleteObjectsResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return deleteObjectsResponse;

	}

	public PathInfoResponse getBreadcrumbs(String pid, String userid) {
		PathInfoRequest request = new PathInfoRequest();
		request.setPid(pid);
		request.setUserid(userid);

		PathInfoResponse pathInfoResponse = (PathInfoResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return pathInfoResponse;
	}

	public GetBreadcrumbsAndChildrenResponse getBreadcrumbsAndChildren(
			IrUrlInfo irUrlInfo, String baseUrl, String accessGroups, String pid) {
		GetBreadcrumbsAndChildrenRequest request = new GetBreadcrumbsAndChildrenRequest();

		request.setBaseUrl(baseUrl);
		request.setIrUrlInfo(irUrlInfo);
		request.setAccessGroups(accessGroups);
		request.setPid(pid);

		GetBreadcrumbsAndChildrenResponse response = (GetBreadcrumbsAndChildrenResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public DataResponse getModsFromRequest(HttpServletRequest request,
			String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		// TODO: This could be a problem if things change as UtilityMethods is
		// initialized in dlservice-servlet.xml
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		logger.debug(irUrlInfo.getDecodedUrl());
		logger.debug(irUrlInfo.getFedoraUrl());

		irUrlInfo.setParameters(Constants.DS_PREFIX + Constants.MD_DESCRIPTIVE);

		return getModsFromIrUrlInfo(irUrlInfo, userid);
	}

	public DataResponse getModsFromIrUrlInfo(IrUrlInfo irUrlInfo, String userid) {
		DataRequest request = new DataRequest();
		DataResponse response = null;

		request.setIrUrlInfo(irUrlInfo);
		request.setUserid(userid);

		try {
			response = dataService.getData(request);
		} catch (Exception e) {
		}
		// if(logger.isDebugEnabled())
		// logger.debug(response.getDissemination().getStream());

		return response;
	}

	public ListDatastreamsResponse getDatastreams(String pid, String userid) {

		return dataService.getDatastreams(pid, userid);
	}

	public FedoraDataResponse getFedoraDataUrlFromRequest(
			HttpServletRequest request, String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		FedoraDataRequest fedoraDataRequest = new FedoraDataRequest();
		fedoraDataRequest.setIrUrlInfo(irUrlInfo);
		fedoraDataRequest.setUserid(userid);

		FedoraDataResponse response = null;

		try {
			response = (FedoraDataResponse) getWebServiceTemplate()
					.marshalSendAndReceive(this.getDefaultUri(),
							fedoraDataRequest);
		} catch (Exception e) {
			logger.debug("bad data url", e);
		}

		return response;
	}

	public DataResponse getDataFromRequest(HttpServletRequest request,
			String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		return getDataFromIrUrlInfo(irUrlInfo, userid);
	}

	public DataResponse getDataFromIrUrlInfo(IrUrlInfo irUrlInfo, String userid) {
		DataRequest request = new DataRequest();
		DataResponse response = null;

		request.setIrUrlInfo(irUrlInfo);
		request.setUserid(userid);

		// TODO: restore encapsulation
		try {
			response = dataService.getData(request);
		} catch (Exception e) {
		}

		return response;
	}

	public DataResponse getDataFromPid(String pid, String disseminator,
			String userid) {
		DataResponse response = null;

		// TODO: restore encapsulation
		try {
			response = dataService.getData(pid, disseminator);
		} catch (Exception e) {
		}

		return response;
	}

	public void reindexSearch(String userid) {

		ReindexSearchRequest searchRequest = new ReindexSearchRequest();
		searchRequest.setUserid(userid);
		getWebServiceTemplate().marshalSendAndReceive(this.getDefaultUri(),
				searchRequest);
	}

	public void addToSearchFromIrUrlInfo(IrUrlInfo irUrlInfo, String userid) {
		// testing
		Id id = getIdFromIrUrlInfo(irUrlInfo, userid);

		if (logger.isDebugEnabled())
			logger.debug(id.getPid());

		AddToSearchRequest searchRequest = new AddToSearchRequest();
		searchRequest.getPid().add(id.getPid());
		getWebServiceTemplate().marshalSendAndReceive(this.getDefaultUri(),
				searchRequest);
	}

	public void removeFromSearchFromIrUrlInfo(IrUrlInfo irUrlInfo, String userid) {
		// testing
		Id id = getIdFromIrUrlInfo(irUrlInfo, userid);

		if (logger.isDebugEnabled())
			logger.debug(id.getPid());

		RemoveFromSearchRequest searchRequest = new RemoveFromSearchRequest();
		searchRequest.getPid().add(id.getPid());
		getWebServiceTemplate().marshalSendAndReceive(this.getDefaultUri(),
				searchRequest);
	}

	public ImageViewResponseList getImageViewFromRequest(
			HttpServletRequest request, String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		// TODO: This could be a problem if things change as UtilityMethods is
		// initialized in dlservice-servlet.xml
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		return getImageViewFromIrUrlInfo(irUrlInfo, userid);
	}

	public ItemInfoResponse getItemInfoFromIrUrlInfo(IrUrlInfo irUrlInfo,
			String userid) {
		ItemInfoRequest request = new ItemInfoRequest();

		request.setIrUrlInfo(irUrlInfo);

		ItemInfoResponse response = (ItemInfoResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public ItemInfoResponse getItemInfoFromRequest(HttpServletRequest request,
			String userid) {
		IrUrlInfo irUrlInfo = new IrUrlInfo();

		// TODO: This could be a problem if things change as UtilityMethods is
		// initialized in dlservice-servlet.xml
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		return getItemInfoFromIrUrlInfo(irUrlInfo, userid);
	}

	public ImageViewResponseList getImageViewFromIrUrlInfo(IrUrlInfo irUrlInfo,
			String userid) {
		ImageViewRequest request = new ImageViewRequest();

		request.setIrUrlInfo(irUrlInfo);

		ImageViewResponseList response = (ImageViewResponseList) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public GetFromXmlDbResponse queryXmlDb(HttpServletRequest request,
			String userid) {

		GetFromXmlDbResponse response;

		String query = request.getParameter("query");

		if (logger.isDebugEnabled())
			logger.debug("'" + query + "'");

		// Shouldn't send query on to be processed
		if ((query == null) || (query.equals(""))) {
			response = new GetFromXmlDbResponse();

			response.setResponse(Constants.XML_DB_ERROR_START
					+ Constants.XML_DB_NO_QUERY_TEXT
					+ Constants.XML_DB_ERROR_END);
		} else if (query.indexOf(Constants.XML_DB_XUPDATE) != -1) {
			response = new GetFromXmlDbResponse();

			response.setResponse(Constants.XML_DB_ERROR_START
					+ Constants.XML_DB_BAD_QUERY_TEXT
					+ Constants.XML_DB_CDATA_START + query
					+ Constants.XML_DB_CDATA_END + Constants.XML_DB_ERROR_END);
		} else {
			GetFromXmlDbRequest xmlDbRequest = new GetFromXmlDbRequest();

			xmlDbRequest.setQuery(query);

			try {
				response = (GetFromXmlDbResponse) getWebServiceTemplate()
						.marshalSendAndReceive(this.getDefaultUri(),
								xmlDbRequest);

				if ((response == null) || (response.getResponse() == null)) {
					response = new GetFromXmlDbResponse();

					response.setResponse(Constants.XML_DB_ERROR_START
							+ Constants.XML_DB_NO_QUERY_RESPONSE
							+ Constants.XML_DB_CDATA_START + query
							+ Constants.XML_DB_CDATA_END
							+ Constants.XML_DB_ERROR_END);
				}

			} catch (Exception e) {
				response = new GetFromXmlDbResponse();

				response.setResponse(Constants.XML_DB_ERROR_START
						+ Constants.XML_DB_CDATA_START + e.getMessage()
						+ Constants.XML_DB_CDATA_END
						+ Constants.XML_DB_ERROR_END);

				e.printStackTrace();
			}
		}

		return response;
	}

	public UserGroupDAO userGroupOperation(UserGroupDAO request) {

		UserGroupDAO response = (UserGroupDAO) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public UpdateIngestObject update(UpdateIngestObject request) {

		UpdateIngestObject response = (UpdateIngestObject) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public CreateCollectionObject createCollection(
			CreateCollectionObject request) {

		CreateCollectionObject response = (CreateCollectionObject) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public MediatedSubmitIngestObject mediatedSubmit(
			MediatedSubmitIngestObject request) {

		MediatedSubmitIngestObject response = (MediatedSubmitIngestObject) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public MetsSubmitIngestObject metsSubmit(MetsSubmitIngestObject request) {

		MetsSubmitIngestObject response = (MetsSubmitIngestObject) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public FixityReplicationObject fixityReplication(FixityReplicationObject request) {

		FixityReplicationObject response = (FixityReplicationObject) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}
	
	public MoveObjectResponse moveObjects(MoveObjectRequest request) {

		MoveObjectResponse response = (MoveObjectResponse) getWebServiceTemplate()
				.marshalSendAndReceive(this.getDefaultUri(), request);

		return response;
	}

	public String getFedoraUrlFromPid(String entry) {
		return utilityMethods.getFedoraUrlFromPid(entry);
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}

	public UtilityMethods getUtilityMethods() {
		return utilityMethods;
	}

	public void setUtilityMethods(UtilityMethods utilityMethods) {
		this.utilityMethods = utilityMethods;
	}
}
