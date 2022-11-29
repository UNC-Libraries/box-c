package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;

import java.util.Map;

/**
 * Factory for making AdminUnit objects
 * @author snluong
 */
public class AdminUnitFactory extends ContentObjectFactory {
    public AdminUnit createAdminUnit(Map<String, String> options) throws Exception {
        var accessModel = getAccessModel(options);
        var adminUnit = repositoryObjectFactory.createAdminUnit(accessModel);
        ContentRootObject contentRoot = repositoryObjectLoader.getContentRootObject(
                RepositoryPaths.getContentRootPid());
        contentRoot.addMember(adminUnit);
        // if options has "hasThumbnail = true" then add a thumbnail
        if (options.containsKey("addThumbnail") && "true".equals(options.get("addThumbnail"))) {
            addThumbnail(adminUnit);
        }
        prepareObject(adminUnit, options);

        return adminUnit;
    }
}
