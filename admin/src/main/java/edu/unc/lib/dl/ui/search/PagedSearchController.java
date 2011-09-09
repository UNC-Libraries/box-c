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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.search.SearchQuery;
import edu.unc.lib.dl.ui.ws.UiWebService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.DefaultSearchSettings;

@Controller
public class PagedSearchController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private DefaultSearchSettings settings;
	private UiWebService uiWebService;
	private int start;
	private int rows;

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	@RequestMapping("/pagedsearch/**")
	public ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException {
		
		if (logger.isDebugEnabled())
			logger.debug("In PagedSearchFormController.onSubmit");

		
		
		String query = (String) request.getParameter("query");
		String restriction = "";

		logger.debug("Query: "+query);

		if ((query != null) && ((query.trim()).length() > 0)) {
			query = query.trim();

			query = stripQuestionableQueries(query);
			
			query = query.replace(":", "\\:");
		}

		
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

			// search for the query term and in the current object and objects
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
			logger.debug(query);

		BasicQueryResponseList queryResponse = uiWebService.basicQuery(
				query, restriction, inside, "test", start, rows, allString);

		String now = (new java.util.Date()).toString();

		Map myModel = new HashMap();
		myModel.put("now", now);
		myModel.put("entries", queryResponse.getBasicQueryResponse());
		myModel.put("queryString", queryResponse.getQueryString());

		// TODO: get these locally instead of through queryResponse
		myModel.put("baseInstUrl", queryResponse.getBaseInstUrl());
		myModel.put("collectionUrl", queryResponse.getCollectionUrl());
		myModel.put("allUrl", queryResponse.getAllUrl());
		myModel.put("pagedUrl", queryResponse.getPagedUrl());
		myModel.put("nextUrl", queryResponse.getNextUrl());
		myModel.put("nextUrlCount", queryResponse.getNextUrlCount());
		myModel.put("previousUrl", queryResponse.getPreviousUrl());
		myModel.put("previousUrlCount", queryResponse.getPreviousUrlCount());
		myModel.put("searchInString", queryResponse.getSearchInString());
		myModel.put("results", new Long(queryResponse.getResults()));
 
		return new ModelAndView("searchresults", "searchResults", myModel);
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
		
		if(param != null) {
			try {
				result = Integer.parseInt(param);
			}
			catch(NumberFormatException e) {
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

	public DefaultSearchSettings getSettings() {
		return settings;
	}

	public void setSettings(DefaultSearchSettings settings) {
		this.settings = settings;
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
