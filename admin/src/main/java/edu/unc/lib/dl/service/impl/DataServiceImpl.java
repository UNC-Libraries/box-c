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
package edu.unc.lib.dl.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.dom.DOMResult;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.CommonsHttpConnection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.unc.lib.dl.schema.DataRequest;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DatastreamDef;
import edu.unc.lib.dl.schema.FedoraDataRequest;
import edu.unc.lib.dl.schema.FedoraDataResponse;
import edu.unc.lib.dl.schema.GetDatastreamDissemination;
import edu.unc.lib.dl.schema.GetDatastreamDisseminationResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.ImageListResponse;
import edu.unc.lib.dl.schema.ImageViewRequest;
import edu.unc.lib.dl.schema.ImageViewResponseList;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ListDatastreams;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;
import edu.unc.lib.dl.service.ConstituentService;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.service.GatherRelsExtInformationService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UtilityMethods;

public class DataServiceImpl extends WebServiceGatewaySupport implements
		DataService {
	private final Logger logger = Logger.getLogger(getClass());
	private IdService idService;
	private GatherRelsExtInformationService gatherRelsExtInformationService;
	private ConstituentService constituentService;
	private String baseHostUrl;
	private UtilityMethods utilityMethods;

	public ListDatastreamsResponse getDatastreams(String pid, String userid) {
		ListDatastreams ld = new ListDatastreams();
		ld.setPid(pid);
		ld.setAsOfDateTime(null);

		WebServiceMessageCallback wsmc = new WebServiceMessageCallback() {
			public void doWithMessage(WebServiceMessage message) {
				((SoapMessage) message)
						.setSoapAction("http://www.fedora.info/definitions/1/0/api/#listDatastreams");
				
				SoapHeader soapHeader = ((SoapMessage)message).getSoapHeader();
				DOMResult xmlHeader = (DOMResult) soapHeader.getResult();
				Node headerNode = xmlHeader.getNode();	
				Element xmlHeaderElement = headerNode.getOwnerDocument().createElementNS("http://cdr.lib.unc.edu", "CDR");
				xmlHeaderElement.setPrefix("cdr");

				Element idConsumidorElement = headerNode.getOwnerDocument().createElementNS("", "groups");
				xmlHeaderElement.appendChild(idConsumidorElement);
				idConsumidorElement.appendChild(headerNode.getOwnerDocument().createTextNode("testgroup"));		
				headerNode.appendChild(xmlHeaderElement);


			}
		};
		
		ListDatastreamsResponse response = (ListDatastreamsResponse) getWebServiceTemplate()
				.marshalSendAndReceive(ld, wsmc);

		return response;
	}

	public ImageListResponse getImageDatastreamIds(String pid) {
		ImageListResponse result = new ImageListResponse();

		if (logger.isDebugEnabled())
			logger.debug("getDatastreams pid: " + pid);

		result.setPid(pid);

		WebServiceMessageCallback wsmc = new WebServiceMessageCallback() {
			public void doWithMessage(WebServiceMessage message) {
				((SoapMessage) message)
						.setSoapAction("http://www.fedora.info/definitions/1/0/api/#listDatastreams");
				
//				CommonsHttpConnection connection = (CommonsHttpConnection) TransportContextHolder.getTransportContext().getConnection();
//				PostMethod postMethod = connection.getPostMethod();
				
//				RequestEntity requestEntity = new RequestEntity();
//				postMethod.setRequestEntity(requestEntity)
			}
		};

		ListDatastreams ld = new ListDatastreams();
		ld.setPid(pid);
		ld.setAsOfDateTime(null);

		ListDatastreamsResponse response = (ListDatastreamsResponse) getWebServiceTemplate()
				.marshalSendAndReceive(ld, wsmc);

		if (logger.isDebugEnabled())
			logger.debug("listDatastreams after WS call");

		List<DatastreamDef> datastreams = response.getDatastreamDef();

		for (DatastreamDef datastream : datastreams) {
			if (datastream.getMIMEType().startsWith("image")) {

				result.getImages().add(datastream.getID());
			} else if (datastream.getMIMEType().equals("application/pdf")) {
				result.getPdfs().add(datastream.getID());
			}
		}

		if (logger.isDebugEnabled())
			logger.debug("getDatastreams exit");

		return result;
	}

	public DataResponse getData(DataRequest dataRequest) {
		Id id = idService.getId(dataRequest.getIrUrlInfo());

		return getData(id.getPid(), getDatastream(dataRequest.getIrUrlInfo()
				.getParameters()));
	}

	public DataResponse getData(String pid, String disseminator) {
		DataResponse result = new DataResponse();
		if (logger.isDebugEnabled())
			logger.debug("getData pid: " + pid);
		if (logger.isDebugEnabled())
			logger.debug("getData disseminator: " + disseminator);

		WebServiceMessageCallback wsmc = new WebServiceMessageCallback() {
			public void doWithMessage(WebServiceMessage message) {
				((SoapMessage) message)
						.setSoapAction("http://www.fedora.info/definitions/1/0/api/#getDatastreamDissemination");
			}
		};

		GetDatastreamDissemination gd = new GetDatastreamDissemination();
		gd.setPid(pid);
		gd.setAsOfDateTime(null);
		gd.setDsID(disseminator);

		GetDatastreamDisseminationResponse response = (GetDatastreamDisseminationResponse) getWebServiceTemplate()
				.marshalSendAndReceive(gd, wsmc);

		if (logger.isDebugEnabled())
			logger.debug("getData after WS call");

		if (logger.isDebugEnabled())
			logger.debug("getData mimetype: "
					+ response.getDissemination().getMIMEType());

		result.setDissemination(response.getDissemination());

		if (logger.isDebugEnabled())
			logger.debug("getData exit");

		return result;
	}

	private String getDatastream(String params) {
		String result = null;

		String[] pairs = params.split("&");

		for (int i = 0; i < pairs.length; i++) {
			if (pairs[i].startsWith(Constants.DS_PREFIX)) {
				result = pairs[i].substring(Constants.DS_PREFIX.length());
				break;
			}
		}

		if (result == null) {
			result = "DC";
		}

		return result;
	}

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public ImageViewResponseList getImageViewList(
			ImageViewRequest imageViewRequest) {
		return getImageViewList(imageViewRequest.getIrUrlInfo());
	}

	public ImageViewResponseList getImageViewList(IrUrlInfo irUrlInfo) {
		ImageViewResponseList result = new ImageViewResponseList();

		String stableUrl = irUrlInfo.getDecodedUrl();

		Map map = gatherRelsExtInformationService
				.getAllFromIrUrlInfo(irUrlInfo);

		result.setStableUrl(utilityMethods.getItemInfoUrlFromPid((String) map
				.get(Constants.PID)));
		result.setTitle(getTitle(map));

		// get parent PDF if any (there is similar code in the add to search
		// index code
		ImageListResponse imageListResponse = getImageDatastreamIds((String) map
				.get(Constants.PID));
		if (imageListResponse.getPdfs().size() > 0) {
			StringBuilder url = new StringBuilder(256);
			url.append(baseHostUrl).append(Constants.DATA_PREFIX).append(
					idService.getUrlFromPid((String) map.get(Constants.PID)))
					.append('?').append(Constants.DS_PREFIX).append(
							imageListResponse.getPdfs().get(0));
			result.getPdfs().add(url.toString());
		}

		List<String> childPids = constituentService.getOrderedConstituentPids(
				(String) map.get(Constants.PID), idService
						.getPidFromRiPid((String) map
								.get(Constants.RI_FIRST_CONSTITUENT)),
				idService.getPidFromRiPid((String) map
						.get(Constants.RI_LAST_CONSTITUENT)));

		// for each child, add an image to the result
		for (String pid : childPids) {
			ImageListResponse response = getImageDatastreamIds(pid);

			String tempUrl = utilityMethods.getDataUrlFromPid(pid);

			List<String> temp = getImageDatastreams(response.getImages(),
					tempUrl);

			if (temp.size() > 0) {
				result.getImages().add(temp.get(0));
			}
		}

		return result;
	}

	private List<String> getImageDatastreams(List datastreams, String stableUrl) {
		List<String> result = new ArrayList(datastreams.size());

		String dataUrl = stableUrl.replaceAll(Constants.IMAGE_VIEW_PREFIX,
				Constants.DATA_PREFIX);

		Object[] ds = datastreams.toArray();

		for (int i = 0; i < ds.length; i++) {
			StringBuffer temp = new StringBuffer(256);
			String datastream = (String) ds[i];
			String dsName = datastream
					.substring(datastream.lastIndexOf('/') + 1);

			temp.append("<img src=\"").append(dataUrl).append("?ds=").append(
					dsName).append("\"/>");
			result.add(temp.toString());

			if (logger.isDebugEnabled())
				logger.debug(temp.toString());
		}

		return result;
	}

	private String getTitle(Map map) {
		if (map.get(Constants.RI_TITLE) != null) {
			return getString(map.get(Constants.RI_TITLE));
		}

		return getString(map.get(Constants.RI_LABEL));
	}

	private String getString(Object object) {
		if (object == null) {
			return "";
		}

		return (String) object;
	}

	public void setGatherRelsExtInformationService(
			GatherRelsExtInformationService gatherRelsExtInformationService) {
		this.gatherRelsExtInformationService = gatherRelsExtInformationService;
	}

	public void setConstituentService(ConstituentService constituentService) {
		this.constituentService = constituentService;
	}

	public void setBaseHostUrl(String baseHostUrl) {
		this.baseHostUrl = baseHostUrl;
	}

	public void setUtilityMethods(UtilityMethods utilityMethods) {
		this.utilityMethods = utilityMethods;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.DataService#getFedoraDataUrl(edu.unc.lib.dl.schema.FedoraDataRequest)
	 */
	public FedoraDataResponse getFedoraDataUrl(
			FedoraDataRequest fedoraDataRequest) {
		FedoraDataResponse response = new FedoraDataResponse();
		String temp;

		response
				.setFedoraDataUrl(utilityMethods
						.getFedoraDataUrlFromIrUrlInfo(fedoraDataRequest
								.getIrUrlInfo()));

		temp = fedoraDataRequest.getIrUrlInfo().getParameters();

		if ((temp != null) && (temp.contains("&mt="))) {
			int index = temp.indexOf("&mt=");
			index = temp.indexOf("=", index);
			response.setMimeType(temp.substring(index + 1, temp
					.length()));
		} else {
			response.setMimeType("DC");
		}

		return response;
	}
}
