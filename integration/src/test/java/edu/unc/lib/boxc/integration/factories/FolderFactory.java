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

import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;

/**
 * @author sharonluong
 */
public class FolderFactory {
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectTreeIndexer repositoryObjectTreeIndexer;
    private RepositoryObjectSolrIndexer repositoryObjectSolrIndexer;

    public FolderObject createFolder() throws Exception {
        // create folder in Fedora
        var folder = repositoryObjectFactory.createFolderObject(null);
        // index folder in triple store
        repositoryObjectTreeIndexer.indexAll(folder.getUri().toString());
        // index into solr
        repositoryObjectSolrIndexer.index(folder.getPid());
        return folder;
    }
}
