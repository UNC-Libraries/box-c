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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Jun 24, 2015
 */
public class DocumentIndexingPackageFactory {

    @Autowired
    private DocumentIndexingPackageDataLoader dataLoader;

    public DocumentIndexingPackage createDip(String pid) {
        return new DocumentIndexingPackage(PIDs.get(pid), null, dataLoader);
    }

    public DocumentIndexingPackage createDip(PID pid) {
        return new DocumentIndexingPackage(pid, null, dataLoader);
    }

    public DocumentIndexingPackage createDip(PID pid, DocumentIndexingPackage parentDip) {
        return new DocumentIndexingPackage(pid, parentDip, dataLoader);
    }

    public void setDataLoader(DocumentIndexingPackageDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

}
