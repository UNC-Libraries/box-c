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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.IanaRelation;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.PcdmUse;
import edu.unc.lib.dl.util.URIUtil;

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
public class FileObject extends ContentObject {

    private final String fileSetPath;
    private final URI fileSetUri;

    protected FileObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);

        fileSetPath = URIUtil.join(pid.getRepositoryPath(), DATA_FILE_FILESET);
        fileSetUri = URI.create(fileSetPath);
    }

    @Override
    public FileObject validateType() throws FedoraException {
        if (!isType(Cdr.FileObject.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a File Object.");
        }
        return this;
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
    public BinaryObject addOriginalFile(InputStream contentStream, String filename,
            String mimetype, String sha1Checksum, String md5Checksum) {

        // Construct the path to where the original file will be created
        String objectPath = constructOriginalFilePath();

        // Add the OriginalFile use type
        Model fileModel = ModelFactory.createDefaultModel();
        Resource resc = fileModel.createResource(objectPath);
        resc.addProperty(RDF.type, PcdmUse.OriginalFile);

        return repoObjFactory.createBinary(fileSetUri, ORIGINAL_FILE, contentStream,
                filename, mimetype, sha1Checksum, md5Checksum, fileModel);
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
    public BinaryObject replaceOriginalFile(InputStream contentStream, String filename,
            String mimetype, String sha1Checksum, String md5Checksum) {

        return repoObjFactory.updateBinary(fileSetUri, ORIGINAL_FILE, contentStream,
                filename, mimetype, sha1Checksum, md5Checksum, null);
    }


    /**
     * Gets the original file for this file object
     *
     * @return
     */
    public BinaryObject getOriginalFile() {
        return driver.getRepositoryObject(PIDs.get(constructOriginalFilePath()),
                BinaryObject.class);
    }

    private String constructOriginalFilePath() {
        return URIUtil.join(fileSetPath, ORIGINAL_FILE);
    }

    /**
     * Create and add a derivative of the original file to this file object.
     *
     * @param contentStream
     * @param mimetype
     * @param type
     * @return the created derivative as a binary object
     */
    public BinaryObject addDerivative(String slug, InputStream contentStream, String filename,
            String mimetype, Resource type) {
        return addBinary(slug, contentStream, filename, mimetype, IanaRelation.derivedfrom, RDF.type, type);
    }

    public BinaryObject addBinary(String slug, InputStream contentStream, String filename,
            String mimetype, Property associationRelation, Property typeRelation, Resource type) {

        String derivPath = URIUtil.join(fileSetPath, slug);

        Model fileModel = null;
        if (type != null && typeRelation != null) {
            fileModel = ModelFactory.createDefaultModel();
            Resource resc = fileModel.createResource(derivPath);
            resc.addProperty(typeRelation, type);
        }

        // Create the derivative binary object
        BinaryObject derivObj = repoObjFactory.createBinary(fileSetUri, slug, contentStream, filename,
                mimetype, null, null, fileModel);

        if (associationRelation != null) {
            // Establish association with original file relation
            repoObjFactory.createRelationship(derivObj.getPid(),
                    associationRelation, createResource(constructOriginalFilePath()));
        }

        return derivObj;
    }

    /**
     * Retrieve all of the binary objects contained by this FileObject.
     *
     * @return List of contained binary objects
     */
    public List<BinaryObject> getBinaryObjects() {
        Resource resc = getResource();

        List<BinaryObject> binaries = new ArrayList<>();
        for (StmtIterator it = resc.listProperties(PcdmModels.hasFile); it.hasNext(); ) {
            PID binaryPid = PIDs.get(it.nextStatement().getResource().getURI());

            binaries.add(driver.getRepositoryObject(binaryPid, BinaryObject.class));
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
    public BinaryObject getBinaryObject(String name) throws NotFoundException {
        Resource resc = getResource();

        StmtIterator it = resc.listProperties(PcdmModels.hasFile);
        try {
            for (; it.hasNext(); ) {
                PID binaryPid = PIDs.get(it.nextStatement().getResource().getURI());

                if (binaryPid.getComponentPath().endsWith("/" + name)) {
                    return driver.getRepositoryObject(binaryPid, BinaryObject.class);
                }
            }
        } finally {
            it.close();
        }

        throw new NotFoundException("No such binary " + name + " contained by " + pid);
    }
}
