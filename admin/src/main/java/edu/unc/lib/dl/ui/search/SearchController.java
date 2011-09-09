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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import edu.unc.lib.dl.search.Search;
import edu.unc.lib.dl.search.SearchProcessor;

public class SearchController extends SimpleFormController {
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    private SearchProcessor searchProcessor;

    @Override
    public ModelAndView onSubmit(Object command) throws ServletException {

    	
	String query = ((Search) command).getQuery();
	if(logger.isDebugEnabled()) logger.debug("Query string is " + query);

	// run query, generate results
	searchProcessor.sendQuery(query);

	String time = (new java.util.Date()).toString();
	if(logger.isDebugEnabled()) logger.debug(time + ", going to " + getSuccessView());

	return new ModelAndView(new RedirectView(getSuccessView()));
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request)
	    throws ServletException {

	Search search = new Search();
	search.setQuery("");

	return search;

    }

    public SearchProcessor getSearchProcessor() {
	return searchProcessor;
    }

    public void setSearchProcessor(SearchProcessor searchProcessor) {
	this.searchProcessor = searchProcessor;
    }

}
