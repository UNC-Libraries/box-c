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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.TombstoneFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DC;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidRelationshipException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class WorkObjectImpl extends AbstractContentContainerObject implements WorkObject {
    private static final Logger log = LoggerFactory.getLogger(WorkObjectImpl.class);

    protected WorkObjectImpl(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public WorkObject validateType() throws FedoraException {
        if (!isType(Cdr.Work.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Work object.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Work;
    }

    /**
     * Clear the primary object set for this work.
     */
    @Override
    public void clearPrimaryObject() {
        repoObjFactory.deleteProperty(this, Cdr.primaryObject);
    }

    /**
     * Set the object with the given PID as the primary object for this work.
     * The primary object must be a file object and must be contained by this work.
     *
     * @param primaryPid
     */
    @Override
    public void setPrimaryObject(PID primaryPid) {
        RepositoryObject primaryObj = driver.getRepositoryObject(primaryPid);
        if (!(primaryObj instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Cannot set " + primaryPid.getUUID()
                    + " as primary object, since objects of type " + primaryObj.getClass().getName()
                    + " are not eligible.");
        }
        // Verify that the intended primary object is a child of this work
        RepositoryObject parent = primaryObj.getParent();
        if (!parent.getPid().equals(getPid())) {
            throw new InvalidRelationshipException("Primary object must be a member of the Work");
        }

        // Add the relation
        repoObjFactory.createExclusiveRelationship(this, Cdr.primaryObject, primaryObj.getResource());
    }

    /**
     * Get the primary object for this work if one is assigned, otherwise return null.
     *
     * @return
     */
    @Override
    public FileObject getPrimaryObject() {
        Resource resc = getResource();
        // Find the primary object relation if it is present
        Statement primaryStmt = resc.getProperty(Cdr.primaryObject);
        if (primaryStmt == null) {
            return null;
        }

        PID primaryPid = PIDs.get(primaryStmt.getResource().getURI());
        try {
            return driver.getRepositoryObject(primaryPid, FileObject.class);
        } catch (TombstoneFoundException e) {
            log.debug("Cannot retrieve primary object for {}", getPid().getId(), e);
        }
        return null;
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FileObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of WorkObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        member.shouldRefresh();
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
    @Override
    public FileObject addDataFile(URI storageUri, String filename, String mimetype,
            String sha1Checksum, String md5Checksum) {

        return addDataFile(null, storageUri, filename, mimetype, sha1Checksum, md5Checksum, null);
    }

    @Override
    public FileObject addDataFile(URI storageUri, String filename,
            String mimetype, String sha1Checksum, String md5Checksum, Model model) {
        return addDataFile(null, storageUri, filename, mimetype, sha1Checksum, md5Checksum, model);
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
    @Override
    public FileObject addDataFile(PID filePid, URI storageUri, String filename,
            String mimetype, String sha1Checksum, String md5Checksum, Model model) {

        if (storageUri == null) {
            throw new IllegalArgumentException("A non-null storage uri is required");
        }

        if (model == null) {
            model = ModelFactory.createDefaultModel();
        }
        if (filename == null) {
            filename = StringUtils.substringAfterLast(storageUri.toString(), "/");
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
        fileObj.addOriginalFile(storageUri, filename, mimetype, sha1Checksum, md5Checksum);

        // Add the new file object as a member of this Work
        repoObjFactory.addMember(this, fileObj);
        // Force the work and its new file to refresh to reflect membership change
        fileObj.shouldRefresh();
        shouldRefresh();

        return fileObj;
    }

    @Override
    public List<PID> getMemberOrder() {
        var memberOrder = getResource().getProperty(Cdr.memberOrder);
        if (memberOrder == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(memberOrder.getString().split("\\|")).map(PIDs::get).collect(Collectors.toList());
    }
}
