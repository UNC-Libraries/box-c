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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.BasicQueryRequest;
import edu.unc.lib.dl.schema.BasicQueryResponse;
import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.GetChildrenResponse;
import edu.unc.lib.dl.schema.OverviewDataRequest;
import edu.unc.lib.dl.schema.OverviewDataResponse;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.search.SearchIndex;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.service.SearchService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DefaultSearchSettings;
import edu.unc.lib.dl.util.SortablePathInfoDao;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

public class SearchServiceImpl implements SearchService {
	protected final Log logger = LogFactory.getLog(getClass());
	private DefaultSearchSettings settings;
	private String baseInstUrl;
	private String collectionUrl;
	private String searchUrl;
	private TripleStoreQueryService tripleStoreQueryService;
	private SearchIndex searchIndex;
	
	public SearchIndex getSearchIndex() {
		return searchIndex;
	}

	public void setSearchIndex(SearchIndex searchIndex) {
		this.searchIndex = searchIndex;
	}

	public void reindexEverything() {
		ReindexEverythingThread thread = new ReindexEverythingThread();
		thread.setTripleStoreQueryService(tripleStoreQueryService);
		thread.start();
	}

	public void addToSearch(List<String> pids) {
		IngestIndexThread thread = new IngestIndexThread(pids);
		thread.start();
	}

	private String getTitleFromUri(String uri, Map<String, String> cache,
			SolrServer server, boolean escapeColon) {
		// short circuit and avoid solr query
		if (cache.containsKey(uri)) {
			return cache.get(uri);
		}

		String localUri = null;

		if (escapeColon) {
			localUri = uri.replace(":", "\\:");
		} else {
			localUri = uri;
		}

		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("uri:" + localUri);
		solrQuery.setFields(Constants.SEARCH_TITLE);
		solrQuery.setRows(1);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int loop = queryResponse.getResults().size();

		try {
			for (int i = 0; i < loop; i++) {
				String temp = (String) queryResponse.getResults().get(i)
						.getFieldValue(Constants.SEARCH_TITLE);

				if (temp != null) {
					cache.put(uri, temp);

					return temp;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public OverviewDataResponse getOverviewData(OverviewDataRequest request) {
		SolrServer server = null;
		QueryResponse queryResponse = null;
		OverviewDataResponse response = new OverviewDataResponse();
		String repoPath = null;
		String displayType = null;
		String mimetype = null;
		
		response.setId(request.getId());
		
		
		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		String temp = request.getId().replace(":", "\\:");
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:" + temp);
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_DISPLAY_DATE, Constants.SEARCH_REPO_PATH,
				Constants.SEARCH_DISPLAY_RESOURCE_TYPE, Constants.SEARCH_DS_1_MIMETYPE);
		solrQuery.setRows(1);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int loop = queryResponse.getResults().size();

		try {
			for (int i = 0; i < loop; i++) {

				response.setDate(getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DISPLAY_DATE)));
				repoPath = getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_REPO_PATH));
				displayType = getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DISPLAY_RESOURCE_TYPE));
				mimetype = getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DS_1_MIMETYPE));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long objects = getNumberOfObjects(repoPath, displayType, mimetype, server);
		
		response.setFiles(Long.toString(objects));
		
		return response;
	}

	
	public BasicQueryResponseList basicQuery(BasicQueryRequest basicQueryRequest) {
		boolean viewAll = false;
		SolrServer server = null;
		QueryResponse queryResponse = null;
		String queryString = basicQueryRequest.getQuery();

		if (Constants.SEARCH_RETURN_ALL_RESULTS.equals(basicQueryRequest
				.getAll())) {
			viewAll = true;
		}

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(queryString + " "
				+ basicQueryRequest.getRestriction());
		solrQuery.setHighlight(true);
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,
				Constants.SEARCH_TITLE, Constants.SEARCH_DISPLAY_DATE,
				Constants.SEARCH_DISPLAY_RESOURCE_TYPE,
				Constants.SEARCH_REPO_PATH, Constants.SEARCH_IS_COLLECTION,
				Constants.SEARCH_SORT_ORDER, Constants.SEARCH_COLLECTION,
				Constants.SEARCH_CREATOR);

		if (viewAll) {
			solrQuery.setRows(Integer.MAX_VALUE);
		} else {
			solrQuery.setRows(basicQueryRequest.getRows());
			solrQuery.setStart(basicQueryRequest.getStart());
		}

		solrQuery
				.addSortField(Constants.SEARCH_SORT_ORDER, SolrQuery.ORDER.asc);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BasicQueryResponseList returnList = new BasicQueryResponseList();
		returnList.setQueryString(queryString);
		returnList.setStart(basicQueryRequest.getStart());
		returnList.setRows(basicQueryRequest.getRows());
		returnList.setEnd(returnList.getStart()
				+ queryResponse.getResults().size());
		returnList.setResults(queryResponse.getResults().getNumFound());

		returnList.setInside(basicQueryRequest.getInside());
		returnList.setNextUrlCount("");
		returnList.setPreviousUrlCount("");
		returnList.setSearchInString("");
		returnList.setPagedUrl("");

		setNextPreviousLinks(returnList, viewAll);

		// logger.debug("queryString " + queryString);
		// logger.debug("numfound " + queryResponse.getResults().getNumFound());
		// logger.debug("size " + queryResponse.getResults().size());

		returnList.setBaseInstUrl(baseInstUrl);
		returnList.setCollectionUrl(collectionUrl);
		int loop = queryResponse.getResults().size();

		try {
			for (int i = 0; i < loop; i++) {
				BasicQueryResponse basicQueryResponse = new BasicQueryResponse();

				String id = (String) queryResponse.getResults().get(i)
						.getFieldValue(Constants.SEARCH_ID);
				basicQueryResponse.setId(id);
				basicQueryResponse.setTitle(getTitle(queryResponse, i));

				basicQueryResponse.setCreator(getString(queryResponse
						.getResults().get(i).getFieldValue(
								Constants.SEARCH_CREATOR)));

				basicQueryResponse
						.setThumbnail(getChildIcon((String) queryResponse
								.getResults().get(i).getFieldValue(
										Constants.SEARCH_DISPLAY_RESOURCE_TYPE)));
				basicQueryResponse.setUri(getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_URI)));

				basicQueryResponse.setDate(getString(queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DISPLAY_DATE)));

				basicQueryResponse.setType(getString(queryResponse.getResults()
						.get(i).getFieldValue(
								Constants.SEARCH_DISPLAY_RESOURCE_TYPE)));

				basicQueryResponse.setRepoPath(getString(queryResponse
						.getResults().get(i).getFieldValue(
								Constants.SEARCH_REPO_PATH)));

				if ((queryResponse.getHighlighting() != null)
						&& (queryResponse.getHighlighting().get(id) != null)
						&& (queryResponse.getHighlighting().get(id).get(
								Constants.SEARCH_TEXT) != null)) {
					basicQueryResponse.setExcerpt(queryResponse
							.getHighlighting().get(id).get(
									Constants.SEARCH_TEXT).get(0));
				}

				basicQueryResponse.setCollectionTitle(getString(queryResponse
						.getResults().get(i).getFieldValue(
								Constants.SEARCH_COLLECTION_TITLE)));

				basicQueryResponse.setCollection(getString(queryResponse
						.getResults().get(i).getFieldValue(
								Constants.SEARCH_COLLECTION)));

				Integer temp = (Integer) queryResponse.getResults().get(i)
						.getFieldValue(Constants.SEARCH_IS_COLLECTION);
				basicQueryResponse.setIsCollection(temp.toString());

				returnList.getBasicQueryResponse().add(basicQueryResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// go through return list and set Collection display name
		Map<String, String> cache = new HashMap<String, String>(returnList
				.getBasicQueryResponse().size());
		for (BasicQueryResponse basicQueryResponse : returnList
				.getBasicQueryResponse()) {
			basicQueryResponse.setCollectionTitle(getTitleFromUri(
					basicQueryResponse.getCollection(), cache, server, true));
		}

		// get search inside string
		if ((basicQueryRequest.getInside() != null)
				&& ((!basicQueryRequest.getInside().equals("")))) {
			returnList.setSearchInString(getTitleFromUri(basicQueryRequest
					.getInside(), cache, server, false));
		}

		return returnList;
	}

	private void setNextPreviousLinks(BasicQueryResponseList queryResponse,
			boolean viewAll) {
		int start = queryResponse.getStart();
		int rows = queryResponse.getRows();
		long results = queryResponse.getResults();
		StringBuffer next = new StringBuffer(128);
		StringBuffer previous = new StringBuffer(128);
		StringBuffer all = new StringBuffer(128);
		StringBuffer paged = new StringBuffer(128);

		logger.debug("rows: " + rows);
		logger.debug("results: " + results);

		// All results url
		if (rows < results) {
			if (viewAll) {
				queryResponse.setAllUrl("");
				paged.append(searchUrl).append("?query=").append(
						queryResponse.getQueryString()).append("&all=false");
				paged.append("&start=").append(start);
				paged.append("&rows=").append(rows);

				queryResponse.setPagedUrl(paged.toString());
			} else {
				all.append(searchUrl).append("?query=").append(
						queryResponse.getQueryString()).append("&all=true");
				queryResponse.setAllUrl(all.toString());

				queryResponse.setPagedUrl("");
			}
		}

		// Previous url conditions
		if (start == 0) {
			queryResponse.setPreviousUrl("");

			queryResponse.setPreviousUrlCount("");
		} else {
			previous.append(searchUrl).append("?query=").append(
					queryResponse.getQueryString());
			int temp = 0;

			if ((start - rows) > 0) {
				temp = start - rows;
			}
			previous.append("&start=").append(temp);
			previous.append("&rows=").append(rows);

			queryResponse.setPreviousUrl(previous.toString());

			if (start >= rows) {
				queryResponse.setPreviousUrlCount(Integer.toString(rows));
			} else {
				queryResponse.setPreviousUrlCount(Integer.toString(start));
			}
		}

		// Next Conditions
		// # of results less than number of rows (i.e. 5 results and rows == 10
		// don't show next links
		if (results <= rows) {
			queryResponse.setNextUrl("");

			queryResponse.setNextUrlCount("");
		}
		// # of results greater than number of start but less than start+rows
		// no next link
		else if ((results > start) && (results <= start + rows)) {
			queryResponse.setNextUrl("");
			queryResponse.setNextUrlCount("");
		}

		// # of results greater than start+rows
		// next link is to start+rows
		else {
			// something like ...search?start=x&rows=y
			next.append(searchUrl).append("?query=").append(
					queryResponse.getQueryString());
			next.append("&start=").append(start + rows);
			next.append("&rows=").append(rows);

			queryResponse.setNextUrl(next.toString());

			if ((start + rows + rows) > results) {
				queryResponse.setNextUrlCount(Long.toString(results
						- (start + rows)));
			} else {
				queryResponse.setNextUrlCount(Integer.toString(rows));
			}
		}

		if ((start < 0) || (rows <= 0) || viewAll || (rows >= results)) {
			queryResponse.setPreviousUrl("");
			queryResponse.setPreviousUrlCount("");

			queryResponse.setNextUrl("");
			queryResponse.setNextUrlCount("");
		}
	}

	public BasicQueryResponseList getImageViewUrl(String url) {
		SolrServer server = null;
		QueryResponse queryResponse = null;

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("uri:\"" + url + "\"");
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_IMAGE_VIEW,
				Constants.SEARCH_URI);
		solrQuery.setRows(Integer.MAX_VALUE);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BasicQueryResponseList returnList = new BasicQueryResponseList();
		int loop = queryResponse.getResults().size();
		List tempList = new ArrayList();

		for (int i = 0; i < loop; i++) {
			BasicQueryResponse basicQueryResponse = new BasicQueryResponse();

			String id = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_ID);
			basicQueryResponse.setId(id);
			basicQueryResponse.setUri(getString(queryResponse.getResults().get(
					i).getFieldValue(Constants.SEARCH_URI)));

			basicQueryResponse.setType("");
			basicQueryResponse.setDate("");
			basicQueryResponse.setCollection("");
			basicQueryResponse.setCollectionTitle("");

			tempList.add(basicQueryResponse);
		}

		returnList.getBasicQueryResponse().addAll(tempList);

		return returnList;

	}

	public String getTitleByUri(String url) {
		String response = null;
		SolrServer server = null;
		QueryResponse queryResponse = null;

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("uri:\"" + url + "\"");
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_TITLE,
				Constants.SEARCH_URI);
		solrQuery.setRows(Integer.MAX_VALUE);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int loop = queryResponse.getResults().size();

		for (int i = 0; i < loop; i++) {
			response = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_TITLE);
		}

		return response;
	}

	public BasicQueryResponseList getChildren(String parentUrl) {
		SolrServer server = null;
		QueryResponse queryResponse = null;

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("parent:\"" + parentUrl + "\"");
		solrQuery.setHighlight(true);
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,

		Constants.SEARCH_TITLE, Constants.SEARCH_THUMBNAIL,
				Constants.SEARCH_DESCRIPTION, Constants.SEARCH_CONTENT_MODEL,
				Constants.SEARCH_ORDER, Constants.SEARCH_CREATOR,
				Constants.SEARCH_PDF_URL, Constants.SEARCH_PAGES,
				Constants.SEARCH_LANGUAGE);
		solrQuery.setRows(Integer.MAX_VALUE);
		solrQuery.addSortField(Constants.SEARCH_ORDER, SolrQuery.ORDER.asc);
		solrQuery.addSortField(Constants.SEARCH_TITLE, SolrQuery.ORDER.asc);

		if (logger.isDebugEnabled())
			logger
					.debug("getChildren query: " + "parent:\"" + parentUrl
							+ "\"");

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BasicQueryResponseList returnList = new BasicQueryResponseList();
		int loop = queryResponse.getResults().size();
		List tempList = new ArrayList();

		for (int i = 0; i < loop; i++) {
			BasicQueryResponse basicQueryResponse = new BasicQueryResponse();

			String id = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_ID);
			basicQueryResponse.setId(id);
			basicQueryResponse.setTitle(getTitle(queryResponse, i));

			basicQueryResponse.setThumbnail(getString(queryResponse
					.getResults().get(i).getFieldValue(
							Constants.SEARCH_THUMBNAIL)));
			basicQueryResponse.setUri(getString(queryResponse.getResults().get(
					i).getFieldValue(Constants.SEARCH_URI)));

			List<String> tempCM = (List<String>) queryResponse.getResults()
					.get(i).getFieldValue(Constants.SEARCH_CONTENT_MODEL);

			basicQueryResponse.setExcerpt(getString(queryResponse.getResults()
					.get(i).getFieldValue(Constants.SEARCH_DESCRIPTION)));

			basicQueryResponse.setType("");
			basicQueryResponse.setDate("");
			basicQueryResponse.setCollection("");
			basicQueryResponse.setCollectionTitle("");

			tempList.add(basicQueryResponse);
			// returnList.getBasicQueryResponse().add(basicQueryResponse);
		}

		// Collections.sort(tempList, new BasicQueryResponseComparator());

		returnList.getBasicQueryResponse().addAll(tempList);

		return returnList;
	}

	public GetChildrenResponse getChildren(GetChildrenRequest request) {
		if (request.getBaseUrl() != null) {
			GetChildrenResponse response = new GetChildrenResponse();
			List<String> tempList = null;
			SolrServer server = null;

			response.setPid(request.getPid());
			
			try {
				server = new CommonsHttpSolrServer(settings.getUrl());
			} catch (MalformedURLException e) {
				// TODO: handle this more gracefully
				e.printStackTrace();
			}

			// return getProcessedChildren(request);
			if (request.getType().equals(Constants.SEARCH_TYPE_COLLECTION)) {
				tempList = getCollectionsChildren(request, server);
				response.getChild().clear();
				response.getChild().addAll(tempList);

				return response;
			} else if (request.getType().equals(Constants.SEARCH_TYPE_FOLDER)) {
				tempList = getFolderChildren(request, server);
				response.getChild().clear();
				response.getChild().addAll(tempList);

				return response;
			} else {
				return getChildrenBySearch(request);
			}
		} else {
			return getRawChildren(request);
		}
	}

	private long getNumberOfObjects(String repoPath, String displayType,
			String mimetype, SolrServer server) {
		QueryResponse queryResponse = new QueryResponse();
		String temp = null;

		// optimize for case where object is an item and has a file
		if (displayType != null && mimetype != null) {
			if (Constants.DISPLAY_FILE.equals(displayType)) {
				return 1;
			}
		}

		logger.debug("repoPath: " + repoPath);

		if (repoPath.endsWith("/")) {
			temp = repoPath;
		} else {
			temp = repoPath + "/";
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("(parentRepoPath:" + temp + "* OR repoPath:" + temp
				+ ") AND ds1:DATA_*");
		solrQuery.setRows(0);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.debug("Number found: "
				+ queryResponse.getResults().getNumFound());

		return queryResponse.getResults().getNumFound();
	}

	private List<String> getCollectionsChildren(GetChildrenRequest request,
			SolrServer server) {
		List<String> children = new ArrayList<String>();
		QueryResponse queryResponse = new QueryResponse();

		String parentRepoPath = request.getBaseUrl();

		parentRepoPath = parentRepoPath.substring(parentRepoPath
				.indexOf("/Collections"));

		logger.debug("parentRepoPath: " + parentRepoPath);

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("parentRepoPath:" + parentRepoPath + "/");
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,
				Constants.SEARCH_DESCRIPTION, Constants.SEARCH_TITLE,
				Constants.SEARCH_CREATOR, Constants.SEARCH_REPO_PATH,
				Constants.SEARCH_ORDER, Constants.SEARCH_DISPLAY_DATE);
		solrQuery.setSortField(Constants.SEARCH_ORDER, ORDER.asc);
		solrQuery.setRows(Integer.MAX_VALUE);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.debug("Number found: " + queryResponse.getResults().size());

		int loop = queryResponse.getResults().size();

		try {
			for (int i = 0; i < loop; i++) {

				StringBuffer temp = new StringBuffer(256);

				if (i % 2 == 0) {
					temp.append(Constants.UI_TABLE_EVEN_TR_BEGIN);
				} else {
					temp.append(Constants.UI_TABLE_ODD_TR_BEGIN);
				}
				temp.append(Constants.UI_TABLE_TR_END);

				temp.append(Constants.UI_TABLE_TITLE_BEGIN);
				temp.append("<a href=\"");
				temp.append(getString((String) queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_URI)));
				temp.append("\">");
				temp.append(getTitle(queryResponse, i));
				temp.append("</a>");

				temp
						.append(Constants.UI_TABLE_TITLE_ABSTRACT_BEGIN);
				temp.append(getString((String) queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DESCRIPTION)));

				temp.append(Constants.UI_TABLE_CREATOR_BEGIN);
				temp.append(getString(queryResponse, Constants.SEARCH_CREATOR, i));

				logger.debug("search_creator: "+getString(queryResponse, Constants.SEARCH_CREATOR, i));
				
				temp.append(Constants.UI_TABLE_FILES_BEGIN);
				temp
						.append(getNumberOfObjects((String) queryResponse
								.getResults().get(i).getFieldValue(
										Constants.SEARCH_REPO_PATH), null,
								null, server));

				temp.append(Constants.UI_TABLE_LAST_UPDATED_BEGIN);
				temp.append(getString((String) queryResponse.getResults()
						.get(i).getFieldValue(Constants.SEARCH_DISPLAY_DATE)));
				temp.append(Constants.UI_TABLE_ROW_END);

				children.add(temp.toString());

				logger.debug(temp.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return children;
	}

	private List<String> getFolderChildren(GetChildrenRequest request,
			SolrServer server) {
		List<String> children = new ArrayList<String>();
		QueryResponse queryResponse = new QueryResponse();

		String parentRepoPath = request.getBaseUrl();

		parentRepoPath = parentRepoPath.substring(parentRepoPath
				.indexOf("/Collections"));

		logger.debug("parentRepoPath: " + parentRepoPath);

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("parentRepoPath:" + parentRepoPath + "/");
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,
				Constants.SEARCH_DS_1_MIMETYPE,
				Constants.SEARCH_DISPLAY_RESOURCE_TYPE, Constants.SEARCH_TITLE,
				Constants.SEARCH_REPO_PATH, Constants.SEARCH_ORDER,
				Constants.SEARCH_DISPLAY_DATE);
		solrQuery.setSortField(Constants.SEARCH_ORDER, ORDER.asc);
		solrQuery.setRows(Integer.MAX_VALUE);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.debug("Number found: " + queryResponse.getResults().size());

		int loop = queryResponse.getResults().size();

		try {
				for (int i = 0; i < loop; i++) {
					String displayType = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_DISPLAY_RESOURCE_TYPE);

        			String mimetype = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_DS_1_MIMETYPE);

					StringBuffer temp = new StringBuffer(256);

					if (i % 2 == 0) {
						temp.append(Constants.UI_TABLE_EVEN_TR_BEGIN);
					} else {
						temp.append(Constants.UI_TABLE_ODD_TR_BEGIN);
					}
//					temp.append(i);
					temp.append(Constants.UI_TABLE_TR_END);

					temp.append(Constants.UI_TABLE_TITLE_BEGIN);
					temp.append("<a href=\"");
					temp.append(getString((String) queryResponse.getResults()
							.get(i).getFieldValue(Constants.SEARCH_URI)));
					temp.append("\">");
					temp.append(getChildIcon(displayType));
					temp.append(getTitle(queryResponse, i));
					temp.append("</a>");

					temp.append(Constants.UI_TABLE_FILES_BEGIN);
					temp
							.append(getNumberOfObjects((String) queryResponse
									.getResults().get(i).getFieldValue(
											Constants.SEARCH_REPO_PATH), displayType,
											mimetype, server));

					temp.append(Constants.UI_TABLE_LAST_UPDATED_BEGIN);
					temp.append(getString((String) queryResponse.getResults()
							.get(i).getFieldValue(Constants.SEARCH_DISPLAY_DATE)));
					temp.append(Constants.UI_TABLE_ROW_END);

					children.add(temp.toString());

					logger.debug(temp.toString());
				}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return children;
	}

	private GetChildrenResponse getChildrenBySearch(GetChildrenRequest request) {
		GetChildrenResponse response = new GetChildrenResponse();
		QueryResponse queryResponse = new QueryResponse();
		SolrServer server = null;

		response.setPid(request.getPid());

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		String parentRepoPath = request.getBaseUrl();

		parentRepoPath = parentRepoPath.substring(parentRepoPath
				.indexOf("/Collections"));

		logger.debug("parentRepoPath: " + parentRepoPath);

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("parentRepoPath:" + parentRepoPath + "/");
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,
				Constants.SEARCH_TITLE, Constants.SEARCH_CREATOR,
				Constants.SEARCH_ORDER, Constants.SEARCH_CONTENT_MODEL,
				Constants.SEARCH_TIMESTAMP);
		solrQuery.setSortField(Constants.SEARCH_ORDER, ORDER.asc);
		solrQuery.setRows(Integer.MAX_VALUE);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.debug("Number found: " + queryResponse.getResults().size());

		int loop = queryResponse.getResults().size();
		List tempList = new ArrayList(response.getChild().size());

		try {
			for (int i = 0; i < loop; i++) {

				StringBuffer temp = new StringBuffer(128);
				temp.append("<a href=\"");
				temp.append((String) queryResponse.getResults().get(i)
						.getFieldValue(Constants.SEARCH_URI));
				temp.append("\">");
				temp.append(getTitle(queryResponse, i));
				temp.append("</a>");

				tempList.add(temp.toString());

				logger.debug(temp.toString());
			}

			response.getChild().clear();
			response.getChild().addAll(tempList);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}

	protected AccessGroupSet getUserAccessGroups(String accessGroups){
		if (accessGroups == null){
			//If we couldn't get the user, set it to public
			//return new AccessGroupSet(AccessGroupConstants.PUBLIC_GROUP);
			return new AccessGroupSet(AccessGroupConstants.ADMIN_GROUP);
		}
		String[] groups = accessGroups.split(" ");
		
		AccessGroupSet set = new AccessGroupSet();
		for(String group : groups){
			logger.debug("Access Group: "+ group);
			set.add(group);
		}
		
		return set;
	}
	
	public List<PathInfoDao> getChildrenFromSolr(String pid, String accessGroupsString, String baseUrl) {
		List<SortablePathInfoDao> children = new ArrayList<SortablePathInfoDao>();
		
		logger.debug("pid: "+pid);
		
		List<PathInfo> relsExtChildren = tripleStoreQueryService.fetchChildPathInfo(new PID(pid));
		
		for(PathInfo aChild : relsExtChildren) {
			PathInfoDao child = new PathInfoDao();
			
			logger.debug("child title: "+ aChild.getLabel());
			logger.debug("child pid: "+ aChild.getPid().getPid());
			
			child.setLabel(aChild.getLabel());
			child.setPid(aChild.getPid().getPid());
			child.setPath(baseUrl+"?id="+child.getPid());
			
			SortablePathInfoDao sortable = new SortablePathInfoDao();
			
			sortable.setPathInfoDao(child);
			
			children.add(sortable);
		}
		
		Collections.sort(children);
		
		List<PathInfoDao> sortedChildren = new ArrayList<PathInfoDao>(children.size());
		
		for(SortablePathInfoDao child : children) {
			sortedChildren.add(child.getPathInfoDao());
		}
		
		
/*		AccessGroupSet accessGroups = getUserAccessGroups(accessGroupsString);	

		SearchState searchState = SearchStateFactory.createTitleListSearchState();
		
		HierarchicalFacet rootAncestorPath = solrSearchService.getAncestorPath(pid, accessGroups);
		
		if(rootAncestorPath == null) {
		    rootAncestorPath = new HierarchicalFacet(SearchFieldKeys.ANCESTOR_PATH, "1,*");
		}
		
		// Set the tier cuttoff to be two tiers after the root so that no children past the immediate will be retrieved
		rootAncestorPath.setCutoffTier(rootAncestorPath.getHighestTier() + 2);
		HashMap<String, Object> facets = new HashMap<String, Object>(1);
		facets.put(SearchFieldKeys.ANCESTOR_PATH, rootAncestorPath);
		searchState.setFacets(facets);
		
		SearchRequest searchRequest = new SearchRequest();
		
		searchRequest.setAccessGroups(accessGroups);		
		
		searchState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
		searchState.setRowsPerPage(searchSettings.defaultListResultsPerPage);
		searchState.setSortType("dateAdded");
		searchRequest.setSearchState(searchState);
		SearchResultResponse resultResponse = solrSearchService.getSearchResults(searchRequest);

		List<BriefObjectMetadataBean> resultList = resultResponse.getResultList();
		
		for(BriefObjectMetadataBean bean : resultList) {
			PathInfoDao child = new PathInfoDao();
			
			logger.debug("child title: "+ bean.getTitle());
			logger.debug("child pid: "+ bean.getId());
			
			child.setLabel(bean.getTitle());
			child.setPid(bean.getId());
			child.setPath(baseUrl+"?id="+bean.getId());
			children.add(child);
		}
*/
		
		
		return sortedChildren;
	}
	
	public List<PathInfoDao> GetPathInfoDaoChildren(GetChildrenRequest request) {
		List<PathInfoDao> children = new ArrayList<PathInfoDao>();

		GetChildrenResponse response = getRawChildren(request);

		// Determine if we need a trailing slash before the slug
		String slash;
		String baseUrl = request.getBaseUrl();

		logger.debug("baseUrl: " + baseUrl);

		if (baseUrl.endsWith("/"))
			slash = "";
		else
			slash = "/";

		for (String child : response.getChild()) {
			PathInfoDao temp = new PathInfoDao();

			// 0000001 uuid slug label
			int firstSpace = child.indexOf(' ');
			int secondSpace = child.indexOf(' ', firstSpace + 1);
			int thirdSpace = child.indexOf(' ', secondSpace + 1);
			// String order = child.substring(0, firstSpace);
			temp.setPid(child.substring(firstSpace + 1, secondSpace));
			temp.setPath(baseUrl + slash
					+ (child.substring(secondSpace + 1, thirdSpace)).trim());
			temp.setLabel(child.substring(thirdSpace, child.length()));

			children.add(temp);
		}

		return children;
	}

	private GetChildrenResponse getProcessedChildren(GetChildrenRequest request) {
		GetChildrenResponse response = getRawChildren(request);

		// cut children

		// Determine if we need a trailing slash before the slug
		String slash;

		if (request.getBaseUrl().endsWith("/"))
			slash = "";
		else
			slash = "/";

		// <a href="baseUrl+[/]+slug">label</a>

		List tempList = new ArrayList(response.getChild().size());

		for (String child : response.getChild()) {

			// 0000001 uuid slug label
			int firstSpace = child.indexOf(' ');
			int secondSpace = child.indexOf(' ', firstSpace + 1);
			int thirdSpace = child.indexOf(' ', secondSpace + 1);
			String order = child.substring(0, firstSpace);
			String slug = child.substring(secondSpace + 1, thirdSpace);
			String label = child.substring(thirdSpace, child.length());

			StringBuffer temp = new StringBuffer(128);
			temp.append("<a href=\"");
			temp.append(request.getBaseUrl());
			temp.append(slash);
			temp.append(slug);
			temp.append("\">");
			temp.append(label);
			temp.append("</a>");

			tempList.add(temp.toString());
		}

		response.getChild().clear();
		response.getChild().addAll(tempList);

		return response;
	}

	private GetChildrenResponse getRawChildren(GetChildrenRequest request) {
		SolrServer server = null;
		QueryResponse queryResponse = null;

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:\"" + request.getPid() + "\"");
		solrQuery.setHighlight(true);
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_CHILD);
		solrQuery.setRows(Integer.MAX_VALUE);
		// solrQuery.addSortField(Constants.SEARCH_CHILD, SolrQuery.ORDER.asc);

		// if (logger.isDebugEnabled())
		// logger.debug("getRawChildren query: " + "parent:"
		// + request.getPid());

		GetChildrenResponse response = new GetChildrenResponse();
		response.setPid(request.getPid());

		try {
			queryResponse = server.query(solrQuery);

			int loop = queryResponse.getResults().size();

			response.setPid(request.getPid());

			for (int i = 0; i < loop; i++) {
				Object result = queryResponse.getResults().get(i)
						.getFieldValue(Constants.SEARCH_CHILD);

				if (result instanceof ArrayList) {
					for (Object child : (ArrayList) result) {
						response.getChild().add(getString(child));
					}
				} else if (result instanceof String) {
					response.getChild().add(getString(result));
				}
			}

			if (response.getChild() != null) {
				Collections.sort(response.getChild());
			}

		} catch (SolrServerException e) {
			// if (logger.isDebugEnabled())
			// logger.debug("getRawChildren query: " + "parent:"
			// + request.getPid() + "returned no children");

			e.printStackTrace();
		}

		return response;
	}

	public BasicQueryResponseList getCollections() {
		SolrServer server = null;
		QueryResponse queryResponse = null;

		try {
			server = new CommonsHttpSolrServer(settings.getUrl());
		} catch (MalformedURLException e) {
			// TODO: handle this more gracefully
			e.printStackTrace();
		}

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("contentModel:Collection");
		solrQuery.setHighlight(true);
		solrQuery.setFields(Constants.SEARCH_ID, Constants.SEARCH_URI,
				Constants.SEARCH_TITLE, Constants.SEARCH_THUMBNAIL,
				Constants.SEARCH_DESCRIPTION, Constants.SEARCH_TYPE);
		solrQuery.setRows(Integer.MAX_VALUE);
		solrQuery.addSortField(Constants.SEARCH_TITLE, SolrQuery.ORDER.asc);

		try {
			queryResponse = server.query(solrQuery);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BasicQueryResponseList returnList = new BasicQueryResponseList();
		int loop = queryResponse.getResults().size();
		for (int i = 0; i < loop; i++) {
			BasicQueryResponse basicQueryResponse = new BasicQueryResponse();

			String id = (String) queryResponse.getResults().get(i)
					.getFieldValue(Constants.SEARCH_ID);
			basicQueryResponse.setId(id);
			basicQueryResponse.setTitle(getTitle(queryResponse, i));
			basicQueryResponse.setCollectionTitle(getTitle(queryResponse, i));
			basicQueryResponse.setThumbnail(getString(queryResponse
					.getResults().get(i).getFieldValue(
							Constants.SEARCH_THUMBNAIL)));
			basicQueryResponse.setUri(getString(queryResponse.getResults().get(
					i).getFieldValue(Constants.SEARCH_URI)));
			basicQueryResponse.setType(getString(queryResponse.getResults()
					.get(i).getFieldValue(Constants.SEARCH_TYPE)));
			basicQueryResponse.setExcerpt(getString(queryResponse.getResults()
					.get(i).getFieldValue(Constants.SEARCH_DESCRIPTION)));

			basicQueryResponse.setCollection(id);
			basicQueryResponse.setDate("");

			returnList.getBasicQueryResponse().add(basicQueryResponse);
		}
		return returnList;
	}

	public void setDefaultSearchSettings(DefaultSearchSettings settings) {
		this.settings = settings;
	}

	private String getDisplayType(Object type, Object mimetype) {
		String typeStr = null;

		if (type != null) {
			typeStr = (String) type;

			if (Constants.DISPLAY_FOLDER.equals(typeStr)) { // don't show type
				// of folder's
				// datastream, just
				// "Folder"
				return Constants.DISPLAY_FOLDER;
			}
		}

		if (mimetype != null) {
			return (String) mimetype;
		} else {
			return Constants.DISPLAY_FILE;
		}
	}

	private String getTitle(QueryResponse response, int i) {
		String result = null;

		Object temp = response.getResults().get(i).getFieldValue(
				Constants.SEARCH_TITLE);

		if ((temp == null) || (temp.equals(Constants.EMPTY_STRING))) {
			String uri = (String) response.getResults().get(i).getFieldValue(
					Constants.SEARCH_URI);

			result = uri
					.substring(uri.lastIndexOf(Constants.FORWARD_SLASH) + 1);
		} else {
			result = (String) temp;

			result = StringEscapeUtils.escapeXml(result);
		}

		return result;
	}

	private String getString(QueryResponse response, String field, int i) {
		String result = null;

		Object temp = response.getResults().get(i).getFieldValue(field);

		if(temp == null) {
			logger.debug("temp is NULL 1");
			temp = response.getResults().get(i).getFieldValues(field);
		}
		
		if(temp == null) {
			logger.debug("temp is NULL 2");
			temp = response.getResults().get(i).getFirstValue(field);			
		}
		
		if ((temp == null) || (temp.equals(Constants.EMPTY_STRING))) {

			logger.debug("fieldValue is NULL");
			
			result = "";
		} else {
			logger.debug("class of fieldValue: "+temp.getClass().toString());

			
			StringBuffer sb = new StringBuffer(128);
			
			if(temp instanceof Collection) {
				Collection collection = (Collection) temp;
				for(Object object : collection) {
					sb.append(StringEscapeUtils.escapeXml((String)object)).append("<br/>");
				}
				int breakIndex = sb.lastIndexOf("<br/>");
				sb.delete(breakIndex, sb.length());				
				
				result = sb.toString().trim();
				
			} else {
				result = StringEscapeUtils.escapeXml((String) temp);				
			}
		}

		return result;
	}

	
	private String getDate(String dateString) {
		if (dateString == null) {

			// if (logger.isDebugEnabled())
			// logger.debug("getString value was null");

			return "";
			// return Constants.SEARCH_UNKNOWN_DATE;
		}

		StringBuilder sb = new StringBuilder(16);

		// assuming date is of the following format: yyyy-mm-ddTHH:mm:ssZ
		// and for now we only care about date, not time

		// Get date
		int tindex = dateString.indexOf('T');
		if (tindex < 8) {
			return "";
		}

		String temp = dateString.substring(0, tindex);

		// Should be 0 - year, 1 - month, 2 - day in month
		String[] dateArray = temp.split("-");

		int year = Integer.parseInt(dateArray[0]);
		int month = Integer.parseInt(dateArray[1]);
		int day = Integer.parseInt(dateArray[2]);

		if (year > 0) {
			sb.append(year);
			if (month > 0) {
				sb.append("-").append(month);
				if (day > 0) {
					sb.append("-").append(day);
				}
			}

			return sb.toString();
		}

		return "";
	}

	private String getChildIcon(String resourceType) {
		if (Constants.DISPLAY_FOLDER.equals(resourceType)) {
			return "<img src=\"/images/tiny_folder.png\"/>";
		} else {
			return "<img src=\"/images/tiny_doc.png\"/>";
		}
	}

	private String getString(Object object) {
		if (object == null) {

			// if (logger.isDebugEnabled())
			// logger.debug("getString value was null");

			return "";
		}

		if(object instanceof List){
			List list = (List) object;
			StringBuffer temp = new StringBuffer(256);
			
			for(int i = 0; i < list.size(); i++){
	
				temp.append((String)list.get(i));
				
				if(i < (list.size() - 1)) {
					temp.append(", ");
				}
			}
			
			return StringEscapeUtils.escapeXml(temp.toString());
		}
		
		// if (logger.isDebugEnabled())
		// logger.debug("getString value was " + object);
		else {
			return StringEscapeUtils.escapeXml((String) object);
		}
	}

	public String getCollectionUrl() {
		return collectionUrl;
	}

	public void setCollectionUrl(String collectionUrl) {
		this.collectionUrl = collectionUrl;
	}

	public String getSearchUrl() {
		return searchUrl;
	}

	public void setSearchUrl(String searchUrl) {
		this.searchUrl = searchUrl;
	}

	public void setBaseInstUrl(String baseInstUrl) {
		this.baseInstUrl = baseInstUrl;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	class ReindexEverythingThread extends Thread {
		protected final Log logger = LogFactory.getLog(getClass());
		private TripleStoreQueryService tripleStoreQueryService;

		public void run() {
			logger.info("Reindexing started");

			searchIndex.clearSearchIndex();
			searchIndex.reindexRepository();

			logger.info("Reindexing complete");
		}

		public void setTripleStoreQueryService(
				TripleStoreQueryService tripleStoreQueryService) {
			this.tripleStoreQueryService = tripleStoreQueryService;
		}
	}
	
	class IngestIndexThread extends Thread {
		protected final Log logger = LogFactory.getLog(getClass());
		private List<String> pids;

		public IngestIndexThread(List<String> pids) {
			this.pids = pids;
		}

		public void run() {
			logger.info("Ingest search indexing started");

			searchIndex.addToSolr(pids);

			logger.info("Ingest search indexing complete");
		}
	}
}
