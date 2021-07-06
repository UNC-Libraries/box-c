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

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getMetadataContainerUri;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.exceptions.InvalidRelationshipException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a generic repository object within the main content tree.
 *
 * @author bbpennel
 * @author harring
 *
 */
public abstract class AbstractContentObject extends AbstractRepositoryObject implements ContentObject {

    protected AbstractContentObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
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
    @Override
    public BinaryObject addSourceMetadata(InputStream sourceMdStream, String sourceProfile)
            throws InvalidRelationshipException {
        if (sourceProfile == null || sourceProfile == "") {
            throw new InvalidRelationshipException("No source profile was provided");
        }

        // Populate a source metadata binary, using a random uuid for the identifier
        Model sourceModel = createDefaultModel();
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
    @Override
    public List<BinaryObject> listMetadata() {
        List<BinaryObject> descs = new ArrayList<>();

        Resource res = this.getResource();
        StmtIterator it = res.listProperties(IanaRelation.describedby);
        while (it.hasNext()) {
            Statement s = it.next();
            PID descPid = PIDs.get(s.getResource().getURI());

            descs.add(driver.getRepositoryObject(descPid, BinaryObjectImpl.class));
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
    @Override
    public BinaryObject getDescription() {
        Resource res = this.getResource();
        Statement s = res.getProperty(Cdr.hasMods);
        if (s != null) {
            PID binPid = PIDs.get(s.getResource().getURI());
            return driver.getRepositoryObject(binPid, BinaryObjectImpl.class);
        } else {
            return null;
        }
    }
}
