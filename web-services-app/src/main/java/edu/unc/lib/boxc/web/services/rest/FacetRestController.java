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
package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.services.FacetValuesService;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for retrieving information from facets
 *
 * @author bbpennel
 */
@Controller
public class FacetRestController extends AbstractSolrSearchController {
    @Autowired
    private FacetValuesService facetValuesService;
    @Autowired
    private SearchSettings searchSettings;

    @RequestMapping(value = "/facet/{facetId}/listValues", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> listValues(@PathVariable("facetId") String facetId,
                                      @RequestParam("facetSort") String sort,
                                      @RequestParam("facetStart") Integer start,
                                      @RequestParam("facetRows") Integer rows,
                                      HttpServletRequest request) {
        return listValues(facetId, null, sort, start, rows, request);
    }

    /**
     * List values within a particular facet field in paginated form, scoped based on any included search state.
     * @param facetId
     * @param sort Sort order to use for the facet values, either by count or index (alphabetic)
     * @param start starting offset for the page of results
     * @param rows number of values to return in the page
     * @param request http request
     * @return json response containing the facet field and its value
     */
    @RequestMapping(value = "/facet/{facetId}/listValues/{rootId}", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> listValues(@PathVariable("facetId") String facetId,
                                      @PathVariable("rootId") String rootId,
                                      @RequestParam("facetSort") String sort,
                                      @RequestParam("facetStart") Integer start,
                                      @RequestParam("facetRows") Integer rows,
                                      HttpServletRequest request) {

        var facetKey = SearchFieldKey.getByUrlParam(facetId);
        if (facetKey == null) {
            throw new IllegalArgumentException("Unknown facet field specified");
        }
        if (!searchSettings.getFacetNames().contains(facetId)) {
            throw new IllegalArgumentException("Invalid facet field specified: " + facetId);
        }
        FacetValuesService.assertValidFacetSortValue(sort);

        SearchRequest searchRequest = generateSearchRequest(request);
        if (rootId != null) {
            searchRequest.setRootPid(PIDs.get(rootId));
        }

        var facetResp = facetValuesService.listValues(facetKey, sort, start, rows, searchRequest);

        return new ResponseEntity<>(facetResp, HttpStatus.OK);
    }
}
