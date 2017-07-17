/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
/**
 * Sets access-related status tags
 *
 * @author harring
 *
 */
public class SetAccessStatusFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetAccessStatusFilter.class);
    private InheritedAclFactory aclFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set access status filter on {}", dip.getPid());
        dip.getDocument().setStatus(determineAccessStatus(dip));
    }

    /**
     * Sets acl factory
     *
     * @param aclFactory an inherited acl factory
     */
    public void setAclFactory(InheritedAclFactory aclFactory) {
        this.aclFactory = aclFactory;
    }

    private List<String> determineAccessStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        ContentObject obj = dip.getContentObject();
        PID pid = obj.getPid();
        List<String> status = new ArrayList<>();

        if (aclFactory.isMarkedForDeletion(pid)) {
            status.add(FacetConstants.MARKED_FOR_DELETION);
        }

        if (aclFactory.getEmbargoUntil(pid) != null) {
            status.add(FacetConstants.EMBARGOED);
        }

        if (aclFactory.getPatronAccess(pid).equals(PatronAccess.everyone)) {
            status.add(FacetConstants.PUBLIC_ACCESS);
        } else if (aclFactory.getPatronAccess(pid).equals(PatronAccess.authenticated)) {
            status.add(FacetConstants.STAFF_ONLY_ACCESS);
        }

        if (aclFactory.getPatronAccess(obj.getParent().getPid()).equals(PatronAccess.authenticated)) {
            status.add(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS);
        }

        return status;
    }

}
