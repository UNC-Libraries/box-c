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
package edu.unc.lib.boxc.indexing.solr.filter;

import static edu.unc.lib.boxc.model.api.DatastreamType.FULLTEXT_EXTRACTION;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.commons.io.FileUtils;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.Derivative;

/**
 * Retrieves full text data for object being indexed and stores it to the indexing document
 * @author bbpennel
 * @author harring
 *
 */
public class SetFullTextFilter implements IndexDocumentFilter {

    private DerivativeService derivativeService;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        FileObject fileObj = getFileObject(dip);
        if (fileObj == null) {
            return;
        }

        Derivative textDeriv = derivativeService.getDerivative(fileObj.getPid(), FULLTEXT_EXTRACTION);
        if (textDeriv == null) {
            return;
        }
        try {
            String fullText = FileUtils.readFileToString(textDeriv.getFile(), UTF_8);
            dip.getDocument().setFullText(fullText);
        } catch (IOException e) {
            throw new IndexingException("Failed to retrieve full text datastream for {}" + dip.getPid(), e);
        }
    }

    private FileObject getFileObject(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject contentObj = dip.getContentObject();
        // object being indexed must be a file object
        if (!(contentObj instanceof FileObject)) {
            return null;
        }

        return (FileObject) contentObj;
    }

    /**
     * @param derivativeService the derivativeService to set
     */
    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
