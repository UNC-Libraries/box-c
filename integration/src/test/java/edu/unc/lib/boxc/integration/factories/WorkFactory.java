package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

import java.util.Map;

/**
 * Factory for creating test WorkObjects and their files
 * @author snluong
 */
public class WorkFactory extends ContentObjectFactory {
    public static final String PRIMARY_OBJECT_KEY = "isPrimaryObject";
    FileFactory fileFactory;

    public WorkObject createWork(CollectionObject collection, Map<String, String> options) throws Exception {
        var accessModel = getAccessModel(options);
        var work = repositoryObjectFactory.createWorkObject(accessModel);
        collection.addMember(work);
        prepareObject(work, options);

        return work;
    }

    /**
     * Adds a specified file (options should have what type of file) to the work
     * optional boolean in options can make this file the primary object in the work
     */
    public FileObject createFileInWork(WorkObject work, Map<String, String> options) throws Exception {
        var file = fileFactory.createFile(options);
        work.addMember(file);
        prepareObject(file, options);

        if (options.containsKey(PRIMARY_OBJECT_KEY) && "true".equals(options.get(PRIMARY_OBJECT_KEY))) {
            work.setPrimaryObject(file.getPid());
            // need to reindex in triple store if adding primary object
            indexTripleStore(work);
        }
        // need to reindex in solr after adding file object
        indexSolr(work);
        return file;
    }

    public void setFileFactory(FileFactory fileFactory) {
        this.fileFactory = fileFactory;
    }
}
