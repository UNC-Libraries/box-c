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
package edu.unc.lib.dl.ui;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DatastreamDef;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.GetChildrenResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;
import edu.unc.lib.dl.schema.OverviewDataResponse;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UtilityMethods;

/**
 * 
 * 
 */
public class XmlDatastreamController extends SimpleFormController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private UiWebService uiWebService;
	private String baseUrl;
	private String repositoryUrl;
	private String repositoryUrlNoTrailing;
	private String collectionUrl;
	private String forwardUrl;

	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map model = new HashMap();
		Source s = null;
		String formName = null;
		
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=utf-8");

		Id id = uiWebService.getIdFromRequest(request, "test");

		// Send queries for non-existent pages to collections page
		if (id == null) {
			logger.debug("invalid request: " + request.getRequestURI());
			return new ModelAndView("redirect:/" + forwardUrl, "collections", model);
		}

		
		// http://localhost/ir/xd/Collections/filetype_test?MD_EVENTS
		logger.debug("XD id: "+id.getPid());
		logger.debug("XD request: "+request.getRequestURI());
		logger.debug("XD query: "+request.getQueryString());

		formName = request.getQueryString();
		
		// Get the url for the requested item
		IrUrlInfo irUrlInfo = new IrUrlInfo();
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		DataResponse dataResponse = uiWebService.getDataFromPid(id.getPid(), request.getQueryString(), "test");

		if ((dataResponse != null) && (dataResponse.getDissemination() != null)) {
			s = new StreamSource(new ByteArrayInputStream(dataResponse.getDissemination().getStream()));
			model.put("xml", s);
		}

		// first two should always be / and /Collections
		PathInfoResponse pathInfoResponse = uiWebService.getBreadcrumbs(id.getPid(), "test");

		for (int i = 0; i < pathInfoResponse.getPaths().size(); i++) {
			logger.debug("breadcrumb label (" + i + "): " + pathInfoResponse.getPaths().get(i).getLabel());
		}

		if ((pathInfoResponse.getPaths().size() - 1) > 0) {

			model.put("parent", repositoryUrlNoTrailing + pathInfoResponse.getPaths().get(pathInfoResponse.getPaths().size() - 1).getPath());

			StringBuffer breadcrumbs = new StringBuffer();
			breadcrumbs.append("<p class=\"breadcrumbs\">");

			breadcrumbs.append("<a href=\"");
			breadcrumbs.append(baseUrl);
			breadcrumbs.append("/\">Home</a> > ");

			for (int i = 1; i < pathInfoResponse.getPaths().size(); i++) {
				if (i < pathInfoResponse.getPaths().size() - 1) {
					breadcrumbs.append("<a href=\"");
					breadcrumbs.append(repositoryUrl);
					breadcrumbs.append(pathInfoResponse.getPaths().get(i).getPath());
					breadcrumbs.append("\">");
				} else {
					breadcrumbs.append("<span class=\"expandableTitle\">");
				}
				
				breadcrumbs.append(StringEscapeUtils.escapeXml(pathInfoResponse.getPaths().get(i).getLabel()));

				logger.debug("breadcrumb label (" + i + "): " + pathInfoResponse.getPaths().get(i).getLabel());

				if (i < pathInfoResponse.getPaths().size() - 1) {
					breadcrumbs.append("</a>");
					breadcrumbs.append(" &gt; ");
				} else {
					breadcrumbs.append("</span>");
				}

				if (i == 2) {
					StringBuffer temp = new StringBuffer(128);
					temp.append(repositoryUrl);
					temp.append(pathInfoResponse.getPaths().get(i).getPath());

					logger.debug(temp.toString());

					model.put("collectionUrl", temp.toString());
					model.put("collectionName", pathInfoResponse.getPaths().get(i).getLabel());
				}
			}
			breadcrumbs.append("</p>");

			model.put("breadcrumbs", new StreamSource(new ByteArrayInputStream(breadcrumbs.toString().getBytes())));

			logger.debug(breadcrumbs.toString());
		}

		if (pathInfoResponse.getPaths().size() == 2) {
			formName = "collections";
		}

		model.put("generate-html", "yes");
		return new ModelAndView(formName, model);
	}

	private void addFileToModel(Map model, String dataUrl, DatastreamDef datastream) {
		StringBuffer dsBuffer = new StringBuffer(128);
		String label = datastream.getLabel();

		dsBuffer.append(dataUrl);
		dsBuffer.append("?");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getID()));
		dsBuffer.append("&");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));

		model.put("fileUrl", dsBuffer.toString());

		dsBuffer.delete(0, dsBuffer.length());

		dsBuffer.append(StringEscapeUtils.escapeXml(label));

		if ((label != null) && (!label.contains("("))) {
			dsBuffer.append(" (");
			dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));
			dsBuffer.append(")");
		}

		model.put("fileName", dsBuffer.toString());
	}

	private String buildDatastreamTableRow(DatastreamDef datastream, String dataUrl, boolean emphasize) {
		StringBuffer dsBuffer = new StringBuffer(256);

		dsBuffer.append("<tr><td>");

		if (emphasize) {
			dsBuffer.append("<em>");
		}

		dsBuffer.append("<a href=\"");

		dsBuffer.append(dataUrl);
		dsBuffer.append("?");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getID()));
		dsBuffer.append("&amp;");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));
		dsBuffer.append("\">");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getLabel()));
		dsBuffer.append("</a>");

		if (emphasize) {
			dsBuffer.append("</em>");
		}

		dsBuffer.append("</td>");
		dsBuffer.append("<td>");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));
		dsBuffer.append("</td></tr>");

		return dsBuffer.toString();
	}


	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
	}

	public String getCollectionUrl() {
		return collectionUrl;
	}

	public void setCollectionUrl(String collectionUrl) {
		this.collectionUrl = collectionUrl;
	}

	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl.substring(0, repositoryUrl.lastIndexOf("/"));
	}

	public String getForwardUrl() {
		return forwardUrl;
	}

	public void setForwardUrl(String forwardUrl) {
		this.forwardUrl = forwardUrl;
	}

	public String getRepositoryUrlNoTrailing() {
		return repositoryUrlNoTrailing;
	}

	public void setRepositoryUrlNoTrailing(String repositoryUrlNoTrailing) {
		this.repositoryUrlNoTrailing = repositoryUrlNoTrailing;
	}
}
