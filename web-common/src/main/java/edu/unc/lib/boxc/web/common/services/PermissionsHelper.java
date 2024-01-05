package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.springframework.util.Assert.notNull;

/**
 * Helper for determining permissions of view objects.
 *
 * @author bbpennel
 *
 */
public class PermissionsHelper {
    private AccessControlService accessControlService;

    public PermissionsHelper() {
    }

    /**
     * Returns true if the principals can access the original file belonging to
     * the requested object, if present.
     *
     * @param principals
     * @param metadata
     * @return
     */
    public boolean hasOriginalAccess(AccessGroupSet principals, ContentObjectRecord metadata) {
        return hasDatastreamAccess(principals, ORIGINAL_FILE, metadata);
    }

    /**
     * Returns true if the provided principals have rights to access the
     * requested datastream, and the datastream is present.
     *
     * @param principals agent principals
     * @param datastream type of datastream being requested
     * @param metadata object
     * @return
     */
    public boolean hasDatastreamAccess(AccessGroupSet principals, DatastreamType datastream,
                                       ContentObjectRecord metadata) {
        notNull(principals, "Requires agent principals");
        notNull(datastream, "Requires datastream type");
        notNull(metadata, "Requires metadata object");

        String dsIdentifier = datastream.getId();
        if (metadata.getDatastreamObjects() == null
                || !containsDatastream(metadata, dsIdentifier)) {
            return false;
        }
        Permission permission = getPermissionForDatastream(datastream);
        return accessControlService.hasAccess(metadata.getPid(), principals, permission);
    }

    private static boolean containsDatastream(ContentObjectRecord metadata, String datastream) {
        return metadata.getDatastreamObjects().stream()
                .anyMatch(d -> d.getName().equals(datastream));
    }

    /**
     * @param accessControlService the accessControlService to set
     */
    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}