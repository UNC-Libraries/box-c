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
package edu.unc.lib.dl.ui.search;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.search.SearchQuery;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.DefaultSearchSettings;

public class SearchFormController extends SimpleFormController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SearchQuery searchQuery;
	private DefaultSearchSettings settings;
	private UiWebService uiWebService;
	private int start;
	private int rows;
	private String collectionUrl;


	@Override
	public ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException {

		if (logger.isDebugEnabled())
			logger.debug("In SearchFormController.onSubmit");


		String query = null;

		if (command != null)
			query = ((SearchQuery) command).getQuery();
		if (query == null)
			query = (String) request.getParameter("query");
		String restriction = "";

		logger.debug("Query: '" + query + "'");

		if ((query != null) && ((query.trim()).length() > 0)) {
			query = query.trim();

			query = stripQuestionableQueries(query);
			
			query = query.replace(":", "\\:");
			
			String inside = (String) request.getParameter("inside");

			// get start and rows

			String startString = (String) request.getParameter("start");
			String rowsString = (String) request.getParameter("rows");
			String allString = (String) request.getParameter("all");

			start = getIntegerParameter(startString, start);
			rows = getIntegerParameter(rowsString, rows);

			logger.debug("start: " + start);
			logger.debug("rows: " + rows);
			logger.debug("all: " + allString);

			// Search from a certain point in the object hierarchy down
			if (notNull(inside)) {

				inside = inside.replace(":", "\\:");

				StringBuffer sb = new StringBuffer(768);

				// search for the query term and in the current object and
				// objects
				// that have the current object as their parent and (finally)
				// objects which are a descendant of the current object
				sb.append(" AND (uri:").append(inside).append(" OR parent:")
						.append(inside).append(" OR parent:").append(inside)
						.append("/*)");

				restriction = sb.toString();

				if (logger.isDebugEnabled())
					logger.debug("Search restriction: " + restriction);
			}

			if (logger.isDebugEnabled())
				logger.debug(searchQuery.getQuery());

			BasicQueryResponseList queryResponse = uiWebService.basicQuery(
					query, restriction, inside, "test", start, rows, allString);

			String now = (new java.util.Date()).toString();
			if (logger.isDebugEnabled())
				logger.debug("returning from SearchFormController view to "
						+ getSuccessView());

			Map myModel = new HashMap();
			myModel.put("now", now);
			myModel.put("entries", queryResponse.getBasicQueryResponse());
			myModel.put("queryString", queryResponse.getQueryString());

			// TODO: get these locally instead of through queryResponse
			myModel.put("baseInstUrl", queryResponse.getBaseInstUrl());
			myModel.put("collectionUrl", queryResponse.getCollectionUrl());
			myModel.put("allUrl", queryResponse.getAllUrl());
			myModel.put("nextUrl", queryResponse.getNextUrl());
			myModel.put("nextUrlCount", queryResponse.getNextUrlCount());
			myModel.put("previousUrl", queryResponse.getPreviousUrl());
			myModel.put("previousUrlCount", queryResponse.getPreviousUrlCount());
			myModel.put("searchInString", queryResponse.getSearchInString());
			myModel.put("results", new Long(queryResponse.getResults()));
			
			return new ModelAndView("searchresults", "searchResults", myModel);
		}
		else {
			Map myModel = new HashMap();

			myModel.put("collectionUrl", collectionUrl);

			return new ModelAndView("searchresults", "searchResults", myModel);
		}
	}

	// Throw away queries which seem to be trying HTML/CSS injection
	private String stripQuestionableQueries(String query) {
		String test = query.toUpperCase();
		 
		if(test.contains("<") ||
           test.contains(">") ||
		   test.contains("%3C") ||
		   test.contains("%3E")) {

			return "";
		}
		
		return query;
	}
	
	private int getIntegerParameter(String param, int defaultValue) {
		int result = defaultValue;

		if (param != null) {
			try {
				result = Integer.parseInt(param);
			} catch (NumberFormatException e) {
				logger.debug(e);
			}
		}

		return result;
	}

	private boolean notNull(String value) {
		if ((value == null) || (value.equals(""))) {
			return false;
		}

		return true;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request)
			throws ServletException {

		if (searchQuery == null) {
			searchQuery = new SearchQuery();
			searchQuery.setQuery("");
		}

		return searchQuery;

	}

	public SearchQuery getSearchQuery() {
		return searchQuery;
	}

	public void setSearchQuery(SearchQuery searchQuery) {
		this.searchQuery = searchQuery;
	}

	public DefaultSearchSettings getSettings() {
		return settings;
	}

	public void setSettings(DefaultSearchSettings settings) {
		this.settings = settings;
	}

	public String getCollectionUrl() {
		return collectionUrl;
	}

	public void setCollectionUrl(String collectionUrl) {
		this.collectionUrl = collectionUrl;
	}

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	
	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}
}
