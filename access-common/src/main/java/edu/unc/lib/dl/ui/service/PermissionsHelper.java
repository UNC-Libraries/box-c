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
package edu.unc.lib.dl.ui.service;

import static edu.unc.lib.dl.acl.fcrepo4.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.Permission.editDescription;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_SMALL;
import static org.springframework.util.Assert.notNull;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;

/**
 * Helper for determining permissions of view objects.
 *
 * @author bbpennel
 *
 */
public class PermissionsHelper {

    // RoleGroup value used to identify patron full public access
    private static final String PUBLIC_ROLE_VALUE = canViewOriginals.getPredicate() + "|" + PUBLIC_PRINC;

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
    public boolean hasOriginalAccess(AccessGroupSet principals, BriefObjectMetadata metadata) {
        return hasDatastreamAccess(principals, ORIGINAL_FILE, metadata);
    }

    /**
     * Returns true if the principals can access thumbnails belonging to
     * the requested object, if present.
     *
     * @param principals
     * @param metadata
     * @return
     */
    public boolean hasThumbnailAccess(AccessGroupSet principals, BriefObjectMetadata metadata) {
        return hasDatastreamAccess(principals, THUMBNAIL_SMALL, metadata);
    }

    /**
     * Returns true if the principals can access the image preview belonging to
     * the requested object, if present.
     *
     * @param principals
     * @param metadata
     * @return
     */
    public boolean hasImagePreviewAccess(AccessGroupSet principals, BriefObjectMetadata metadata) {
        return hasDatastreamAccess(principals, JP2_ACCESS_COPY, metadata);
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
            BriefObjectMetadata metadata) {
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

    private static boolean containsDatastream(BriefObjectMetadata metadata, String datastream) {
        return metadata.getDatastreamObjects().stream()
                .anyMatch(d -> d.getName().equals(datastream));
    }

    /**
     * Returns true if the provided principals have permission to edit the
     * objects description.
     *
     * @param principals agent principals
     * @param metadata object metadata
     * @return
     */
    public boolean hasEditAccess(AccessGroupSet principals, BriefObjectMetadata metadata) {
        notNull(principals, "Requires agent principals");
        notNull(metadata, "Requires metadata object");

        return accessControlService.hasAccess(metadata.getPid(), principals, editDescription);
    }

    /**
     * Determines if full public access is allowed for the provided object.
     *
     * @param metadata object
     * @return true if full public access is allow for the object
     */
    public boolean allowsPublicAccess(BriefObjectMetadata metadata) {
        if (metadata.getRoleGroup() == null) {
            return false;
        }
        return metadata.getRoleGroup().contains(PUBLIC_ROLE_VALUE);
    }

    /**
     * @return the accessControlService
     */
    public AccessControlService getAccessControlService() {
        return accessControlService;
    }

    /**
     * @param accessControlService the accessControlService to set
     */
    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
