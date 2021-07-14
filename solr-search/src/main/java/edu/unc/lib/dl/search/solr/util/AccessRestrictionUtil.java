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
package edu.unc.lib.dl.search.solr.util;

import org.apache.solr.client.solrj.SolrQuery;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GlobalPermissionEvaluator;

/**
 * Utility for constructing access restrictions for solr searches.
 *
 * @author bbpennel
 *
 */
public class AccessRestrictionUtil {

    private SearchSettings searchSettings;

    private boolean disablePermissionFiltering;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    /**
     * Adds access restrictions to the provided SolrQuery.
     *
     * If there are no access groups in the provided group set, then an
     * AccessRestrictionException is thrown as it is invalid for a user to have
     * no permissions. If the user is an admin, then do not restrict access
     *
     * @param query Solr query to add restriction to.
     * @param principals set of access principals for constructing restriction.
     * @throws AccessRestrictionException thrown if no groups are provided.
     */
    public void add(SolrQuery query, AccessGroupSet principals)
            throws AccessRestrictionException {
        String restrictions = getRestrictions(principals);
        if (restrictions != null) {
            query.addFilterQuery(restrictions);
        }
    }

    /**
     * Compute access restrictions query filter.
     *
     * If there are no access groups in the provided group set, then an
     * AccessRestrictionException is thrown as it is invalid for a user to have
     * no permissions. If the user is an admin, then null is returned.
     * @param principals
     * @return
     */
    public String getRestrictions(AccessGroupSet principals) {
        // Skip adding permission filters if disabled
        if (disablePermissionFiltering) {
            return null;
        }

        // Agent must provide principals
        if (principals == null || principals.size() == 0) {
            throw new AccessRestrictionException("No access groups were provided.");
        }

        // If the agent has any global permissions then no filtering is necessary.
        if (globalPermissionEvaluator.hasGlobalPrincipal(principals)) {
            return null;
        }

        boolean allowPatronAccess = searchSettings.getAllowPatronAccess();
        String joinedGroups = principals.joinAccessGroups(" OR ", null, true);
        StringBuilder sb = new StringBuilder();
        if (allowPatronAccess) {
            sb.append("readGroup:(").append(joinedGroups).append(')')
                    .append(" OR adminGroup:(").append(joinedGroups).append(')');
        } else {
            sb.append("adminGroup:(").append(joinedGroups).append(')');
        }
        return sb.toString();
    }

    /**
     * @param searchSettings the searchSettings to set
     */
    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    /**
     * @param disablePermissionFiltering the disablePermissionFiltering to set
     */
    public void setDisablePermissionFiltering(boolean disablePermissionFiltering) {
        this.disablePermissionFiltering = disablePermissionFiltering;
    }

    /**
     * @param globalPermissionEvaluator the globalPermissionEvaluator to set
     */
    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }
}
