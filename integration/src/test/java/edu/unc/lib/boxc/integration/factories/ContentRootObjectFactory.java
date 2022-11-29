package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;

import java.util.Collections;

/**
 * @author bbpennel
 */
public class ContentRootObjectFactory extends ContentObjectFactory {
    protected RepositoryInitializer repositoryInitializer;

    /**
     * Initialize and prepare root of the repository
     * @throws Exception
     */
    public void initializeRepository() throws Exception {
        repositoryInitializer.initializeRepository();
        ContentRootObject contentRoot = repositoryObjectLoader.getContentRootObject(
                RepositoryPaths.getContentRootPid());
        prepareObject(contentRoot, Collections.emptyMap());
    }

    public void setRepositoryInitializer(RepositoryInitializer repoInitializer) {
        this.repositoryInitializer = repoInitializer;
    }
}
