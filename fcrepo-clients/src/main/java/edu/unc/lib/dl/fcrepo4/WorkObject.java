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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DC;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.InvalidOperationForObjectType;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * A repository object which represents a single work, and should contain one or
 * more data files. A work may have a single primary object which is considered
 * the main work file, in which case the other data files are considered to be
 * supplemental.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class WorkObject extends ContentContainerObject {

    protected WorkObject(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public WorkObject validateType() throws FedoraException {
        if (!isType(Cdr.Work.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Work object.");
        }
        return this;
    }

    /**
     * Set the object with the given PID as the primary object for this work.
     * The primary object must be a file object and must be contained by this work.
     *
     * @param primaryPid
     */
    public void setPrimaryObject(PID primaryPid) {
        RepositoryObject repoObj = driver.getRepositoryObject(primaryPid);
        if (!(repoObj instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Cannot set " + primaryPid.getUUID()
                    + " as primary object, since objects of type " + repoObj.getClass().getName()
                    + " are not eligible.");
        }
        // Check that the file object is contained by this work
        Resource resc = getResource();
        Resource primaryResc = createResource(primaryPid.getRepositoryPath());
        if (!resc.hasProperty(PcdmModels.hasMember, primaryResc)) {
            throw new InvalidRelationshipException("Primary object must be a member of the Work");
        }
        // Add the relation
        repoObjFactory.createExclusiveRelationship(this, Cdr.primaryObject, primaryResc);
    }

    /**
     * Get the primary object for this work if one is assigned, otherwise return null.
     *
     * @return
     */
    public FileObject getPrimaryObject() {
        Resource resc = getResource();
        // Find the primary object relation if it is present
        Statement primaryStmt = resc.getProperty(Cdr.primaryObject);
        if (primaryStmt == null) {
            return null;
        }

        PID primaryPid = PIDs.get(primaryStmt.getResource().getURI());
        return driver.getRepositoryObject(primaryPid, FileObject.class);
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FileObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of WorkObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        return this;
    }

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
    public FileObject addDataFile(InputStream contentStream, String filename, String mimetype,
            String sha1Checksum, String md5Checksum) {

        return addDataFile(null, contentStream, filename, mimetype, sha1Checksum, md5Checksum, null);
    }

    public FileObject addDataFile(InputStream contentStream, String filename,
            String mimetype, String sha1Checksum, String md5Checksum, Model model) {
        return addDataFile(null, contentStream, filename, mimetype, sha1Checksum, md5Checksum, model);
    }

    /**
     * Adds a new file object containing the provided input stream as its
     * original file, using the provided pid as the identifier for the new
     * FileObject.
     *
     *
     * @param contentStream
     *            Inputstream containing the binary content for the data file. Required.
     * @param filename
     * @param mimetype
     * @param sha1Checksum
     * @param model
     *            model containing properties for the new fileObject
     * @return
     */
    public FileObject addDataFile(PID filePid, InputStream contentStream, String filename,
            String mimetype, String sha1Checksum, String md5Checksum, Model model) {

        if (contentStream == null) {
            throw new IllegalArgumentException("A non-null contentstream is required");
        }

        if (model == null) {
            model = ModelFactory.createDefaultModel();
        }
        model.getResource("").addProperty(DC.title, filename);

        // Create the file object
        FileObject fileObj;
        if (filePid == null) {
            fileObj = repoObjFactory.createFileObject(model);
        } else {
            fileObj = repoObjFactory.createFileObject(filePid, model);
        }
        // Add the binary content to it as its original file
        fileObj.addOriginalFile(contentStream, filename, mimetype, sha1Checksum, md5Checksum);

        // Add the new file object as a member of this Work
        repoObjFactory.addMember(this, fileObj);

        return fileObj;
    }
}
