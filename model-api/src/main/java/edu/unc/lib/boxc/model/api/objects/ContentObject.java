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

import java.io.InputStream;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.InvalidRelationshipException;

/**
 * Represents a generic repository object within the main content tree.
 * @author bbpennel
 */
public interface ContentObject extends RepositoryObject {

    /**
     * Adds source metadata file to this object
     *
     * @param sourceMdStream
     * @param sourceProfile
     *            identifies the encoding, profile, and/or origins of the
     *            sourceMdStream using an identifier defined in
     *            edu.unc.lib.dl.util.MetadataProfileConstants
     * @return BinaryObjects for source metadata
     * @throws InvalidRelationshipException
     *             in case no source profile was provided
     */
    BinaryObject addSourceMetadata(InputStream sourceMdStream, String sourceProfile)
            throws InvalidRelationshipException;

    /**
     * Gets a list of BinaryObjects for the metadata binaries associated with
     * this object.
     *
     * @return List of metadata BinaryObjects for this object
     */
    List<BinaryObject> listMetadata();

    /**
     * Gets the BinaryObject with the MODS for this object
     * @return the BinaryObject
     */
    BinaryObject getDescription();

}