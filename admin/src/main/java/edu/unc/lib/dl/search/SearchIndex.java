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
package edu.unc.lib.dl.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.AddToSearchResponse;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.DatastreamDef;
import edu.unc.lib.dl.schema.ImageListResponse;
import edu.unc.lib.dl.schema.JournalArticleMetadata;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;
import edu.unc.lib.dl.service.ConstituentService;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.service.GatherRelsExtInformationService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.UtilityMethods;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class SearchIndex implements ServletContextAware {
	private final Logger logger = Logger.getLogger(getClass());
	private SolrServer server;
	private String solrUrl;
	private IdService idService;
	private ConstituentService constituentService;
	private GatherRelsExtInformationService gatherRelsExtInformationService;
	private String baseHostUrl;
	private String baseIrUrl;
	private DataService dataService;
	private UtilityMethods utilityMethods;
	private TripleStoreQueryService tripleStoreQueryService;
	private String collectionUrl;
	private String userName;
	private String password;
	private ServletContext servletContext;

	public void clearSearchIndex() {
		try {
			server = new CommonsHttpSolrServer(solrUrl);

			server.deleteByQuery("*:*");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void removeFromSolr(List<String> list) {
		try {
			server = new CommonsHttpSolrServer(solrUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			for (String pid : list) {
				UpdateResponse response = server.deleteById(pid);
				if (logger.isDebugEnabled())
					logger.debug(response.toString());
			}
			UpdateResponse response = server.commit();

			if (logger.isDebugEnabled())
				logger.debug(response.toString());

		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<String> LoadAccessControl() {
		List<String> results = new ArrayList<String>();
		SAXBuilder saxBuilder = new SAXBuilder();
		Document xmlDoc = null;

		try {

			ByteArrayInputStream fstream = (ByteArrayInputStream) servletContext
					.getResourceAsStream("/WEB-INF/uiapp-servlet.xml");
			xmlDoc = saxBuilder.build(fstream);

			Namespace namespace = Namespace.getNamespace("beans",
					"http://www.springframework.org/schema/beans");

			Namespace namespace2 = Namespace.getNamespace("security",
					"http://www.springframework.org/schema/security");

			List<Element> list = xpathQuery(xmlDoc,
					"/beans:beans/security:http/security:intercept-url",
					namespace, namespace2);

			for (Element el : list) { // process results; get entries starting
				// with /ir/info/
				String temp = el.getAttributeValue("pattern");

				if (temp.startsWith("/ir/info/")) {
					// Strip off '/ir/info/' and trailing '**'
					temp = temp.substring(8, temp.indexOf('*'));

					logger.debug(temp);

					// Store rest in list of results
					results.add(temp);
				}
			}

			fstream.close();
		} catch (JDOMException e) {
			logger.error("Could not load access control information");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			logger.error("Could not load access control information");
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			System.err.println("Access control file input error");
			e.printStackTrace();
			return null;
		}

		return results;
	}

	public AddToSearchResponse reindexRepository() {
		Stack<PID> children = new Stack<PID>();
		String errorString = "Error; see log";
		AddToSearchResponse response = new AddToSearchResponse();
		response.setResponse("Success");
		String date = getDisplayDateString(new Date());

		List<String> restrictions = LoadAccessControl();
		if (restrictions == null) { // access control failure of some kind
			response.setResponse("Failure -- access control did not load");
			return response;
		}
		try {
			server = new CommonsHttpSolrServer(solrUrl);
		} catch (MalformedURLException e) {
			response.setResponse(errorString);
			e.printStackTrace();
		}

		PID starterPid = ContentModelHelper.Administrative_PID.REPOSITORY
		.getPID();

		List<PathInfo> childPaths = tripleStoreQueryService.fetchChildPathInfo(starterPid);
		for(PathInfo childPath : childPaths) {
			children.push(childPath.getPid());
		}

		while (!children.empty()) {
			// pop top PID

			PID pid = children.pop();

		try{
			Thread.sleep(500);
		} catch(InterruptedException e) {
			logger.warn("Error reindexing repository", e);
		}
			
			// reindex

			// for each item, add it to Solr
			boolean isRestricted = false;

			if (!allowIndexing(pid.getPid())) {
				logger.debug("Search indexing not allowed for pid: " + pid);

				continue;
			}

			if (restrictions.size() > 0) { // some paths are restricted
				List<PathInfo> pathList = tripleStoreQueryService
						.lookupRepositoryPathInfo(pid);

				PathInfo path = pathList.get(pathList.size() - 1);
				String stableUrl = path.getPath();

				if (!stableUrl.endsWith("/")) { // this will make comparisons
					// work for
					// /Collections/pathname and
					// prevent false matches on
					// /Collections/pathnamemumblemumble
					stableUrl = stableUrl + "/";
				}

				for (String restricted : restrictions) {
					logger.debug("Access Control: restricted path: "
							+ restricted + " stableUrl: " + stableUrl);

					if (stableUrl.startsWith(restricted)) {
						isRestricted = true;
					}
				}
			}

			buildSearchIndex(pid.getPid(), errorString, response, date,
					isRestricted);

			// get children and add to stack
			
			childPaths = tripleStoreQueryService.fetchChildPathInfo(pid);
			for(PathInfo childPath : childPaths) {
				children.push(childPath.getPid());
			}
		}
		
		return response;
	}

	public AddToSearchResponse addToSolr(List<String> pids) {
		String errorString = "Error; see log";
		AddToSearchResponse response = new AddToSearchResponse();
		response.getPid().addAll(pids);
		response.setResponse("Success");
		String date = getDisplayDateString(new Date());

		List<String> restrictions = LoadAccessControl();
		if (restrictions == null) { // access control failure of some kind
			response.setResponse("Failure -- access control did not load");
			return response;
		}
		try {
			server = new CommonsHttpSolrServer(solrUrl);
		} catch (MalformedURLException e) {
			response.setResponse(errorString);
			e.printStackTrace();
		}

		// for each item, add it to Solr
		for (String pid : pids) {
			boolean isRestricted = false;

			if (!allowIndexing(pid)) {
				logger.debug("Search indexing not allowed for pid: " + pid);

				continue;
			}

			if (restrictions.size() > 0) { // some paths are restricted
				PID Pid = new PID(pid);
				List<PathInfo> pathList = tripleStoreQueryService
						.lookupRepositoryPathInfo(Pid);

				PathInfo path = pathList.get(pathList.size() - 1);
				String stableUrl = path.getPath();

				if (!stableUrl.endsWith("/")) { // this will make comparisons
					// work for
					// /Collections/pathname and
					// prevent false matches on
					// /Collections/pathnamemumblemumble
					stableUrl = stableUrl + "/";
				}

				for (String restricted : restrictions) {
					logger.debug("Access Control: restricted path: "
							+ restricted + " stableUrl: " + stableUrl);

					if (stableUrl.startsWith(restricted)) {
						isRestricted = true;
					}
				}
			}

			buildSearchIndex(pid, errorString, response, date, isRestricted);

			isRestricted = false; // reset check
		}

		return response;
	}

	// Solr date processing testing
	// String dateString = list.get(0).getTextTrim();
	// DateTime dateTime =
	// DateTimeUtil.parseISO8601toUTC(dateString);
	// DateTime dateTime = DateTimeUtil.parseISO8601toUTC("2009");
	// setAddDocField(doc, Constants.SEARCH_DATE,
	// dateTime.toString(), false);

	private void buildSearchIndex(String pid, String errorString,
			AddToSearchResponse response, String date, boolean isRestricted) {
		boolean hasMODS = false;
		boolean hasDC = false;
		boolean hasCONTENTS = false;
		boolean hasDATA = false;
		String dsName = "";
		String dsMimetype = "";

		logger.debug("In unrestrictedBuildSearchIndex");

		SolrInputDocument doc = new SolrInputDocument();
		DataResponse dc = null;
		SAXBuilder saxBuilder = new SAXBuilder();
		Document modsXmlDoc = null;
		Document dcXmlDoc = null;

		// get datastreams list to check for DC and MODS
		ListDatastreamsResponse listDatastreamsResponse = dataService
				.getDatastreams(pid, "test");

		for (DatastreamDef def : listDatastreamsResponse.getDatastreamDef()) {
			logger.debug("pid: " + pid + " datastream: " + def.getID()
					+ " label: " + def.getLabel());

			if (Constants.MD_DC.equals(def.getID()))
				hasDC = true;
			if (Constants.MD_DESCRIPTIVE.equals(def.getID()))
				hasMODS = true;
			if (Constants.MD_CONTENTS.equals(def.getID()))
				hasCONTENTS = true;
			if (def.getID().startsWith(Constants.SEARCH_DATA_)) {
				hasDATA = true;
				dsName = def.getID();
				dsMimetype = def.getMIMEType();
			}
		}

		logger.debug("pid: " + pid + " hasDC: " + hasDC + " hasMODS: "
				+ hasMODS + " hasCONTENTS: " + hasCONTENTS + " hasDATA: "
				+ hasDATA);

		// if this item has neither, log error and skip it
		if ((!hasDC) && (!hasMODS)) {
			logger
					.debug("Search indexing skipped as neither DC nor MODS present.  pid: "
							+ pid);

			return;
		}

		setAddDocField(doc, Constants.SEARCH_ID, pid, false);

		String orderNum = getOrder(pid);
		if (orderNum != null) {
			setAddDocField(doc, Constants.SEARCH_ORDER, orderNum, false);
		}

		setAddDocField(doc, Constants.SEARCH_DISPLAY_DATE, date, false);

		if (hasDATA) {
			setAddDocField(doc, Constants.SEARCH_DS_1, dsName, false);
			setAddDocField(doc, Constants.SEARCH_DS_1_MIMETYPE, dsMimetype,
					false);
		}

		// Load mods, set mods flag
		DataResponse mods = null;

		if (hasMODS) {
			try {
				mods = dataService.getData(pid, Constants.MD_DESCRIPTIVE);
			} catch (Exception e) {
				response.setResponse(errorString);
				logger.debug("No MODS for pid: " + pid);
			}
		}
		if ((mods != null) && (mods.getDissemination() != null)) {

			logger.debug("Processing MODS for pid: " + pid);

			try {
				modsXmlDoc = saxBuilder.build(new ByteArrayInputStream(mods
						.getDissemination().getStream()));
				if (modsXmlDoc == null) {
					hasMODS = false;
				}
			} catch (JDOMException e) {
				response.setResponse(errorString);
				logger.debug("Could not add: " + pid + " to search");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				response.setResponse(errorString);
				logger.debug("Could not add: " + pid + " to search");
				e.printStackTrace();
				return;
			}
		}

		if (hasDC) {
			dc = dataService.getData(pid, Constants.MD_DC);
			try {
				dcXmlDoc = saxBuilder.build(new ByteArrayInputStream(dc
						.getDissemination().getStream()));
				if (dcXmlDoc == null) {
					hasDC = false;
				}
			} catch (JDOMException e) {
				response.setResponse(errorString);
				logger.debug("Could not add: " + pid + " to search");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				response.setResponse(errorString);
				logger.debug("Could not add: " + pid + " to search");
				e.printStackTrace();
				return;
			}
		}

		try {
			if (hasDC) { // use DC to get creator
				Namespace namespace = Namespace.getNamespace("dc",
						"http://purl.org/dc/elements/1.1/");
				Namespace namespace2 = Namespace.getNamespace("oai_dc",
						"http://www.openarchives.org/OAI/2.0/oai_dc/");

				List<Element> list = xpathQuery(dcXmlDoc,
						"/oai_dc:dc/dc:creator", namespace, namespace2);
				setAddDocField(doc, Constants.SEARCH_CREATOR, list, -1, false);

				for (Element item : list) {
					logger.debug("DC creator: " + item.getTextTrim());
				}

			}

			if (hasMODS) {
				Namespace namespace = Namespace.getNamespace("mods",
						"http://www.loc.gov/mods/v3");

				List<Element> list = xpathQuery(modsXmlDoc,
						"/mods:mods/mods:abstract", namespace);
				setAddDocField(doc, Constants.SEARCH_DESCRIPTION, list, 0,
						false);

				list = xpathQuery(modsXmlDoc,
						"/mods:mods/mods:titleInfo/mods:title", namespace);
				setAddDocField(doc, Constants.SEARCH_TITLE, list, 0, false);

				if (!hasDC) {
					list = xpathQuery(modsXmlDoc,
							"/mods:mods/mods:name/mods:displayForm", namespace);
					setAddDocField(doc, Constants.SEARCH_CREATOR, list, 0,
							false);
				}

				list = xpathQuery(modsXmlDoc,
						"/mods:mods/mods:subject/mods:topic", namespace);
				setAddDocField(doc, Constants.SEARCH_SUBJECT, list, -1, false);

				if (!isRestricted) {
					list = xpathQuery(modsXmlDoc,
							"/mods:mods/mods:originInfo/mods:dateCreated",
							namespace);
					setAddDocField(doc, Constants.SEARCH_DATESTRING, list, 0,
							false);

					list = xpathQuery(modsXmlDoc,
							"/mods:mods/mods:language/mods:languageTerm",
							namespace);
					setAddDocField(doc, Constants.SEARCH_LANGUAGE, list, 0,
							false);
				}

				list = xpathQuery(modsXmlDoc, "/mods:mods/mods:genre",
						namespace);
				setAddDocField(doc, Constants.SEARCH_KEYWORD, list, 0, false);

				list = xpathQuery(modsXmlDoc,
						"/mods:mods/mods:physicalDescription/mods:extent",
						namespace);
				setAddDocField(doc, Constants.SEARCH_DS_1_SIZE, list, 0, false);

			} else if (hasDC) { // use DC
				Namespace namespace = Namespace.getNamespace("dc",
						"http://purl.org/dc/elements/1.1/");
				Namespace namespace2 = Namespace.getNamespace("oai_dc",
						"http://www.openarchives.org/OAI/2.0/oai_dc/");

				List<Element> list = xpathQuery(dcXmlDoc,
						"/oai_dc:dc/dc:description", namespace, namespace2);
				setAddDocField(doc, Constants.SEARCH_DESCRIPTION, list, 0,
						false);

				list = xpathQuery(dcXmlDoc, "/oai_dc:dc/dc:title", namespace,
						namespace2);
				setAddDocField(doc, Constants.SEARCH_TITLE, list, 0, false);

				// list = xpathQuery(xmlDoc, "/oai_dc:dc/dc:creator", namespace,
				// namespace2);
				// setAddDocField(doc, Constants.SEARCH_CREATOR, list, 0,
				// false);

				list = xpathQuery(dcXmlDoc, "/oai_dc:dc/dc:subject", namespace,
						namespace2);
				setAddDocField(doc, Constants.SEARCH_SUBJECT, list, -1, false);

				if (!isRestricted) {
					list = xpathQuery(dcXmlDoc, "/oai_dc:dc/dc:date",
							namespace, namespace2);
					setAddDocField(doc, Constants.SEARCH_DATESTRING, list, 0,
							false);

					list = xpathQuery(dcXmlDoc, "/oai_dc:dc/dc:language",
							namespace, namespace2);
					setAddDocField(doc, Constants.SEARCH_LANGUAGE, list, 0,
							false);
				}
			}
		} catch (JDOMException e) {
			response.setResponse(errorString);
			logger.debug("Could not add: " + pid + " to search");
			e.printStackTrace();
			return;
		}

		DataResponse contents = null;

		if (hasCONTENTS) {
			try {
				contents = dataService.getData(pid, Constants.MD_CONTENTS);
			} catch (Exception e) {
				response.setResponse(errorString);
				logger.debug("No CONTENTS for pid: " + pid);
			}
		}
		if ((contents != null) && (contents.getDissemination() != null)) {

			logger.debug("Processing CONTENTS for pid: " + pid);

			// try {
			// contentsXmlDoc = saxBuilder.build(new
			// ByteArrayInputStream(contents.getDissemination().getStream()));
			// } catch (JDOMException e) {
			// response.setResponse(errorString);
			// logger.debug("Could not add: " + pid + " to search");
			// e.printStackTrace();
			// return;
			// } catch (IOException e) {
			// response.setResponse(errorString);
			// logger.debug("Could not add: " + pid + " to search");
			// e.printStackTrace();
			// return;
			// }

			// get children
			if (hasCONTENTS) {
				String contentsStr = new String(contents.getDissemination()
						.getStream());
				List<String> childPids = new ArrayList<String>();

				try {
					Document d = new SAXBuilder()
							.build(new ByteArrayInputStream(contentsStr
									.getBytes()));

					Element parentDiv = d.getRootElement().getChild("div",
							JDOMNamespaceUtil.METS_NS);
					List<Element> childDivs = parentDiv.getChildren();
					ArrayList<PID> order = new ArrayList<PID>();

					for (Element child : childDivs) {
						childPids.add(child.getAttributeValue("ID"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				int i = 1;
				List tempChildren = new ArrayList(256);

				PID parent = new PID(pid);
				List<PathInfo> childPaths = tripleStoreQueryService
						.fetchChildPathInfo(parent);

				String space = " ";

				for (PathInfo childPath : childPaths) {
					NumberFormat formatter = new DecimalFormat("0000000");

					StringBuffer sb = new StringBuffer(128);
					sb.append(formatter.format(i++));
					sb.append(space);
					sb.append(childPath.getPid().getPid());
					sb.append(space);
					sb.append(StringEscapeUtils.escapeXml(childPath.getSlug()));
					sb.append(space);
					sb
							.append(StringEscapeUtils.escapeXml(childPath
									.getLabel()));

					tempChildren.add(sb.toString());
				}

				setAddDocField(doc, Constants.SEARCH_CHILD, tempChildren, false);
			}
		}

		PID Pid = new PID(pid);
		List<PathInfo> pathList = tripleStoreQueryService
				.lookupRepositoryPathInfo(Pid);

		/*
		 * Example PathInfo.getPath() output for
		 * /Collections/testaccesscontrol/2/3/4 0 / 1 /Collections 2
		 * /Collections/testaccesscontrol 3 /Collections/testaccesscontrol/2 4
		 * /Collections/testaccesscontrol/2/3 5
		 * /Collections/testaccesscontrol/2/3/4
		 * 
		 * int i = 0; for(PathInfo path : pathList) {
		 * logger.debug(i+" "+path.getPath()); i++; }
		 */

		PathInfo path = pathList.get(pathList.size() - 1);
		String stableUrl = path.getPath();

		setAddDocField(doc, Constants.SEARCH_URI, baseIrUrl + stableUrl, false);
		setAddDocField(doc, Constants.SEARCH_REPO_PATH, stableUrl + "/", false);

		setAddDocField(doc, Constants.SEARCH_PARENT, getParent(baseIrUrl
				+ stableUrl), false);
		setAddDocField(doc, Constants.SEARCH_PARENT_REPO_PATH,
				getParent(stableUrl) + "/", false);

		PathInfo collection;

		if (pathList.size() > 3) { // folder or item in a collection
			collection = pathList.get(2);

			setAddDocField(doc, Constants.SEARCH_COLLECTION, baseIrUrl
					+ collection.getPath(), false);

			setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, collection
					.getSlug(), false);

			setAddDocField(doc, Constants.SEARCH_SORT_ORDER,
					Constants.SEARCH_SORT_ORDER_OTHER_VALUE, false);
			setAddDocField(doc, Constants.SEARCH_IS_COLLECTION,
					Constants.SEARCH_IS_COLLECTION_FALSE, false);
		} else if (pathList.size() == 3) { // individual collection
			collection = pathList.get(1);

			setAddDocField(doc, Constants.SEARCH_COLLECTION, baseIrUrl
					+ collection.getPath(), false);

			setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, collection
					.getSlug(), false);

			setAddDocField(doc, Constants.SEARCH_SORT_ORDER,
					Constants.SEARCH_SORT_ORDER_COLLECTION_VALUE, false);
			setAddDocField(doc, Constants.SEARCH_IS_COLLECTION,
					Constants.SEARCH_IS_COLLECTION_TRUE, false);
		} else if (pathList.size() == 2) { // Collections
			collection = pathList.get(1);

			setAddDocField(doc, Constants.SEARCH_COLLECTION, baseIrUrl
					+ collection.getPath(), false);

			setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, collection
					.getSlug(), false);

			setAddDocField(doc, Constants.SEARCH_SORT_ORDER,
					Constants.SEARCH_SORT_ORDER_COLLECTION_VALUE, false);
			setAddDocField(doc, Constants.SEARCH_IS_COLLECTION,
					Constants.SEARCH_IS_COLLECTION_TRUE, false);
		}

		List<URI> contentModels = tripleStoreQueryService
				.lookupContentModels(Pid);

		setDisplayResourceType(doc, contentModels);

		for (URI contentModel : contentModels) {
			setAddDocField(doc, Constants.SEARCH_CONTENT_MODEL, contentModel
					.toString(), false);
		}

		if (!isRestricted) {
			// get the index text and add it to the text field
			addTextToAddDoc(doc, getIndexTextDatastream(pid));
		}

		try {
			server.add(doc);
			UpdateResponse updateResponse = server.commit();
			response.setResponse(updateResponse.toString());

			if (logger.isDebugEnabled())
				logger.debug(updateResponse.toString());

		} catch (SolrServerException e) {
			response.setResponse(errorString);
			e.printStackTrace();
		} catch (IOException e) {
			response.setResponse(errorString);
			e.printStackTrace();
		}
	}

	private void setDisplayResourceType(SolrInputDocument doc,
			List<URI> contentModels) {
		String display = "";

		for (URI contentModel : contentModels) {
			String temp = contentModel.toString();
			if (Constants.CONTENT_MODEL_SIMPLE.equals(temp)) {
				display = Constants.DISPLAY_FILE;
			} else if (Constants.CONTENT_MODEL_CONTAINER.equals(temp)) {
				display = Constants.DISPLAY_FOLDER;
			} else if (Constants.CONTENT_MODEL_DISK.equals(temp)) {
				display = Constants.DISPLAY_FOLDER;
				break;
			}

		}

		setAddDocField(doc, Constants.SEARCH_DISPLAY_RESOURCE_TYPE, display,
				false);
	}

	private String getDisplayDateString(Date date) {
		SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd");

		return displayFormat.format(date);
	}

	private String getOrder(String pid) {
		DataResponse contents = null;
		SAXBuilder saxBuilder = new SAXBuilder();
		Document xmlDoc = null;
		String parentPid;

		PID childPID = new PID(pid);
		PID parent = tripleStoreQueryService.fetchContainer(childPID);

		parentPid = parent.getPid();

		try {
			contents = dataService.getData(parentPid, Constants.MD_CONTENTS);
		} catch (Exception e) {
			logger.debug("No CONTENTS for parentPid: " + parentPid);
		}

		if ((contents != null) && (contents.getDissemination() != null)) {

			logger.debug("Processing CONTENTS for parentPid: " + parentPid);

			try {
				xmlDoc = saxBuilder.build(new ByteArrayInputStream(contents
						.getDissemination().getStream()));
			} catch (JDOMException e) {
				logger.debug("Could not get order of: " + pid + " for search");
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				logger.debug("Could not get order of: " + pid + " for search");
				e.printStackTrace();
				return null;
			}

			String contentsStr = new String(contents.getDissemination()
					.getStream());
			try {
				Document d = new SAXBuilder().build(new ByteArrayInputStream(
						contentsStr.getBytes()));

				Element parentDiv = d.getRootElement().getChild("div",
						JDOMNamespaceUtil.METS_NS);
				List<Element> childDivs = parentDiv.getChildren();
				ArrayList<PID> order = new ArrayList<PID>();

				for (Element child : childDivs) {
					if (pid.equals(child.getAttributeValue("ID"))) {
						return child.getAttributeValue("ORDER");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private String resourceType() {
		String results = "";

		return results;
	}

	private List<Element> xpathQuery(Document doc, String query,
			Namespace namespace, Namespace namespace2) throws JDOMException {
		XPath xpath = XPath.newInstance(query);
		xpath.addNamespace(namespace);
		xpath.addNamespace(namespace2);

		return xpath.selectNodes(doc);
	}

	private List<Element> xpathQuery(Document doc, String query,
			Namespace namespace) throws JDOMException {
		XPath xpath = XPath.newInstance(query);
		xpath.addNamespace(namespace);

		return xpath.selectNodes(doc);
	}

	// TODO: Remove this code when time available for cleanup
	public void addToSolr(Map map) {
		try {
			server = new CommonsHttpSolrServer(solrUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SolrInputDocument doc = new SolrInputDocument();
		setAddDocField(doc, Constants.SEARCH_CREATOR, getCleanerString(map
				.get(Constants.RI_CREATOR)), false);
		setAddDocField(doc, Constants.SEARCH_CONTRIBUTOR, getCleanerString(map
				.get(Constants.RI_CONTRIBUTOR)), false);
		setAddDocField(doc, Constants.SEARCH_LOCATION, getCleanerString(map
				.get(Constants.RI_COVERAGE)), false);
		setAddDocField(doc, Constants.SEARCH_TITLE, getTitle(map), false);
		setAddDocField(doc, Constants.SEARCH_DESCRIPTION, getCleanerString(map
				.get(Constants.RI_DESCRIPTION)), false);
		setAddDocField(doc, Constants.SEARCH_ID, map.get(Constants.PID), false);
		setAddDocField(doc, Constants.SEARCH_ISSUED,
				Constants.SEARCH_UNKNOWN_DATE, false);

		Object tempDate = map.get(Constants.RI_DATE);
		if (tempDate != null) {
			String temp = (String) tempDate;
			setAddDocField(doc, Constants.SEARCH_DATE, temp, false);
			setAddDocField(doc, Constants.SEARCH_DATESTRING, temp, false);
		} else {

			setAddDocField(doc, Constants.SEARCH_DATE,
					Constants.SEARCH_UNKNOWN_DATE, false);
		}

		setAddDocCollection(doc, map);

		setAddDocField(doc, Constants.SEARCH_PUBLISHER, map
				.get(Constants.RI_PUBLISHER), false);
		setAddDocField(doc, Constants.SEARCH_TYPE, map.get(Constants.RI_TYPE),
				false);

		String stableUrl = (String) map.get(Constants.SEARCH_URI);
		setAddDocField(doc, Constants.SEARCH_URI, stableUrl, false);
		setAddDocField(doc, Constants.SEARCH_PARENT, getParent(stableUrl),
				false);

		if (logger.isDebugEnabled())
			logger.debug("addToSolr thumbnail: "
					+ (String) map.get(Constants.RI_THUMBNAIL));

		doc.addField(Constants.SEARCH_THUMBNAIL,
				getDatastreamUrlFromDsPid((String) map
						.get(Constants.RI_THUMBNAIL)));

		setAddDocField(doc, Constants.SEARCH_CONTENT_MODEL, map
				.get(Constants.RI_CONTENT_MODEL), false);
		setAddDocField(doc, Constants.SEARCH_IS_CONSTITUENT_OF, map
				.get(Constants.RI_IS_CONSTITUENT_OF), true);
		setAddDocField(doc, Constants.SEARCH_LAST_CONSTITUENT, map
				.get(Constants.RI_LAST_CONSTITUENT), true);
		setAddDocField(doc, Constants.SEARCH_FIRST_CONSTITUENT, map
				.get(Constants.RI_FIRST_CONSTITUENT), true);
		setAddDocField(doc, Constants.SEARCH_NEXT_CONSTITUENT, map
				.get(Constants.RI_NEXT_CONSTITUENT), true);
		setAddDocField(doc, Constants.SEARCH_PREV_CONSTITUENT, map
				.get(Constants.RI_PREV_CONSTITUENT), true);

		// add pdf view to search index
		ImageListResponse imageListResponse = dataService
				.getImageDatastreamIds((String) map.get(Constants.PID));
		if (imageListResponse.getPdfs().size() > 0) {
			StringBuilder url = new StringBuilder(256);
			url.append(baseHostUrl).append(Constants.DATA_PREFIX).append(
					idService.getUrlFromPid((String) map.get(Constants.PID)))
					.append('?').append(Constants.DS_PREFIX).append(
							imageListResponse.getPdfs().get(0));
			doc.addField(Constants.SEARCH_PDF_URL, url.toString());
		}

		// process constituents
		Object first = map.get(Constants.RI_FIRST_CONSTITUENT);
		Object last = map.get(Constants.RI_LAST_CONSTITUENT);
		if ((first != null) && (last != null)) {
			List<String> temp = constituentService.getOrderedConstituentPids(
					(String) map.get(Constants.PID), (String) first,
					(String) last);

			// determine if we should add an image view link to the
			// search
			// results
			ImageListResponse forImageListResponse = dataService
					.getImageDatastreamIds(temp.get(0));

			if (forImageListResponse.getImages().size() > 0) {
				StringBuilder url = new StringBuilder(256);
				url.append(baseHostUrl).append(Constants.IMAGE_VIEW_PREFIX)
						.append(
								idService.getUrlFromPid((String) map
										.get(Constants.PID)));
				doc.addField(Constants.SEARCH_IMAGE_VIEW, url.toString());
			}

			setAddDocField(doc, Constants.SEARCH_HAS_CONSTITUENT, temp, true);
		}

		setAddDocField(doc, Constants.SEARCH_SUBJECT, getCleanerStringArray(map
				.get(Constants.RI_SUBJECT)), false);

		if (map.get(Constants.RI_ORDER) != null) {
			doc.addField(Constants.SEARCH_ORDER, map.get(Constants.RI_ORDER));
		} else {
			doc.addField(Constants.SEARCH_ORDER, -1L);
		}

		addDatastreamsToAddDoc(doc, map);

		addTextToAddDoc(doc, map.get(Constants.RI_TEXT));

		// get collection info to get collection title for new search
		// results
		// display

		// if this object is a member of a collection
		if (map.get(Constants.RI_COLLECTION) != null) {
			Map collection = gatherRelsExtInformationService
					.getAllFromPid((String) map.get(Constants.RI_COLLECTION));

			setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, collection
					.get(Constants.RI_TITLE), false);
		} else {
			// it is a collection
			String contentModel = (String) map.get(Constants.RI_HAS_MODEL);

			if ((contentModel != null)
					&& (contentModel.equals(Constants.RI_MODEL_COLLECTION))) {
				setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, map
						.get(Constants.RI_TITLE), false);
			} else {
				setAddDocField(doc, Constants.SEARCH_COLLECTION_TITLE, "",
						false);
			}
		}

		String type = (String) map.get(Constants.RI_TYPE);
		if ((type != null)
				&& ((type.equals(Constants.RESOURCE_TYPE_JOURNAL_ARTICLE))
						|| (type
								.equals(Constants.RESOURCE_TYPE_JOURNAL_CONTENTS))
						|| (type
								.equals(Constants.RESOURCE_TYPE_JOURNAL_FRONT_MATTER)) || (type
						.equals(Constants.RESOURCE_TYPE_JOURNAL_BACK_MATTER)))) {
			DataResponse dataResponse = dataService.getData((String) map
					.get(Constants.PID), "XML"
					+ (String) map.get(Constants.RI_ORDER));

			try {
				JAXBContext jContext = JAXBContext
						.newInstance("edu.unc.lib.dl.schema");

				Unmarshaller unmarshaller = jContext.createUnmarshaller();

				JournalArticleMetadata journalArticleMetadata = (JournalArticleMetadata) unmarshaller
						.unmarshal(new ByteArrayInputStream(dataResponse
								.getDissemination().getStream()));

				setAddDocField(doc, Constants.SEARCH_PAGES,
						journalArticleMetadata.getPages(), false);
				setAddDocField(doc, Constants.SEARCH_LANGUAGE,
						journalArticleMetadata.getLanguage(), false);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		try {
			server.add(doc);
			UpdateResponse response = server.commit();

			if (logger.isDebugEnabled())
				logger.debug(response.toString());

		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Object getTitle(Map map) {
		if (map.get(Constants.RI_TITLE) != null) {
			return map.get(Constants.RI_TITLE);
		}

		return map.get(Constants.RI_LABEL);
	}

	private void setAddDocCollection(SolrInputDocument doc, Map map) {
		String model = (String) map.get(Constants.RI_HAS_MODEL);

		if (Constants.RI_MODEL_COLLECTION.equals(model)) {
			setAddDocField(doc, Constants.SEARCH_COLLECTION, map
					.get(Constants.PID), true);
		} else {
			setAddDocField(doc, Constants.SEARCH_COLLECTION, map
					.get(Constants.RI_COLLECTION), true);
		}
	}

	private void addTextToAddDoc(SolrInputDocument doc, Object text) {
		DataResponse response = null;

		if (logger.isDebugEnabled())
			logger.debug("addTextToAddDoc enter");

		// if text != null
		if (text == null) {
			if (logger.isDebugEnabled())
				logger.debug("addTextToAddDoc text was null, exiting");
			return;
		}

		// get datastream
		String[] temp = ((String) text).split(Constants.FORWARD_SLASH);

		if (logger.isDebugEnabled()) {

			for (int i = 0; i < temp.length; i++) {
				logger.debug("addTextToAddDoc temp[" + i + "]:" + temp[i]);
			}
		}
		if (temp.length == 2) {
			response = dataService.getData(temp[0], temp[1]);
		} else if (temp.length == 3) {
			response = dataService.getData(temp[1], temp[2]);
		}

		if (response == null) {
			if (logger.isDebugEnabled())
				logger.debug("addTextToAddDoc DataResponse is null");
		} else {

			try {
				// convert ds to string, add to solr field
				String value = new String(response.getDissemination()
						.getStream());

				if (logger.isDebugEnabled())
					logger.debug("addTextToAddDoc value:" + value);

				if (value != null) {
					doc.addField(Constants.SEARCH_TEXT, value);
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					e.printStackTrace();
				}

			}
		}
	}

	private void addDatastreamsToAddDoc(SolrInputDocument doc, Map map) {
		List list = (List) map.get(Constants.RI_DATASTREAM);

		Object[] ds = list.toArray();
		List dsUrls = new ArrayList(ds.length);

		for (int i = 0; i < ds.length; i++) {
			dsUrls.add(getDatastreamUrlFromDsPid((String) ds[i]));
		}

		setAddDocField(doc, Constants.SEARCH_DATASTREAM, dsUrls, false);
	}

	private String getDatastreamUrlFromDsPid(String entry) {
		// test:84/TEXT ->
		// http://localhost/ir/data/gamelan/mss06-15?ds=TEXT

		if (entry == null) {
			return "";
		}

		StringBuffer url = new StringBuffer(96);

		String[] tokens = entry.split(Constants.FORWARD_SLASH);

		if (logger.isDebugEnabled())
			logger.debug("getDatastreamUrlFromDsPid entry: " + entry);

		for (int i = 0; i < tokens.length; i++) {
			if (logger.isDebugEnabled())
				logger.debug(tokens[i]);
		}

		url.append(baseHostUrl).append(Constants.DATA_PREFIX).append(
				idService.getUrlFromPid(tokens[0])).append('?').append(
				Constants.DS_PREFIX).append(tokens[1]);

		if (logger.isDebugEnabled())
			logger.debug("getDatastreamUrlFromDsPid url: " + url.toString());

		return url.toString();
	}

	private void setAddDocField(SolrInputDocument doc, String name,
			List<Element> list, int index, boolean url) {

		if (list.size() > 0) {
			if (index == 0) {
				Element element = list.get(0);

				if (logger.isDebugEnabled())
					logger.debug(element.getTextTrim());

				setAddDocField(doc, name, element.getTextTrim(), url);
			} else if (index == -1) {
				for (Element element : list) {
					if (logger.isDebugEnabled())
						logger.debug(element.getTextTrim());

					setAddDocField(doc, name, element.getTextTrim(), url);
				}
			}
		}

	}

	private void setAddDocField(SolrInputDocument doc, String name,
			Object value, boolean url) {
		if (value == null) {
			if (logger.isDebugEnabled())
				logger.debug(name + " was null");
			return;
		}
		if ((value instanceof List) && (((List) value).size() < 1)) {
			if (logger.isDebugEnabled())
				logger.debug(name + " list was empty");
			return;
		}

		if (value instanceof List) {
			List temp = (List) value;
			for (int i = 0; i < temp.size(); i++)
				if (logger.isDebugEnabled())
					logger.debug(name + " value[" + i + "]: " + temp.get(i));
		} else if (logger.isDebugEnabled())
			logger.debug(name + " value: " + value);

		if (url) {
			if (value instanceof List) {
				for (Object object : ((List) value)) {
					doc.addField(name, utilityMethods
							.getItemInfoUrlFromPid((String) object));
				}
			} else {
				doc.addField(name, utilityMethods
						.getItemInfoUrlFromPid((String) value));
			}
		} else {
			doc.addField(name, value);
		}
	}

	private List<String> getCleanerStringArray(Object object) {
		if ((object == null) || (!(object instanceof List))) {
			return null;
		}
		List objects = (List) object;

		List<String> result = new ArrayList<String>(objects.size());

		for (Object temp : objects) {
			result.add(getCleanerString(temp));
		}

		return result;
	}

	// remove quotation marks, etc. before placing into Solr add doc
	private String getCleanerString(Object object) {

		if ((object == null) || (!(object instanceof String))) {
			return null;
		}
		String value = (String) object;

		StringBuffer result = new StringBuffer(value.length());

		for (int i = 0; i < value.length(); i++) {
			char temp = value.charAt(i);
			if (temp != '\"') {
				result.append(temp);
			}
		}

		return result.toString();
	}

	private String getParent(String stableUrl) {

		if (logger.isDebugEnabled())
			logger.debug("getParent stableUrl: " + stableUrl);

		String prefix = Constants.IR_PREFIX.substring(0, Constants.IR_PREFIX
				.lastIndexOf('/'));

		if (logger.isDebugEnabled())
			logger.debug("getParent prefix: " + prefix);

		String temp = stableUrl.substring(0, stableUrl.lastIndexOf('/'));

		if (logger.isDebugEnabled())
			logger.debug("getParent temp: " + temp);

		if ((temp.endsWith(prefix)) || (temp.equals(""))) {
			return Constants.FORWARD_SLASH;
		} else {
			return temp;
		}
	}

	private String getIndexTextDatastream(String pid) {
		// get uri form of pid
		String temp = Constants.RI_PID_PREFIX + pid;

		StringBuffer q = new StringBuffer();
		q.append("select $var from <%1$s>")
				.append(" where <%2$s> <%3$s> $var;");
		String query = String.format(q.toString(), this
				.getTripleStoreQueryService().getResourceIndexModelUri(), temp,
				ContentModelHelper.CDRProperty.indexText);
		List<List<String>> resp = this.getTripleStoreQueryService()
				.queryResourceIndex(query);

		for (List<String> solution : resp) {

			logger.debug("getIndexTextDatastream for pid: " + pid + " "
					+ solution.get(0));

			return solution.get(0);
		}

		return null;
	}

	private boolean allowIndexing(String pid) {
		boolean result = false;

		// get uri form of pid
		String temp = Constants.RI_PID_PREFIX + pid;

		StringBuffer q = new StringBuffer();
		q.append("select $var from <%1$s>")
				.append(" where <%2$s> <%3$s> $var;");
		String query = String.format(q.toString(), this
				.getTripleStoreQueryService().getResourceIndexModelUri(), temp,
				ContentModelHelper.CDRProperty.allowIndexing);
		List<List<String>> resp = this.getTripleStoreQueryService()
				.queryResourceIndex(query);

		for (List<String> solution : resp) {
			String allowIndexing = solution.get(0);

			logger.debug("Search allowIndexing value: " + allowIndexing);

			if (allowIndexing != null) {
				if (Constants.RI_ALLOW_INDEXING.equals(allowIndexing)) {
					result = true;
				}
			}

		}

		return result;
	}

	public void setSolrUrl(String solrUrl) {
		this.solrUrl = solrUrl;
	}

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public void setUtilityMethods(UtilityMethods utilityMethods) {
		this.utilityMethods = utilityMethods;
	}

	public void setConstituentService(ConstituentService constituentService) {
		this.constituentService = constituentService;
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}

	public void setGatherRelsExtInformationService(
			GatherRelsExtInformationService gatherRelsExtInformationService) {
		this.gatherRelsExtInformationService = gatherRelsExtInformationService;
	}

	public String getBaseIrUrl() {
		return baseIrUrl;
	}

	public void setBaseIrUrl(String baseIrUrl) {
		this.baseIrUrl = baseIrUrl;
	}

	public String getCollectionUrl() {
		return collectionUrl;
	}

	public void setCollectionUrl(String collectionUrl) {
		this.collectionUrl = collectionUrl;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;

	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setBaseHostUrl(String baseHostUrl) {
		this.baseHostUrl = baseHostUrl;
	}
}
