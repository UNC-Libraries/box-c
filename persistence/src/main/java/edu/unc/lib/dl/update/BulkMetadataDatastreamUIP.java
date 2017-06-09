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
package edu.unc.lib.dl.update;

import org.jdom2.Document;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Jul 13, 2015
 */
public class BulkMetadataDatastreamUIP extends MetadataUIP {
    private String lastModified;
    private String datastream;

    /**
     * @param pid
     * @param user
     * @param operation
     */
    public BulkMetadataDatastreamUIP(PID pid, String user, UpdateOperation operation,
            String datastream, String lastModified, Document content) {
        super(pid, user, operation);
        this.lastModified = lastModified;
        this.datastream = datastream;

        getIncomingData().put(datastream, content.getRootElement());
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getDatastream() {
        return datastream;
    }

    public void setDatastream(String datastream) {
        this.datastream = datastream;
    }
}
