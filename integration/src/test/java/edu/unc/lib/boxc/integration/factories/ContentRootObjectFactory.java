/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
