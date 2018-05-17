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
package edu.unc.lib.dl.ui.util;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;

/**
 * Utility methods for presenting datastreams in views.
 *
 * @author bbpennel
 *
 */
public class DatastreamUtil {

    private static final List<String> INDEXABLE_EXTENSIONS = asList(
            "doc", "docx", "htm", "html", "pdf", "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "xml");

    private DatastreamUtil() {
    }

    /**
     * Returns a URL for retrieving a specific datastream of the provided object.
     *
     * @param metadata metadata record for object
     * @param datastreamName name of datastream to return
     * @return url for accessing the datastream.
     */
    public static String getDatastreamUrl(BriefObjectMetadata metadata, String datastreamName) {
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
        if (!ORIGINAL_FILE.equals(preferredDS.getName())) {
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
    public static String getOriginalFileUrl(BriefObjectMetadata metadata) {
        return getDatastreamUrl(metadata, ORIGINAL_FILE);
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
    public static Datastream getPreferredDatastream(BriefObjectMetadata metadata, String datastreamName) {
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

}
