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
package edu.unc.lib.dcr.migration.content;

import static edu.unc.lib.dcr.migration.MigrationConstants.convertBxc3RefToPid;
import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty.hasSourceMimeType;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.AGGREGATE_WORK;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.COLLECTION;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.CONTAINER;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel.SIMPLE;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.createdDate;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty.lastModifiedDate;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Relationship.originalDeposit;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.ORIGINAL_DS;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.getObjectModel;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.listDatastreamVersions;
import static edu.unc.lib.dcr.migration.paths.PathIndex.ORIGINAL_TYPE;
import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static java.nio.file.Files.newInputStream;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;

import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * Action to transform a content object from bxc3 into a depositable structure
 * for bxc5.
 *
 * @author bbpennel
 */
public class ContentObjectTransformer extends RecursiveAction {

    private static final long serialVersionUID = 1L;

    private static final Logger log = getLogger(ContentObjectTransformer.class);

    private PathIndex pathIndex;
    private DepositModelManager modelManager;
    private boolean topLevelAsUnit;
    private ContentObjectTransformerManager manager;
    private RepositoryPIDMinter pidMinter;

    private PID pid;
    private Resource parentType;

    /**
     *
     */
    public ContentObjectTransformer(PID pid, Resource parentType) {
        this.pid = pid;
        this.parentType = parentType;
    }

    @Override
    protected void compute() {
        Path foxmlPath = pathIndex.getPath(pid);

        // Deserialize the foxml document
        Document foxml;
        try {
            foxml = createSAXBuilder().build(newInputStream(foxmlPath));
        } catch (IOException | JDOMException e) {
            throw new RepositoryException("Failed to read FOXML for " + pid, e);
        }

        try {
        // Retrieve all properties/relationships for the object
        Model model = getObjectModel(foxml);
        Resource bxc3Resc = model.getResource(toBxc3Uri(pid));

        if (isMarkedForDeletion(bxc3Resc)) {
            log.warn("Skipping transformation of object {}, it is marked for deletion", pid);
            return;
        }

        // Determine what type of resource this should be in boxc5
        Resource resourceType = getResourceType(bxc3Resc);
        if (resourceType == null) {
            return;
        }

        Model depositModel = createDefaultModel();

        if (resourceType.equals(Cdr.FileObject)) {
            populateFileObject(bxc3Resc, depositModel, foxml);
        } else {
            populateContainerObject(bxc3Resc, resourceType, depositModel);
        }

        Resource depResc = depositModel.getResource(pid.getRepositoryPath());

        populateTimestamps(bxc3Resc, depResc);
        populateOriginalDeposit(bxc3Resc, depResc);

        // TODO set patron access

        // TODO set title, based on dc title and/or label

        // TODO copy most recent MODS to deposit directory

        // Push triples for this object to the shared model for this deposit
        modelManager.addTriples(depositModel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateTimestamps(Resource bxc3Resc, Resource depResc) {
        String created = bxc3Resc.getProperty(createdDate.getProperty()).getString();
        String updated = bxc3Resc.getProperty(lastModifiedDate.getProperty()).getString();
        depResc.addLiteral(CdrDeposit.createTime, created);
        depResc.addLiteral(CdrDeposit.lastModifiedTime, updated);
    }

    private void populateOriginalDeposit(Resource bxc3Resc, Resource depResc) {
        Statement originalDepStmt = bxc3Resc.getProperty(originalDeposit.getProperty());
        if (originalDepStmt == null) {
            return;
        }
        PID originalDepPid = convertBxc3RefToPid(originalDepStmt.getResource());
        depResc.addProperty(CdrDeposit.originalDeposit, createResource(originalDepPid.getRepositoryPath()));
    }

    private void populateContainerObject(Resource bxc3Resc, Resource resourceType, Model depositModel) {
        Bag containerBag = depositModel.getBag(pid.getRepositoryPath());
        containerBag.addProperty(RDF.type, resourceType);

        List<PID> contained = listContained(bxc3Resc);
        for (PID containedPid : contained) {
            // Make sure we can find FOXML for the child before we add it
            if (pathIndex.getPath(containedPid) == null) {
                log.error("Dangling containment: {} references {}, but no foxml found for the child",
                        bxc3Resc, containedPid);
                continue;
            }

            // Add the child to its parent bag
            containerBag.add(createResource(containedPid.getRepositoryPath()));

            // Spawn and execute transformer for children
            ContentObjectTransformer childTransformer = manager.createTransformer(containedPid, resourceType);
            childTransformer.fork();
        }

        if (Cdr.Work.equals(resourceType)) {
            Statement dwoStmt = bxc3Resc.getProperty(defaultWebObject.getProperty());
            if (dwoStmt != null) {
                PID primaryObjPid = convertBxc3RefToPid(dwoStmt.getResource());
                containerBag.addProperty(Cdr.primaryObject, createResource(primaryObjPid.getRepositoryPath()));
            }
        }

        // TODO transform PREMIS and copy to deposit directory
        // TODO set staff access
    }

    private void populateFileObject(Resource bxc3Resc, Model depositModel, Document foxml) {
        Resource fileResc;
        Bag workBag = null;
        if (Cdr.Work.equals(parentType)) {
            fileResc = depositModel.getResource(pid.getRepositoryPath());
        } else {
            // Use the pid of the current object to make a new work object
            workBag = depositModel.getBag(pid.getRepositoryPath());
            workBag.addProperty(RDF.type, Cdr.Work);

            // Build a new resource for the file object
            PID newFilePid = pidMinter.mintContentPid();
            fileResc = depositModel.getResource(newFilePid.getRepositoryPath());

            // Add the new file resource as the primary object of the work
            workBag.add(fileResc);
            workBag.addProperty(Cdr.primaryObject, fileResc);
        }

        fileResc.addProperty(RDF.type, Cdr.FileObject);

        Statement mimeTypeStmt = bxc3Resc.getProperty(hasSourceMimeType.getProperty());
        if (mimeTypeStmt != null) {
            fileResc.addProperty(CdrDeposit.mimetype, mimeTypeStmt.getString());
        }

        List<DatastreamVersion> originalVersions = listDatastreamVersions(foxml, ORIGINAL_DS);
        if (originalVersions.size() > 0) {
            DatastreamVersion lastV = originalVersions.get(originalVersions.size() - 1);
            fileResc.addLiteral(CdrDeposit.md5sum, lastV.getMd5());
            fileResc.addLiteral(CdrDeposit.createTime, lastV.getCreated());
            fileResc.addLiteral(CdrDeposit.lastModifiedTime, lastV.getCreated());
            fileResc.addProperty(CdrDeposit.size, lastV.getSize());
        }

        // Populate the original file path
        Path originalPath = pathIndex.getPath(pid, ORIGINAL_TYPE);
        if (originalPath == null) {
            log.warn("No original file path for {}", pid);
        } else {
            fileResc.addLiteral(stagingLocation, originalPath.toUri().toString());
        }

        // TODO transform PREMIS, making it refer to the FILE object rather than
        // work, and copy to deposit directory
    }

    private Resource getResourceType(Resource bxc3Resc) {
        Set<Resource> contentModels = new HashSet<>();
        StmtIterator it = bxc3Resc.listProperties(hasModel.getProperty());
        while (it.hasNext()) {
            contentModels.add(it.next().getResource());
        }

        if (contentModels.contains(AGGREGATE_WORK.getResource())) {
            return Cdr.Work;
        }
        if (contentModels.contains(COLLECTION.getResource())) {
            if (Cdr.AdminUnit.equals(parentType) || !topLevelAsUnit) {
                return Cdr.Collection;
            } else {
                return Cdr.AdminUnit;
            }
        }
        if (contentModels.contains(SIMPLE.getResource())) {
            return Cdr.FileObject;
        }
        if (contentModels.contains(CONTAINER.getResource())) {
            return Cdr.Folder;
        }

        log.warn("Unsupported content type for {} with models: {}", bxc3Resc, contentModels);
        return null;
    }

    private List<PID> listContained(Resource bxc3Resc) {
        List<PID> contained = new ArrayList<>();
        StmtIterator it = bxc3Resc.listProperties(contains.getProperty());
        while (it.hasNext()) {
            Resource containedResc = it.next().getResource();
            contained.add(convertBxc3RefToPid(containedResc));
        }
        return contained;
    }

    private boolean isMarkedForDeletion(Resource resc) {
        return resc.hasLiteral(FedoraProperty.state.getProperty(), "Deleted");
    }

    public void setPathIndex(PathIndex pathIndex) {
        this.pathIndex = pathIndex;
    }

    public void setModelManager(DepositModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void setTopLevelAsUnit(boolean topLevelAsUnit) {
        this.topLevelAsUnit = topLevelAsUnit;
    }

    public void setManager(ContentObjectTransformerManager manager) {
        this.manager = manager;
    }

    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    public PID getPid() {
        return pid;
    }
}
