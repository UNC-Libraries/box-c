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

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.WorkObjectImpl;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * Sets content-related status tags
 *
 * @author harring
 *
 */
public class SetContentStatusFilter implements IndexDocumentFilter{
    private static final Logger log = LoggerFactory.getLogger(SetContentStatusFilter.class);
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        List<String> contentStatus = determineContentStatus(dip);
        dip.getDocument().setContentStatus(contentStatus);

        log.debug("Content status for {} set to {}", dip.getPid().toString(), contentStatus);
    }

    private List<String> determineContentStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        List<String> status = new ArrayList<>();
        ContentObject obj = dip.getContentObject();
        Resource resc = obj.getResource();

        if (resc.hasProperty(Cdr.hasMods)) {
            status.add(FacetConstants.CONTENT_DESCRIBED);
        } else {
            status.add(FacetConstants.CONTENT_NOT_DESCRIBED);
        }

        if (obj instanceof WorkObjectImpl) {
            if (resc.hasProperty(Cdr.primaryObject)) {
                status.add(FacetConstants.HAS_PRIMARY_OBJECT);
            } else {
                status.add(FacetConstants.NO_PRIMARY_OBJECT);
            }
        }

        if (obj instanceof FileObjectImpl) {
            Resource parentResc = obj.getParent().getResource();
            if (parentResc.hasProperty(Cdr.primaryObject, resc)) {
                status.add(FacetConstants.IS_PRIMARY_OBJECT);
            }
        }

        return status;
    }
}
