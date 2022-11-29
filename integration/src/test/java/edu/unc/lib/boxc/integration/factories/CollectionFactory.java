package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;

import java.util.Map;

/**
 * Factory for creating collection objects
 * @author snluong
 */
public class CollectionFactory extends ContentObjectFactory {
    public CollectionObject createCollection(AdminUnit adminUnit, Map<String, String> options) throws Exception {
        var accessModel = getAccessModel(options);
        var collection = repositoryObjectFactory.createCollectionObject(accessModel);
        // if options has "hasThumbnail = true" then add a thumbnail
        if ("true".equals(options.get("addThumbnail"))) {
            addThumbnail(collection);
        }

        adminUnit.addMember(collection);
        prepareObject(collection, options);

        return collection;
    }
}
