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

import edu.unc.lib.boxc.model.api.objects.WorkObject;

import java.util.Map;

/**
 * @author sharonluong
 */
public class WorkFactory extends ContentObjectFactory{
    FileFactory fileFactory;

    public WorkObject createWork(Map<String, String> options) throws Exception {
        var work = repositoryObjectFactory.createWorkObject(null);
        prepareObject(work, options);

        return work;
    }

    public void createFileInWork(WorkObject work, Map<String, String> options) throws Exception {
        var file = fileFactory.createFile(options);
        work.addMember(file);
        if ("true".equals(options.get("isPrimaryObject"))) {
            work.setPrimaryObject(file.getPid());
            // need to reindex in triple store if adding primary object
            indexTripleStore(work);
        }
        // need to reindex in solr after adding file object
        indexSolr(work);
    }
}
