package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.IdListRequest;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value = "/status/item" )
public class ItemInfoRestController {

    private static final Logger LOG = LoggerFactory.getLogger(ItemInfoRestController.class);

    @Autowired
    private SolrQueryLayerService solrSearchService;

    private static final List<String> VERSION_FIELDS = Collections.singletonList(SearchFieldKey.VERSION.name());

    @RequestMapping(value = "{id}/solrRecord/version", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> getVersion(@PathVariable("id") String id) {
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        SimpleIdRequest idRequest = new SimpleIdRequest(PIDs.get(id), VERSION_FIELDS, principals);
        ContentObjectRecord md = solrSearchService.getObjectById(idRequest);
        if (md == null || md.get_version_() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(md.get_version_(), HttpStatus.OK);
    }

    @RequestMapping(value = "solrRecord/version",
            method = RequestMethod.POST,
            produces = "application/json; charset=UTF-8")
    public @ResponseBody
    ResponseEntity<Object> getVersions(@RequestParam("ids") String idsString) {
        if (idsString == null || idsString.trim().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<String> ids = Arrays.asList(idsString.split("\n"));

        LOG.debug("Requesting version numbers for {}", ids);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        IdListRequest listRequest = new IdListRequest(ids, VERSION_FIELDS, principals);
        List<ContentObjectRecord> listResults = solrSearchService.getObjectsById(listRequest);
        Map<String, String> results = new HashMap<>(listResults.size());

        for (ContentObjectRecord result: listResults) {
            results.put(result.getId(), Long.toString(result.get_version_()));
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
