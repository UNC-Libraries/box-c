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

import java.util.Arrays;
import java.util.List;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 *
 * @author count0
 *
 */
public class FedoraUtil {
    private String fedoraUrl;

    public static String getDatastreamUrl(Object object, String datastream, FedoraUtil fedoraUtil) {
        if (object instanceof String) {
            return fedoraUtil.getDatastreamUrl((String) object, datastream);
        }
        if (object instanceof BriefObjectMetadata) {
            return fedoraUtil.getDatastreamUrl((BriefObjectMetadata) object, datastream);
        }
        return null;
    }

    /**
     * Returns a URL for a specific datastream of the object identified by pid, according to the RESTful Fedora API.
     * Example: <fedoraBaseURL>/objects/uuid:5fdc16d9-8272-41f7-a7da-a953192174df/datastreams/DC/content
     *
     * @param pid
     * @param datastream
     * @return
     */
    public String getDatastreamUrl(String pid, String datastream) {
        StringBuilder url = new StringBuilder();
        url.append("content/").append(pid);
        if (!ContentModelHelper.Datastream.DATA_FILE.getName().equals(datastream)) {
            url.append("/").append(datastream);
        }
        return url.toString();
    }

    public String getDatastreamUrl(BriefObjectMetadata metadata, String datastreamName) {
        // Prefer the matching datastream from this object over the same datastream with a different pid prefix
        Datastream preferredDS = getPreferredDatastream(metadata, datastreamName);

        if (preferredDS == null) {
            return "";
        }

        StringBuilder url = new StringBuilder();

        if (preferredDS.getExtension() != null) {
            int extensionIndex = Arrays.binarySearch(new String[] { "doc", "docx", "htm", "html", "pdf", "ppt", "pptx",
                    "rtf", "txt", "xls", "xlsx", "xml" }, preferredDS.getExtension());
            if (extensionIndex >= 0) {
                url.append("indexable");
            }
        }

        url.append("content/");
        if (preferredDS.getOwner() == null) {
            url.append(metadata.getId());
        } else {
            url.append(preferredDS.getOwner());
        }
        if (!ContentModelHelper.Datastream.DATA_FILE.getName().equals(datastreamName)) {
            url.append("/").append(preferredDS.getName());
        }
        return url.toString();
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

    public String getFedoraUrl() {
        return fedoraUrl;
    }

    public void setFedoraUrl(String fedoraUrl) {
        this.fedoraUrl = fedoraUrl;
    }

}
