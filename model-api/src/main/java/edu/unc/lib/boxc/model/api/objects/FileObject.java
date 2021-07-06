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
import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Repository object which contains a single original file and any number of
 * derivatives, alternate versions or technical metadata related to that file.
 * May only contain BinaryObjects as children, but can also have descriptive
 * metadata.
 * @author bbpennel
 */
public interface FileObject extends ContentObject {

    /**
     * Adds the original file for this file object
     *
     * @param contentStream
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @return
     */
    BinaryObject addOriginalFile(URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum);

    /**
     * Replaces the original file for this file object
     *
     * @param contentStream
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @return
     */
    BinaryObject replaceOriginalFile(URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum);

    /**
     * Gets the original file for this file object
     *
     * @return
     */
    BinaryObject getOriginalFile();

    /**
     * Create and add a binary to this file object.
     *
     * @param binPid the PID of the binary to add
     * @param storageUri uri of the content for this binary
     * @param filename name of the binary
     * @param mimetype mimetype
     * @param associationRelation if provided, the binary will relate to the original binary with this property.
     * @param typeRelation relation for defining the type for this binary
     * @param type the type for this binary
     * @return the new binary
     */
    BinaryObject addBinary(PID binPid, URI storageUri, String filename, String mimetype,
            Property associationRelation, Property typeRelation, Resource type);

    BinaryObject addBinary(PID binPid, URI storageUri, String filename, String mimetype, String sha1Checksum,
            String md5Checksum, Property associationRelation, Property typeRelation, Resource type);

    /**
     * Retrieve all of the binary objects contained by this FileObject.
     *
     * @return List of contained binary objects
     */
    List<BinaryObject> getBinaryObjects();

    /**
     * Retrieve binary object by name from the set of binaries contained by this
     * FileObject.
     *
     * @param name name of the binary object to retrieve
     * @return BinaryObject identified by name
     * @throws NotFoundException thrown if no datastream with the given name is
     *             present in this FileObject.
     */
    BinaryObject getBinaryObject(String name) throws NotFoundException;

}