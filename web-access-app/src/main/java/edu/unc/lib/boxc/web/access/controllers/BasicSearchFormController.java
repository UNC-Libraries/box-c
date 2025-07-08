package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * Handles search requests from basic search forms.  Can handle
 * a single search box with search type specified, as well as search within searches using
 * the previous search state stored in session, and any number of navigation actions.  Constructs
 * a new search state and sends it to the search controller.
 * @author bbpennel
 */
@Controller
@RequestMapping("/api/basicSearch")
public class BasicSearchFormController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(BasicSearchFormController.class);
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    protected SearchStateFactory searchStateFactory;

    @RequestMapping(method = RequestMethod.GET)
    public String searchForm(@RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queryType", required = false) String queryType,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "within", required = false) String searchWithin,
            @RequestParam(value = "searchType", required = false) String searchType, Model model,
            HttpServletRequest request) {
        // Query needs to be encoded before being added into the new url
        try {
            if (query != null) {
                query = URLEncoder.encode(query, "UTF-8");
            }
        } catch (UnsupportedEncodingException e1) {
        }

        StringBuilder destination = new StringBuilder();
        if (request.getParameter("queryPath") != null && request.getParameter("queryPath").equals("structure")) {
            destination.append("redirect:/structure");
        } else {
            destination.append("redirect:/search");
        }

        if (!"".equals(searchType) && container != null && container.length() > 0) {
            destination.append('/').append(container);
        }

        if ("within".equals(searchType) && searchWithin != null) {
            try {
                searchWithin = URLDecoder.decode(searchWithin, "UTF-8");
                HashMap<String, String[]> parameters = SearchStateUtil.getParametersAsHashMap(searchWithin);
                SearchState withinState = searchStateFactory.createSearchState(parameters);
                if (withinState.getSearchFields().size() > 0) {
                    String queryKey = searchSettings.searchFieldKey(queryType);
                    String typeValue = withinState.getSearchFields().get(queryKey);
                    if (queryKey != null) {
                        if (typeValue == null) {
                            withinState.getSearchFields().put(queryKey, query);
                        } else {
                            withinState.getSearchFields().put(queryKey, typeValue + " " + query);
                        }
                        String searchStateUrl = SearchStateUtil.generateStateParameterString(withinState);
                        destination.append('?').append(searchStateUrl);
                    }
                } else {
                    destination.append('?').append(queryType).append('=').append(query);
                    destination.append('&').append(searchWithin);
                }
            } catch (Exception e) {
                LOG.error("Failed to decode searchWithin " + searchWithin, e);
            }
        } else {
            destination.append('?').append(queryType).append('=').append(query);
        }
        return destination.toString();
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }
}
