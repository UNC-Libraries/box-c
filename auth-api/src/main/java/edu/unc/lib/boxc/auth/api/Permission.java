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
    viewReducedResolutionImages,
    viewOriginal,
    // Staff Permissions
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
}