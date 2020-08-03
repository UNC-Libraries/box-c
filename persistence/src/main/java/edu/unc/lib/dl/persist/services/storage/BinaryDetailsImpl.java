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
package edu.unc.lib.dl.persist.services.storage;

import java.net.URI;
import java.util.Date;

import edu.unc.lib.dl.persist.api.storage.BinaryDetails;

/**
 * Implementation of generic details of a binary file
 *
 * @author bbpennel
 */
public class BinaryDetailsImpl implements BinaryDetails {

    private URI uri;
    private Date lastModified;
    private long size;

    /**
     * @param lastModified
     * @param size
     */
    public BinaryDetailsImpl(URI uri, Date lastModified, long size) {
        this.uri = uri;
        this.lastModified = lastModified;
        this.size = size;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public URI getDestinationUri() {
        return uri;
    }

}
