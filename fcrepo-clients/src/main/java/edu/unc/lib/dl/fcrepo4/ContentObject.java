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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.MD_DESCRIPTIVE_FILE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getMetadataContainerUri;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.IanaRelation;

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
     * @return the BinaryObject for the descriptive record
     */
    public BinaryObject setDescription(InputStream modsStream) {
        URI mdURI = getMetadataContainerUri(pid);

        BinaryObject descObj = this.getDescription();
        if (descObj == null) {
            Model descModel = ModelFactory.createDefaultModel();
            descModel.getResource("").addProperty(RDF.type, Cdr.DescriptiveMetadata);

            descObj = repoObjFactory.createBinary(mdURI, MD_DESCRIPTIVE_FILE, modsStream,
                    null, "text/xml", null, null, null);

            repoObjFactory.createRelationship(this, Cdr.hasMods, descObj.getResource());

            return descObj;
        } else {
            return repoObjFactory.updateBinary(mdURI, MD_DESCRIPTIVE_FILE, modsStream,
                    null, "text/xml", null, null, null);
        }
    }

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
    public BinaryObject addSourceMetadata(InputStream sourceMdStream, String sourceProfile)
            throws InvalidRelationshipException {
        if (sourceProfile == null || sourceProfile == "") {
            throw new InvalidRelationshipException("No source profile was provided");
        }

        // Populate a source metadata binary, using a random uuid for the identifier
        Model sourceModel = ModelFactory.createDefaultModel();
        Resource sourceResc = sourceModel.getResource("");
        sourceResc.addProperty(RDF.type, Cdr.SourceMetadata);
        sourceResc.addProperty(Cdr.hasSourceMetadataProfile, sourceProfile);
        URI mdURI = getMetadataContainerUri(pid);
        BinaryObject srcObj = repoObjFactory.createBinary(mdURI, UUID.randomUUID().toString(), sourceMdStream,
                null, "text/plain", null, null, sourceModel);

        // Link the descriptive binary to the source
        BinaryObject descObj = getDescription();
        if (descObj != null) {
            repoObjFactory.createRelationship(descObj, IanaRelation.derivedfrom, srcObj.getResource());
        }

        return srcObj;
    }

    /**
     * Gets a list of BinaryObjects for the metadata binaries associated with
     * this object.
     *
     * @return List of metadata BinaryObjects for this object
     */
    public List<BinaryObject> listMetadata() {
        List<BinaryObject> descs = new ArrayList<>();

        Resource res = this.getResource();
        StmtIterator it = res.listProperties(IanaRelation.describedby);
        while (it.hasNext()) {
            Statement s = it.next();
            PID descPid = PIDs.get(s.getResource().getURI());

            descs.add(driver.getRepositoryObject(descPid, BinaryObject.class));
        }

        return descs;
    }

    @Override
    public RepositoryObject getParent() {
        return driver.getParentObject(this);
    }

    /**
     * Gets the BinaryObject with the MODS for this object
     * @return the BinaryObject
     */
    public BinaryObject getDescription() {
        Resource res = this.getResource();
        Statement s = res.getProperty(Cdr.hasMods);
        if (s != null) {
            PID binPid = PIDs.get(s.getResource().getURI());
            return driver.getRepositoryObject(binPid, BinaryObject.class);
        } else {
            return null;
        }
    }
}
