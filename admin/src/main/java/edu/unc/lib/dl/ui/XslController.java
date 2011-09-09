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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.UtilityMethods;

/**
 * 
 * 
 */
public class XslController extends SimpleFormController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private UiWebService uiWebService;
	private String baseUrl;
	private String repositoryUrl;
	private String repositoryUrlNoTrailing;
	private String collectionUrl;
	private String forwardUrl;

	/*
	  NOTE: To debug XsltView errors, try log4j.logger.org.springframework=DEBUG
	  
	  i.e.: 
	  	2010-04-01 05:59:30,653 DEBUG [org.springframework.web.servlet.DispatcherServlet] - <Could not complete request>
		; SystemID: http://cdr.lib.unc.edu/xsl/folder.xsl; Line#: 33; Column#: -1
		net.sf.saxon.trans.DynamicError: net.sf.saxon.trans.DynamicError: org.xml.sax.SAXParseException: The content of elements must consist of well-formed character data or markup.
	 */
	
	
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map model = new HashMap();
		Source s = null;
		String formName = "item";
		boolean hasDC = false;
		boolean hasMODS = false;
		boolean hasCONTENTS = false;
		

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=utf-8");

		model.put("orderchildren", "0"); // this parameter is required by
		// folder.xsl

		Id id = uiWebService.getIdFromRequest(request, "test");

		// Send queries for non-existent pages to collections page
		if (id == null) {
			logger.debug("invalid request: " + request.getRequestURI());
			return new ModelAndView("redirect:/" + forwardUrl, "collections", model);
		}

		// Get the url for the requested item
		IrUrlInfo irUrlInfo = new IrUrlInfo();
		UtilityMethods.populateIrUrlInfo(irUrlInfo, request);

		if (uiWebService.isContainer(irUrlInfo)) {
			formName = "folder";
		}

		// get datastreams list to check for DC and MODS and MD_CONTENTS
		ListDatastreamsResponse listDatastreamsResponse = uiWebService.getDatastreams(id.getPid(), "test");

		for (DatastreamDef def : listDatastreamsResponse.getDatastreamDef()) {
			logger.debug("pid: " + id.getPid() + " datastream: " + def.getID() + " label: " + def.getLabel());

			if (Constants.MD_DC.equals(def.getID()))
				hasDC = true;
			if (Constants.MD_DESCRIPTIVE.equals(def.getID()))
				hasMODS = true;
			if (Constants.MD_CONTENTS.equals(def.getID()))
				hasCONTENTS = true;
		}

		logger.debug("pid: " + id.getPid() + " hasDC: " + hasDC + " hasMODS: " + hasMODS + " hasCONTENTS: " + hasCONTENTS);

		// TODO: get title from Solr
		if (hasDC) {
			// get title from DC
			DataResponse dc = uiWebService.getDataFromIrUrlInfo(irUrlInfo, "test");

			if (dc != null) {
				String dcStr = new String(dc.getDissemination().getStream());

				try {
					Document d = new SAXBuilder().build(new ByteArrayInputStream(dcStr.getBytes()));

					Element e = d.getRootElement();

					List<Element> children = (List<Element>) e.getChildren();

					for (Element child : children) {
						if (Constants.DC_TITLE.equals(child.getName())) {
							model.put("title", child.getText());
							logger.debug("using dc:title: " + child.getText());
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				logger.debug("Dublin Core is null for " + request.getRequestURI());
			}
		}

		if (hasMODS) {
			DataResponse dataResponse = uiWebService.getModsFromRequest(request, "test");
			try {
				if (dataResponse != null) {
					s = new StreamSource(new ByteArrayInputStream(dataResponse.getDissemination().getStream()));

					model.put("xml", s);
				}
			} catch (NullPointerException e) { // requested item DNE
				e.printStackTrace();
			}
		} else if (hasDC) { // no MODS for item, get DC
			DataResponse dc = uiWebService.getDataFromIrUrlInfo(irUrlInfo, "test");

			s = new StreamSource(new ByteArrayInputStream(dc.getDissemination().getStream()));

			logger.debug("DC: " + new String(dc.getDissemination().getStream()));

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

		// folder XSL needs to know if it is a collection or a folder
		if (pathInfoResponse.getPaths().size() == 3) {
			model.put("type", "collection");
		} else {
			model.put("type", formName);
		}

		// see if we need to show overview for collection
		String temp = (String) request.getParameter("overview");
		if (temp != null && "true".equals(temp)) {
			model.put("overview", "true");
		}

		// get data for overview for items and folders
		if (("item".equals(formName)) || ("folder".equals(formName))) {
			OverviewDataResponse overviewData = uiWebService.getSupplimentalOverviewData(id.getPid(), false);

			model.put("lastUpdated", overviewData.getDate());
			if (overviewData.getFiles() != null) {
				model.put("numberOfFiles", overviewData.getFiles());
			}
		}

		// get datastreams
		ListDatastreamsResponse datastreamResponse = uiWebService.getDatastreams(id.getPid(), "test");

		List<DatastreamDef> datastreamList = datastreamResponse.getDatastreamDef();

		List<DatastreamDef> userDatastreamList = new ArrayList<DatastreamDef>();

		for (DatastreamDef datastream : datastreamList) {
			if (datastream.getID().startsWith(Constants.USER_DATA_PREFIX)) {
				userDatastreamList.add(datastream);
			}
		}

		String stableUrl = request.getRequestURL().toString();
		String dataUrl = stableUrl.replaceAll(Constants.IR_PREFIX, Constants.DATA_PREFIX);
		String reportUrl = stableUrl.replaceAll(Constants.IR_PREFIX, Constants.DATASTREAM_REPORT_PREFIX);

		if (datastreamList.size() > 0) {
			StringBuffer datastreams = new StringBuffer(
					"<table id=\"index\" style=\"margin-top: 0pt;\" xmlns:mods=\"http://www.loc.gov/mods/v3\" align=\"left\"><tbody><tr><th>Data</th><th>Report</th><th>Type</th></tr>");
			for (DatastreamDef datastream : userDatastreamList) {
				if ("folder".equals(formName)) { // for (disks) with ISOs
					datastreams.append(buildDatastreamTableRow(datastream, null, dataUrl, true));
				} else if ("item".equals(formName)) {
					addFileToModel(model, dataUrl, datastream);
				}
			}

			for (DatastreamDef datastream : datastreamList) {
				if (!datastream.getID().startsWith(Constants.USER_DATA_PREFIX)) {
					datastreams.append(buildDatastreamTableRow(datastream, reportUrl, dataUrl, false));
				}
			}
			datastreams.append("</tbody></table>");

			model.put("datastreams", new StreamSource(new ByteArrayInputStream(datastreams.toString().getBytes())));
		}

		// get children
		if (hasCONTENTS) {

			String parentUrl = request.getRequestURL().toString();
			if (parentUrl.endsWith("/")) {
				parentUrl = parentUrl.substring(0, parentUrl.length() - 1);
			}

			StringBuffer sb = new StringBuffer(256);

			// Build table for children
			if (formName.equals("collections")) {
				sb.append(Constants.UI_COLLECTIONS_TABLE_BEGIN);
			} else {
				sb.append(Constants.UI_FOLDER_TABLE_BEGIN);
			}

			GetChildrenRequest getChildrenRequest = new GetChildrenRequest();

			getChildrenRequest.setBaseUrl(parentUrl);
			getChildrenRequest.setPid(id.getPid());
			getChildrenRequest.setType(formName);

			GetChildrenResponse getChildrenResponse = uiWebService.getChildren(getChildrenRequest);

			int numChildren = 0;

			for (String child : getChildrenResponse.getChild()) {
				numChildren++;

				sb.append(child);
			}

			sb.append(Constants.UI_TABLE_END);

			model.put("children", new StreamSource(new ByteArrayInputStream(sb.toString().getBytes())));

			model.put("orderchildren", numChildren);
		}
		
		if(logger.isDebugEnabled()) {
			Set set = model.keySet();
			Object keys[] = set.toArray();
			
			for(int i = 0; i < keys.length; i++) {
				if(model.get(keys[i]) == null) {
					logger.debug("model key: "+(String) keys[i] +" value is NULL");
				} else if(model.get(keys[i]) instanceof String) {
					logger.debug("model key: "+(String) keys[i] +" value: "+(String) model.get(keys[i]));
				}
			}
		}
		
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

	private String buildDatastreamTableRow(DatastreamDef datastream, String reportUrl, String dataUrl, boolean emphasize) {
		StringBuffer dsBuffer = new StringBuffer(512);

		dsBuffer.append("<tr>");

		if (emphasize) {
			dsBuffer.append("<em>");
		}

		dsBuffer.append("<td>");
		createDatastreamLink(datastream, dsBuffer, dataUrl, true);

		if (emphasize) {
			dsBuffer.append("</em>");
		}

		dsBuffer.append("</td>");

		if (reportUrl != null) {
			dsBuffer.append("<td>");
			createDatastreamLink(datastream, dsBuffer, reportUrl, false);
			dsBuffer.append("</td>");			
		} else {
			dsBuffer.append("<td></td>");
		}

		
		dsBuffer.append("<td>");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));
		dsBuffer.append("</td></tr>");

		return dsBuffer.toString();
	}

	private void createDatastreamLink(DatastreamDef datastream, StringBuffer dsBuffer, String url, boolean isDownload) {
		dsBuffer.append("<a href=\"");

		dsBuffer.append(url);
		dsBuffer.append("?");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getID()));

		if (isDownload) {
			dsBuffer.append("&amp;mt=");
			dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getMIMEType()));
		}
		
		dsBuffer.append("\">");
		dsBuffer.append(StringEscapeUtils.escapeXml(datastream.getLabel()));
		dsBuffer.append("</a>");
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
