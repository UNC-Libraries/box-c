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

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;

/**
 * Implementation of the service to evaluate and retrieve CDR access controls in
 * a fcrepo4 repository.
 *
 * @author bbpennel
 *
 */
public class AccessControlServiceImpl implements AccessControlService {

    private InheritedPermissionEvaluator permissionEvaluator;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @Deprecated
    @Override
    public ObjectAccessControlsBean getObjectAccessControls(PID pid) {
        // TODO stub
        return null;
    }

    @Override
    public boolean hasAccess(PID pid, AccessGroupSet principals, Permission permission) {
        // Check if there are any global agents, if so evaluate immediately against requested permission
        if (globalPermissionEvaluator.hasGlobalPermission(principals, permission)) {
            return true;
        }

        return permissionEvaluator.hasPermission(pid, principals, permission);
    }

    @Override
    public void assertHasAccess(PID pid, AccessGroupSet principals, Permission permission)
            throws AccessRestrictionException {
        assertHasAccess(null, pid, principals, permission);
    }

    @Override
    public void assertHasAccess(String message, PID pid, AccessGroupSet principals, Permission permission)
            throws AccessRestrictionException {
        if (!hasAccess(pid, principals, permission)) {
            throw new AccessRestrictionException(message);
        }
    }

    public void setPermissionEvaluator(InheritedPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

}
