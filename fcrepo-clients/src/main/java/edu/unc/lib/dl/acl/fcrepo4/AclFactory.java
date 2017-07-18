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
package edu.unc.lib.dl.acl.fcrepo4;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fedora.PID;

/**
 * Interface for factories which provide access control details repository
 * resources
 *
 * @author bbpennel
 *
 */
public interface AclFactory {

    /**
     * Returns an aggregated map of principals to sets of granted roles
     *
     * @param pid
     * @return
     */
    public Map<String, Set<String>> getPrincipalRoles(PID pid);

    /**
     * Returns the patron access setting for this object if specified, otherwise
     * inherit from parent is returned
     *
     * @param pid
     * @return PatronAccess enumeration value for this object's access setting
     *         if specified, otherwise parent.
     */
    public PatronAccess getPatronAccess(PID pid);

    /**
     * Returns the expiration date of an embargo imposed on the object, or null if no embargo is specified
     *
     * @param pid
     * @return
     */
    public Date getEmbargoUntil(PID pid);

    /**
     * Returns true if the object specified is marked for deletion.
     *
     * @param pid
     * @return
     */
    public boolean isMarkedForDeletion(PID pid);
}
