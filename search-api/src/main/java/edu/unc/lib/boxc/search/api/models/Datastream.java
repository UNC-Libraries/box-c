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
package edu.unc.lib.boxc.search.api.models;

/**
 * A record representing a datastream held by a repository object
 *
 * @author bbpennel
 */
public interface Datastream {

    /**
     * @return identifier of this specific instance of a datastream, including the owner id and datastream name
     */
    String getDatastreamIdentifier();

    /**
     * @return Name of this datastream, generally the identifier of the datastream independent of the object holding it
     */
    String getName();

    /**
     * @return identifier of the object to which this datastream belongs
     */
    String getOwner();

    /**
     * @return filesize in byte for this datastream
     */
    Long getFilesize();

    /**
     * @return mimetype of this datastream
     */
    String getMimetype();

    /**
     * @return file extension for this datastream
     */
    String getExtension();

    /**
     * @return Checksum/digest for this datastream
     */
    String getChecksum();

    /**
     * @return filename of the datastream
     */
    String getFilename();

    /**
     * @return technical details of the datastream, such as dimensions
     */
    String getExtent();

}