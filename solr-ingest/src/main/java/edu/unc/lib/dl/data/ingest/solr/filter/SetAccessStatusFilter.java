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
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
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
    private InheritedAclFactory inheritedAclFactory;
    private ObjectAclFactory objAclFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set access status filter on {}", dip.getPid());
        dip.getDocument().setStatus(determineAccessStatus(dip));
    }

    /**
     * Sets inherited acl factory
     *
     * @param aclFactory an inherited acl factory
     */
    public void setInheritedAclFactory(InheritedAclFactory iaf) {
        this.inheritedAclFactory = iaf;
    }

    /**
     * Sets non-inherited acl factory
     *
     * @param aclFactory an inherited acl factory
     */
    public void setObjectAclFactory(ObjectAclFactory oaf) {
        this.objAclFactory = oaf;
    }

    private List<String> determineAccessStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        PID pid = dip.getPid();
        List<String> status = new ArrayList<>();

        PatronAccess inheritedAccess = inheritedAclFactory.getPatronAccess(pid);
        PatronAccess objAccess = objAclFactory.getPatronAccess(pid);

        if (inheritedAclFactory.isMarkedForDeletion(pid)) {
            status.add(FacetConstants.MARKED_FOR_DELETION);
        }

        Date objEmbargo = objAclFactory.getEmbargoUntil(pid);
        Date parentEmbargo = inheritedAclFactory.getEmbargoUntil(pid);
        if (objEmbargo != null) {
            status.add(FacetConstants.EMBARGOED);
        } else if (parentEmbargo != null) {
            status.add(FacetConstants.EMBARGOED_PARENT);
        }

        if (objAccess.equals(PatronAccess.none)) {
            status.add(FacetConstants.STAFF_ONLY_ACCESS);
        } else if (inheritedAclFactory.getPrincipalRoles(pid).containsKey(AccessPrincipalConstants.PUBLIC_PRINC)) {
            status.add(FacetConstants.PUBLIC_ACCESS);
        }

        if (inheritedAccess != null && inheritedAccess.equals(PatronAccess.none)
                && !objAccess.equals(PatronAccess.none)) {
            status.add(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS);
        }

        return status;
    }

}
