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
package edu.unc.lib.boxc.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.ResourceType;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.PcdmUse;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Repository object which contains a single original file and any number of
 * derivatives, alternate versions or technical metadata related to that file.
 * May only contain BinaryObjects as children, but can also have descriptive
 * metadata.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class FileObjectImpl extends AbstractContentObject implements FileObject {

    protected FileObjectImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public FileObjectImpl validateType() throws FedoraException {
        if (!isType(Cdr.FileObject.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a File Object.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.File;
    }

    /**
     * Adds the original file for this file object
     *
     * @param contentStream
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @return
     */
    @Override
    public BinaryObject addOriginalFile(URI storageUri, String filename,
            String mimetype, String sha1Checksum, String md5Checksum) {

        // Construct the path to where the original file will be created
        PID originalPid = getOriginalFilePid(pid);

        return addBinary(originalPid, storageUri, filename, mimetype, sha1Checksum, md5Checksum,
                null, RDF.type, PcdmUse.OriginalFile);
    }

    /**
     * Replaces the original file for this file object
     *
     * @param contentStream
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @return
     */
    @Override
    public BinaryObject replaceOriginalFile(URI storageUri, String filename,
            String mimetype, String sha1Checksum, String md5Checksum) {

        PID originalPid = getOriginalFilePid(pid);

        return repoObjFactory.createOrUpdateBinary(originalPid, storageUri,
                filename, mimetype, sha1Checksum, md5Checksum, null);
    }


    /**
     * Gets the original file for this file object
     *
     * @return
     */
    @Override
    public BinaryObject getOriginalFile() {
        return driver.getRepositoryObject(getOriginalFilePid(pid),
                BinaryObjectImpl.class);
    }

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
    @Override
    public BinaryObject addBinary(PID binPid, URI storageUri, String filename,
            String mimetype, Property associationRelation, Property typeRelation, Resource type) {

        return addBinary(binPid, storageUri, filename, mimetype, null, null,
                associationRelation, typeRelation, type);
    }

    @Override
    public BinaryObject addBinary(PID binPid, URI storageUri, String filename,
            String mimetype, String sha1Checksum, String md5Checksum,
            Property associationRelation, Property typeRelation, Resource type) {

        Model fileModel = null;
        if (type != null && typeRelation != null) {
            fileModel = ModelFactory.createDefaultModel();
            Resource resc = fileModel.createResource(binPid.getRepositoryPath());
            resc.addProperty(typeRelation, type);
        }

        // Create the binary object
        BinaryObject binObj = repoObjFactory.createOrUpdateBinary(binPid, storageUri, filename,
                mimetype, sha1Checksum, md5Checksum, fileModel);

        if (associationRelation != null) {
            // Establish association with original file relation
            repoObjFactory.createRelationship(binObj,
                    associationRelation, createResource(getOriginalFilePid(pid).getRepositoryPath()));
        }

        shouldRefresh();
        return binObj;
    }

    /**
     * Retrieve all of the binary objects contained by this FileObject.
     *
     * @return List of contained binary objects
     */
    @Override
    public List<BinaryObject> getBinaryObjects() {
        Resource resc = getResource();

        List<BinaryObject> binaries = new ArrayList<>();
        for (StmtIterator it = resc.listProperties(PcdmModels.hasFile); it.hasNext(); ) {
            PID binaryPid = PIDs.get(it.nextStatement().getResource().getURI());

            binaries.add(driver.getRepositoryObject(binaryPid, BinaryObjectImpl.class));
        }

        return binaries;
    }

    /**
     * Retrieve binary object by name from the set of binaries contained by this
     * FileObject.
     *
     * @param name name of the binary object to retrieve
     * @return BinaryObject identified by name
     * @throws NotFoundException thrown if no datastream with the given name is
     *             present in this FileObject.
     */
    @Override
    public BinaryObject getBinaryObject(String name) throws NotFoundException {
        Resource resc = getResource();

        StmtIterator it = resc.listProperties(PcdmModels.hasFile);
        try {
            for (; it.hasNext(); ) {
                PID binaryPid = PIDs.get(it.nextStatement().getResource().getURI());

                if (binaryPid.getComponentPath().endsWith("/" + name)) {
                    return driver.getRepositoryObject(binaryPid, BinaryObjectImpl.class);
                }
            }
        } finally {
            it.close();
        }

        throw new NotFoundException("No such binary " + name + " contained by " + pid);
    }
}
