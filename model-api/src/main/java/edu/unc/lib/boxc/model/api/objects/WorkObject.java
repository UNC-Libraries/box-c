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
package edu.unc.lib.boxc.model.api.objects;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * A repository object which represents a single work, and should contain one or
 * more data files. A work may have a single primary object which is considered
 * the main work file, in which case the other data files are considered to be
 * supplemental.
 * @author bbpennel
 */
public interface WorkObject extends ContentContainerObject {

    /**
     * Clear the primary object set for this work.
     */
    void clearPrimaryObject();

    /**
     * Set the object with the given PID as the primary object for this work.
     * The primary object must be a file object and must be contained by this work.
     *
     * @param primaryPid
     */
    void setPrimaryObject(PID primaryPid);

    /**
     * Get the primary object for this work if one is assigned, otherwise return null.
     *
     * @return
     */
    FileObject getPrimaryObject();

    /**
     * Adds a new file object containing the provided input stream as its original file.
     *
     * @param contentStream
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @param md5Checksum
     * @return
     */
    FileObject addDataFile(URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum);

    FileObject addDataFile(URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum, Model model);

    /**
     * Adds a new file object containing the provided input stream as its
     * original file, using the provided pid as the identifier for the new
     * FileObject.
     *
     * @param filePid
     * @param storageUri
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @param md5Checksum
     * @param model
     * @return
     */
    FileObject addDataFile(PID filePid, URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum, Model model);

}