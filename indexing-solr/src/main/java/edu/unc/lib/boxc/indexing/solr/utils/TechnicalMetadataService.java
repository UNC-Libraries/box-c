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
package edu.unc.lib.boxc.indexing.solr.utils;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bbpennel
 */
public class TechnicalMetadataService {
    private RepositoryObjectLoader repositoryObjectLoader;

    public Document retrieveDocument(PID filePid) {
        var techMdPid = DatastreamPids.getTechnicalMetadataPid(filePid);
        var techMdObj = repositoryObjectLoader.getBinaryObject(techMdPid);
        return retrieveDocument(techMdObj);
    }

    public Document retrieveDocument(BinaryObject techMdObj) {
        InputStream techMdData = techMdObj.getBinaryStream();
        String techMdId = techMdObj.getPid().getId();

        try {
            SAXBuilder builder = new SAXBuilder();
            return builder.build(techMdData);
        } catch (JDOMException | IOException e) {
            throw new RepositoryException("Unable to parse technical metadata for " + techMdId, e);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
