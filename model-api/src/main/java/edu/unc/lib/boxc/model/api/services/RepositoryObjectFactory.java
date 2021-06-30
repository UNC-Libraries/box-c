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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or ied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.model.api.services;

import java.io.InputStream;
import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

/**
 * @author bbpennel
 */
public interface RepositoryObjectFactory {

    /**
     * Creates a new deposit record object with the given model.
     * Properties in the supplied model will be added to the deposit record.
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    DepositRecord createDepositRecord(Model model) throws FedoraException;

    DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException;

    /**
     * Creates a new AdminUnit with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    AdminUnit createAdminUnit(Model model) throws FedoraException;

    /**
     * Creates a new AdminUnit with the given model and provided pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    AdminUnit createAdminUnit(PID pid, Model model) throws FedoraException;

    /**
     * Creates a content root object structure with the provided properties
     *
     * @param path
     * @param model
     * @return
     * @throws FedoraException
     */
    URI createContentRootObject(URI path, Model model) throws FedoraException;

    /**
     * Creates a new CollectionObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    CollectionObject createCollectionObject(Model model) throws FedoraException;

    /**
     * Creates a new CollectionObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    CollectionObject createCollectionObject(PID pid, Model model) throws FedoraException;

    /**
     * Creates a new FolderObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    FolderObject createFolderObject(Model model) throws FedoraException;

    /**
     * Creates a new FolderObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    FolderObject createFolderObject(PID pid, Model model) throws FedoraException;

    /**
     * Creates a new WorkObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    WorkObject createWorkObject(Model model) throws FedoraException;

    /**
     * Creates a new WorkObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    WorkObject createWorkObject(PID pid, Model model) throws FedoraException;

    /**
     * Creates a new file object with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    FileObject createFileObject(Model model) throws FedoraException;

    /**
     * Creates a new file object with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    FileObject createFileObject(PID pid, Model model) throws FedoraException;

    /**
     * Creates a BinaryObject with the given PID, using the provided storage URI as the proxied external binary
     * location.
     *
     * @param pid pid of the binary
     * @param storageUri location where the binary is stored
     * @param filename filename for the binary
     * @param mimetype mimetype of the binary
     * @param sha1Checksum sha1 digest of the content.
     * @param md5Checksum md5 digest of the content.
     * @param model Model containing any properties to include in the description of this binary
     * @return the newly created BinaryObject
     * @throws FedoraException
     */
    BinaryObject createOrUpdateBinary(PID pid, URI storageUri, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model);

    /**
    * Creates a binary resource at the given path.
    *
    * @param path
    *        Repository path where the binary will be created
    * @param slug
    *        Name in the path for the binary resource. Optional.
    * @param content
    *        Input stream containing the binary content for this resource.
    * @param filename
    *        Filename of the binary content. Optional.
    * @param mimetype
    *        Mimetype of the content. Optional.
    * @param checksum
    *        SHA-1 digest of the content. Optional.
    * @param model
    *        Model containing additional triples to add to the new binary's metadata. Optional
    * @return URI of the newly created binary
    * @throws FedoraException
    */
    BinaryObject createBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model) throws FedoraException;

    /**
     * Updates a binary resource at the given path.
     *
     * @param path
     *        Repository path for the binary that will be updated
     * @param slug
     *        Name in the path for the binary resource.
     * @param content
     *        Input stream containing the binary content for this resource.
     * @param filename
     *        Filename of the binary content. Optional.
     * @param mimetype
     *        Mimetype of the content. Optional.
     * @param sha1Checksum
     *        SHA-1 digest of the content. Optional.
     * @param md5Checksum
     *        SHA-1 digest of the content. Optional.
     * @param model
     *        Model containing additional triples to add to the binary's metadata. Optional
     * @return URI of the updated binary
     * @throws FedoraException
     */
    BinaryObject updateBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model) throws FedoraException;

    /**
     * Add a member to the parent object.
     *
     * @param parent
     * @param member
     */
    void addMember(ContentObject parent, ContentObject member);

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    void createProperty(RepositoryObject subject, Property property, String object);

    /**
     * Creates a triple in Fedora by replacing the current property with the given property parameter
     * @param repoObj repository object to update the properties of.
     * @param property the property to update
     * @param object all of the new values for the property
     */
    void createExclusiveRelationship(RepositoryObject repoObj, Property property, Object object);

    /**
     * Deletes all triples with the given property predicate from the specified repository object.
     * @param repoObj repository object to remove properties from
     * @param property predicate of properties to remove
     */
    void deleteProperty(RepositoryObject repoObj, Property property);

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    void createRelationship(RepositoryObject subject, Property property, Resource object);

    /**
     * Creates the relevant triples in Fedora from the given model
     * @param subject
     * @param model
     */
    void createRelationships(RepositoryObject subject, Model model);

    /**
     * Creates a fedora object at the given location with the provided
     * properties, or replaces an existing object's triples with those in
     * the provided model
     *
     * @param uri
     * @param model
     * @return
     * @throws FedoraException
     */
    URI createOrTransformObject(URI uri, Model model) throws FedoraException;

    boolean objectExists(URI uri);

}