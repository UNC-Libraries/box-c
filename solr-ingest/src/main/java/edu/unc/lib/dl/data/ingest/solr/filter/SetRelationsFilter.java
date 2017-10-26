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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.rdf.Cdr;

/**
 * Populates the relations field with the primary object and invalid terms.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class SetRelationsFilter implements IndexDocumentFilter{
    private static final Logger log = LoggerFactory.getLogger(SetRelationsFilter.class);

    private FileObject primaryObj;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Applying setRelationsFilter");

        List<String> relations = new ArrayList<>();

        ContentObject contentObj = dip.getContentObject();

        // if the content obj is a work obj, set relation on its primary object
        if (contentObj instanceof WorkObject) {
            primaryObj = ((WorkObject) contentObj).getPrimaryObject();

            if (primaryObj != null) {
                // store primary-object relation
                relations.add(Cdr.primaryObject.toString() + "|" + primaryObj.getPid().getId());
            }
        }

        // retrieve and store invalid terms
        List<String> invalidTerms = new ArrayList<>();
        StmtIterator it = contentObj.getResource().listProperties(Cdr.invalidTerm);
        while (it.hasNext()) {
            invalidTerms.add(it.nextStatement().getLiteral().getString());
        }
        String invalidTermPred = Cdr.invalidTerm.toString();
        if (invalidTerms != null) {
            for (String invalidTermTriple : invalidTerms) {
                // store invalid-term relation
                relations.add(invalidTermPred + "|" + invalidTermTriple);
            }
        }

        dip.getDocument().setRelations(relations);
    }
}
