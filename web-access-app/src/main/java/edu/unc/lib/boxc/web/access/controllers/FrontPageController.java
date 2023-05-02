package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/collectionStats")
public class FrontPageController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(FrontPageController.class);

    @RequestMapping(produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getCollectionStats() {
        LOG.debug("Requesting front page collection stats");
        var collectionStats = new HashMap<String, Object>();
        AccessGroupSet groups = GroupsThreadStore.getPrincipals();
        collectionStats.put("formatCounts", this.queryLayer.getFormatCounts(groups));

        return SerializationUtil.objectToJSON(collectionStats);
    }
}
