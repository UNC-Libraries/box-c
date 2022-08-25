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
package edu.unc.lib.boxc.auth.api;

/**
 * Permissions for user actions within the repository
 *
 * @author bbpennel
 *
 */
public enum Permission {
    viewMetadata,
    viewAccessCopies,
    viewOriginal,
    // TODO replaces viewAdminUI and viewEmbargoed
    viewHidden,
    editDescription,
    bulkUpdateDescription,
    ingest,
    orderMembers,
    move,
    markForDeletion,
    markForDeletionUnit,
    destroy,
    destroyUnit,
    createCollection,
    createAdminUnit,
    changePatronAccess,
    assignStaffRoles,
    editResourceType,
    runEnhancements,
    reindex;

    private Permission() {
    }

    public static Permission getPermission(String permissionName) {
        for (Permission permission: Permission.values()) {
            if (permission.name().equals(permissionName)) {
                return permission;
            }
        }
        return null;
    }
}