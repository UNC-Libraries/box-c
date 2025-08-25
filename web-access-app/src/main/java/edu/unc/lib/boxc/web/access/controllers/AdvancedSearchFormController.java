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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
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

    @RequestMapping(path = "/api/advancedSearch/collectionsJson", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getCollections() {
        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        SearchResultResponse collectionResultResponse = queryLayer.getCollectionList(accessGroups);
        return SerializationUtil.resultsToJSON(collectionResultResponse, accessGroups);
    }

    @RequestMapping(path = "/api/advancedSearch/formats", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getFormats() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(FORMAT_VALUES);
    }

    @RequestMapping(path = "/api/advancedSearch", method = RequestMethod.GET)
    public ModelAndView handleRequest(ModelMap model, HttpServletRequest request) {
        // If the user has submitted the search form, then generate a search state
        // and forward them to the search servlet.
        SearchState searchState = searchStateFactory.createSearchStateAdvancedSearch(request.getParameterMap());
        model.addAllAttributes(SearchStateUtil.generateStateParameters(searchState));

        return new ModelAndView("redirect:/search", model);
    }
}
