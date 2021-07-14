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
package edu.unc.lib.boxc.auth.fcrepo.services;

import static edu.unc.lib.boxc.model.api.DatastreamType.getByIdentifier;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.auth.api.Permission;

/**
 * Helper methods for determining permissions of datastreams.
 *
 * @author bbpennel
 *
 */
public class DatastreamPermissionUtil {
    private final static Map<DatastreamType, Permission> DS_PERMISSION_MAP;
    static {
        DS_PERMISSION_MAP = new EnumMap<>(DatastreamType.class);
        DS_PERMISSION_MAP.put(DatastreamType.FULLTEXT_EXTRACTION, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.JP2_ACCESS_COPY, Permission.viewAccessCopies);
        DS_PERMISSION_MAP.put(DatastreamType.MD_DESCRIPTIVE, Permission.viewMetadata);
        DS_PERMISSION_MAP.put(DatastreamType.MD_DESCRIPTIVE_HISTORY, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.MD_EVENTS, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.ORIGINAL_FILE, Permission.viewOriginal);
        DS_PERMISSION_MAP.put(DatastreamType.TECHNICAL_METADATA, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.TECHNICAL_METADATA_HISTORY, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.THUMBNAIL_SMALL, Permission.viewMetadata);
        DS_PERMISSION_MAP.put(DatastreamType.THUMBNAIL_LARGE, Permission.viewMetadata);
    }

    private DatastreamPermissionUtil() {
    }

    /**
     * Determine the Permission which applies to accessing the specified datastream.
     *
     * @param dsName name of the datastream.  Required.
     * @return permission
     */
    public static Permission getPermissionForDatastream(String dsName) {
        if (StringUtils.isBlank(dsName)) {
            throw new IllegalArgumentException("A non-null datastream name must be provided");
        }

        DatastreamType datastream = getByIdentifier(dsName);
        // If the requested datastream is not a known type, consider it to be administrative
        if (datastream == null) {
            return viewHidden;
        }

        return getPermissionForDatastream(datastream);
    }

    /**
     * Determine the Permission which applies to accessing the specified datastream.
     *
     * @param datastream datastream type to check. Required.
     * @return permission
     */
    public static Permission getPermissionForDatastream(DatastreamType datastream) {
        Assert.notNull(datastream);

        return DS_PERMISSION_MAP.get(datastream);
    }
}
