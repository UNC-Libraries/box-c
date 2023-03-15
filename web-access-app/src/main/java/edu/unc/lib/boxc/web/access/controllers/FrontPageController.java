package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller which populates the dynamic components making up the front page of
 * the public UI
 * @author bbpennel, krwong
 */
@Controller
@RequestMapping("/")
public class FrontPageController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(FrontPageController.class);

    @Autowired
    @Qualifier("solrAccessRestrictionUtil")
    private AccessRestrictionUtil restrictionUtil;
    @Autowired
    private SolrQueryLayerService queryLayer;

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest() {
        LOG.debug("In front page controller");
        return "frontPage";
    }

    @RequestMapping(path = "/collectionStats", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getCollectionStats() {
        var collectionStats = new HashMap<String, Object>();
        AccessGroupSet groups = GroupsThreadStore.getPrincipals();
        collectionStats.put("formatCounts", this.queryLayer.getFormatCounts(groups));

        return SerializationUtil.objectToJSON(collectionStats);
    }
}
