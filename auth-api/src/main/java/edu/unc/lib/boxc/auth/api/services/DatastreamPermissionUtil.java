package edu.unc.lib.boxc.auth.api.services;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.api.DatastreamType.getByIdentifier;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.model.api.DatastreamType;

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
        DS_PERMISSION_MAP.put(DatastreamType.AUDIO_ACCESS_COPY, Permission.viewAccessCopies);
        DS_PERMISSION_MAP.put(DatastreamType.ACCESS_SURROGATE, Permission.viewAccessCopies);
        DS_PERMISSION_MAP.put(DatastreamType.MD_DESCRIPTIVE, Permission.viewMetadata);
        DS_PERMISSION_MAP.put(DatastreamType.MD_DESCRIPTIVE_HISTORY, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.MD_EVENTS, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.ORIGINAL_FILE, Permission.viewOriginal);
        DS_PERMISSION_MAP.put(DatastreamType.TECHNICAL_METADATA, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.TECHNICAL_METADATA_HISTORY, Permission.viewHidden);
        DS_PERMISSION_MAP.put(DatastreamType.THUMBNAIL_SMALL, Permission.viewAccessCopies);
        DS_PERMISSION_MAP.put(DatastreamType.THUMBNAIL_LARGE, Permission.viewAccessCopies);
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
        if (datastream == null) {
            throw new IllegalArgumentException("Must provide a non-null datastream");
        }

        return DS_PERMISSION_MAP.get(datastream);
    }
}
