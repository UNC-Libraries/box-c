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
import edu.unc.lib.dl.util.DefaultSearchSettings;
import edu.unc.lib.dl.util.Constants;

public class AdvancedSearchFormController extends SimpleFormController {
	/** Logger for this class and subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SearchQuery searchQuery;
	private DefaultSearchSettings settings;
	private UiWebService uiWebService;

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	@Override
	public ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors)
			throws ServletException {

		if (logger.isDebugEnabled())
			logger.debug("In SearchFormController.onSubmit");

		
		SearchQuery searchQuery = (SearchQuery) command;

		String query = searchQuery.getQuery();

		String creator = searchQuery.getCreator();
		String title = searchQuery.getTitle();
		String collection = searchQuery.getCollection();
		String type = searchQuery.getType();
		String notContains = searchQuery.getNotContains();

		StringBuffer sb = new StringBuffer(1024);

		boolean somethingIsNotNull = false;

		String collectionSearch = null;
		String titleSearch = null;
		String typeSearch = null;
		String notContainsSearch = null;
		String creatorSearch = null;

		// Search within a particular collection
		if (notNull(collection)) {
			collection = collection.replace(":", "\\:");

			StringBuffer collectionSb = new StringBuffer(256);

			// search for the query term and in the current object and
			// objects
			// that have the current object as their parent and (finally)
			// objects which are a descendant of the current object
			collectionSb.append("(uri:").append(collection).append(
					" OR parent:").append(collection).append(" OR parent:")
					.append(collection).append("/*)");

			collectionSearch = collectionSb.toString();
		}

		// Search in a certain type (Journal Article, etc.)
		if (notNull(title)) {
			titleSearch = "titleText:\"" + title + "\"";
		}

		// Search in a certain type (Journal Article, etc.)
		if (notNull(type)) {
			typeSearch = "type:\"" + type + "\"";
		}

		// Search in creator
		if (notNull(creator)) {
			creatorSearch = "creatorText:\"" + creator + "\"";
		}

		//  
		if (notNull(notContains)) {
			notContainsSearch = " NOT \"" + notContains + "\"";
		}

		// build query
		if (notNull(query)) {
			somethingIsNotNull = true;
			sb.append(query);
		}

		if (notNull(collection)) {
			if (somethingIsNotNull) {
				sb.append(" AND ");
			}
			somethingIsNotNull = true;
			sb.append(collectionSearch);
		}
		if (notNull(title)) {
			if (somethingIsNotNull) {
				sb.append(" AND ");
			}
			somethingIsNotNull = true;
			sb.append(titleSearch);
		}
		if (notNull(type)) {
			if (somethingIsNotNull) {
				sb.append(" AND ");
			}
			somethingIsNotNull = true;
			sb.append(typeSearch);
		}
		if (notNull(creator)) {
			if (somethingIsNotNull) {
				sb.append(" AND ");
			}
			somethingIsNotNull = true;
			sb.append(creatorSearch);
		}

		// can't run a query with just a not clause
		if ((notNull(notContains)) && (somethingIsNotNull)) {
			sb.append(" AND ");
			sb.append(notContainsSearch);
		}

		query = sb.toString();

		if (logger.isDebugEnabled())
			logger.debug(searchQuery.getQuery());

		BasicQueryResponseList queryResponse = uiWebService.basicQuery(
				query, "", "", "test", 0, Integer.MAX_VALUE, Constants.SEARCH_RETURN_ALL_RESULTS);

		String now = (new java.util.Date()).toString();
		if (logger.isDebugEnabled())
			logger.debug("returning from SearchFormController view to "
					+ getSuccessView());

		// List<BasicQueryResponse> results = response.getBasicQueryResponse();

		// SearchResult searchResult = new SearchResult();
		// searchResult.setExerpt(response.getExcerpt());
		// searchResult.setTitle(response.getTitle());
		// searchResult.setUri(response.getUri());
		// results.add(searchResult);

		Map myModel = new HashMap();
		myModel.put("now", now);
		myModel.put("entries", queryResponse.getBasicQueryResponse());

		return new ModelAndView("searchresults", "searchResults", myModel);
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
}
