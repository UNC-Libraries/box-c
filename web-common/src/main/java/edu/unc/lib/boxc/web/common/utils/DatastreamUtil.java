package edu.unc.lib.boxc.web.common.utils;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;

import java.util.List;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility methods for presenting datastreams in views.
 *
 * @author bbpennel
 *
 */
public class DatastreamUtil {

    private static String datastreamEndpoint;

    private static final List<String> INDEXABLE_EXTENSIONS = asList(
            "doc", "docx", "htm", "html", "pdf", "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "xml");

    private DatastreamUtil() {
    }

    public static void setDatastreamEndpoint(String uri) {
        datastreamEndpoint = uri;
    }

    /**
     * Returns a URL for retrieving a specific datastream of the provided object.
     *
     * @param metadata metadata record for object
     * @param datastreamName name of datastream to return
     * @return url for accessing the datastream.
     */
    public static String getDatastreamUrl(ContentObjectRecord metadata, String datastreamName) {
        // Prefer the matching datastream from this object over the same datastream with a different pid prefix
        Datastream preferredDS = getPreferredDatastream(metadata, datastreamName);

        if (preferredDS == null) {
            return "";
        }

        StringBuilder url = new StringBuilder();

        if (!isBlank(preferredDS.getExtension())) {
            if (INDEXABLE_EXTENSIONS.contains(preferredDS.getExtension())) {
                url.append("indexable");
            }
        }

        url.append("content/");
        if (isBlank(preferredDS.getOwner())) {
            url.append(metadata.getId());
        } else {
            url.append(preferredDS.getOwner());
        }
        if (!ORIGINAL_FILE.getId().equals(preferredDS.getName())) {
            url.append("/").append(preferredDS.getName());
        }
        return url.toString();
    }

    /**
     * Returns a URL for retrieving the original file datastream of the provided
     * object if present, otherwise a blank string is returned.
     *
     * @param metadata metadata record for object
     * @return url for accessing the datastream.
     */
    public static String getOriginalFileUrl(ContentObjectRecord metadata) {
        return getDatastreamUrl(metadata, ORIGINAL_FILE.getId());
    }

    /**
     * @param metadata metadata record for object
     * @param pattern mimetype suffix
     * @return
     */
    public static boolean originalFileMimetypeMatches(ContentObjectRecord metadata, String pattern) {
        var formatTypes = metadata.getFileFormatType();
        if (formatTypes == null || formatTypes.isEmpty()) {
            return false;
        }

        for (String format : formatTypes) {
            if (format.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the preferred instance of the datastream identified by datastreamName. The preferred datastream is the
     * datastream owned by the object itself, rather then a reference to a datastream owed by another object.  This
     * arises in cases where an object has a defaultWebObject which is another object.
     *
     * @param metadata
     * @param datastreamName
     * @return
     */
    public static Datastream getPreferredDatastream(ContentObjectRecord metadata, String datastreamName) {
        Datastream preferredDS = null;
        List<Datastream> dataStreams = metadata.getDatastreamObjects();

        if (dataStreams == null) {
            return null;
        }

        for (Datastream ds : dataStreams) {
            if (ds.getName().equals(datastreamName)) {
                preferredDS = ds;

                if (metadata.getId().equals(ds.getOwner())) {
                    break;
                }
            }
        }

        return preferredDS;
    }

    /**
     * Returns the ID of the owner of the thumbnail for the provided object.
     * @param metadata
     * @return id of the owner of the thumbnail, or null if the thumbnail is not present
     */
    public static String getThumbnailOwnerId(ContentObjectRecord metadata) {
        // Prefer the matching derivative from this object
        Datastream preferredDS = getPreferredDatastream(metadata, DatastreamType.JP2_ACCESS_COPY.getId());

        // Ensure that this item has the appropriate thumbnail
        if (preferredDS == null) {
            return null;
        }

        return isBlank(preferredDS.getOwner()) ?  metadata.getId() : preferredDS.getOwner();
    }

    /**
     * @param id
     * @return constructed large thumbnail url, using the provided id
     */
    public static String constructThumbnailUrl(String id) {
        return constructThumbnailUrl(id, "large");
    }

    /**
     * @param id
     * @param size
     * @return constructed thumbnail url, using the provided id and size
     */
    public static String constructThumbnailUrl(String id, String size) {
        if (id == null) {
            return null;
        }
        String selectedSize = size == null ? "large" : size;
        selectedSize = selectedSize.toLowerCase().trim();
        StringBuilder url = new StringBuilder(datastreamEndpoint);
        return url.append("thumb/")
                .append(id)
                .append("/")
                .append(selectedSize)
                .toString();
    }
}
