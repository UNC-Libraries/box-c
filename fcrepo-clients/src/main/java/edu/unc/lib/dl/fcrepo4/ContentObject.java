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

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * Represents a generic repository object within the main content tree.
 *
 * @author bbpennel
 * @author harring
 *
 */
public abstract class ContentObject extends RepositoryObject {

    protected ContentObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    /**
     * If no description exists, adds description information to this object (which includes a MODS record).
     * If description already exists, updates description information for this object.
     * @param modsStream
     * @return the FileObject containing the BinaryObject for the MODS
     */
    public FileObject setDescription(InputStream modsStream) {
        FileObject fileObj = this.getDescription();
        if (fileObj == null) {
            Model descModel = ModelFactory.createDefaultModel();
            descModel.getResource("").addProperty(RDF.type, Cdr.DescriptiveMetadata);

            fileObj = repoObjFactory.createFileObject(descModel);

            BinaryObject mods = fileObj.addOriginalFile(modsStream, null, "text/xml", null, null);
            repoObjFactory.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());
            repoObjFactory.createRelationship(pid, Cdr.hasMods, mods.getResource());
            return fileObj;
        } else {
            fileObj.replaceOriginalFile(modsStream, null, "text/xml", null, null);
            return fileObj;
        }
    }

    /**
     * Adds description information to this object, which includes source
     * metadata and a MODS record derived from it
     *
     * @param sourceMdStream
     * @param sourceProfile,
     *            identifies the encoding, profile, and/or origins of the
     *            sourceMdStream using an identifier defined in
     *            edu.unc.lib.dl.util.MetadataProfileConstants
     * @param modsStream
     * @return a FileObject containing BinaryObjects for source metadata and
     *         MODS
     * @throws InvalidRelationshipException
     *             in case no source profile was provided
     */
    public FileObject addDescription(InputStream sourceMdStream, String sourceProfile,
            InputStream modsStream) throws InvalidRelationshipException {
        if (sourceProfile == null || sourceProfile == "") {
            throw new InvalidRelationshipException("No source profile was provided");
        }
        FileObject fileObj = createFileObject();

        BinaryObject orig = fileObj.addOriginalFile(sourceMdStream, null, "text/plain", null, null);
        repoObjFactory.createProperty(orig.getPid(), Cdr.hasSourceMetadataProfile, sourceProfile);
        repoObjFactory.createRelationship(orig.getPid(), RDF.type, Cdr.SourceMetadata);
        repoObjFactory.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());

        BinaryObject mods = fileObj.addDerivative(null, modsStream, null, "text/plain", null);
        repoObjFactory.createRelationship(pid, Cdr.hasMods, mods.getResource());

        return fileObj;
    }

    /**
     * Gets the FileObject with source metadata and MODS for this object
     * @return the FileObject
     */
    public FileObject getDescription() {
        Resource res = this.getResource();
        Statement s = res.getProperty(PcdmModels.hasRelatedObject);
        if (s != null) {
            PID fileObjPid = PIDs.get(s.getResource().getURI());
            return driver.getRepositoryObject(fileObjPid, FileObject.class);
        } else {
            return null;
        }
    }

    @Override
    public RepositoryObject getParent() {
        return driver.getParentObject(this);
    }

    /**
     * Gets the BinaryObject with the MODS for this object
     * @return the BinaryObject
     */
    public BinaryObject getMODS() {
        Resource res = this.getResource();
        Statement s = res.getProperty(Cdr.hasMods);
        if (s != null) {
            PID binPid = PIDs.get(s.getResource().getURI());
            return driver.getRepositoryObject(binPid, BinaryObject.class);
        } else {
            return null;
        }
    }

    private FileObject createFileObject() {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        return fileObj;
    }

}
