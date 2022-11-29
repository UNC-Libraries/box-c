package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;

import java.util.Map;

/**
 * @author snluong
 */
public class FolderFactory extends ContentObjectFactory {
    public FolderObject createFolder(CollectionObject collection, Map<String, String> options) throws Exception {
        var accessModel = getAccessModel(options);
        var folder = repositoryObjectFactory.createFolderObject(accessModel);
        collection.addMember(folder);
        prepareObject(folder, options);

        return folder;
    }
}
