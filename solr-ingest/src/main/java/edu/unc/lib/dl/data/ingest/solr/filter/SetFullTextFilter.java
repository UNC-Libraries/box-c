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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.FedoraException;

/**
 * Retrieves full text data for object being indexed and stores it to the indexing document
 * @author bbpennel
 * @author harring
 *
 */
public class SetFullTextFilter implements IndexDocumentFilter {

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {

        ContentObject contentObj = dip.getContentObject();
        // object being indexed must be a work or a file object
        if (!(contentObj instanceof WorkObject) && !(contentObj instanceof FileObject)) {
            return;
        }
        FileObject fileObj = getFileObject(dip);
        if (fileObj == null) {
            return;
        }
        BinaryObject binObj;
        binObj = fileObj.getOriginalFile();
        if (!binObj.getMimetype().contains("text")) {
            return;
        }

        try {
            String fullText = IOUtils.toString(binObj.getBinaryStream(), "UTF-8");
            dip.getDocument().setFullText(fullText);
        } catch (FedoraException | IOException e) {
            throw new IndexingException("Failed to retrieve full text datastream for {}" + dip.getPid(), e);
        }
    }

    private FileObject getFileObject(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject obj = dip.getContentObject();
        FileObject fileObj;
        if (obj instanceof WorkObject) {
            fileObj = ((WorkObject) obj).getPrimaryObject();
        } else {
            fileObj = (FileObject) obj;
        }
        return fileObj;
    }
}
