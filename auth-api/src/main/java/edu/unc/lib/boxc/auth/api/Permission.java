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