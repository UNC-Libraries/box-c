package edu.unc.lib.boxc.web.access.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Handles requests to the advanced search page, sending users to the form if there are no
 * query string parameters set, or constructing a search state and sending the user to
 * get results if they have populated the form.
 * @author bbpennel
 */
@Controller
public class AdvancedSearchFormController extends AbstractErrorHandlingSearchController {
    private final List<String> FORMAT_VALUES = Arrays.stream(ContentCategory.values())
            .map(ContentCategory::getDisplayName)
            .sorted()
            .collect(Collectors.toList());

    @RequestMapping(path = "/collections", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getCollections() {
        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        SearchResultResponse collectionResultResponse = queryLayer.getCollectionList(accessGroups);
        return SerializationUtil.resultsToJSON(collectionResultResponse, accessGroups);
    }

    @RequestMapping(path = "/formats", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getFormats() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(FORMAT_VALUES);
    }

    @RequestMapping(path = "/advancedSearch", method = RequestMethod.GET)
    public String handleRequest(Model model, HttpServletRequest request) {
        //If the user is coming to this servlet without any parameters set then send them to form.
        if (request.getQueryString() == null || request.getQueryString().length() == 0) {
            return "advancedSearch";
        }

        // If the user has submitted the search form, then generate a search state
        // and forward them to the search servlet.
        SearchState searchState = searchStateFactory.createSearchStateAdvancedSearch(request.getParameterMap());
        model.addAllAttributes(SearchStateUtil.generateStateParameters(searchState));

        return "redirect:/search";
    }
}
