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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;

/**
 * A Deposit Record repository object, which tracks information pertaining to a single deposit.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class DepositRecord extends RepositoryObject {

    protected DepositRecord(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    /**
     *  Adds the given file as a manifest for this deposit.
     *
     * @param manifest File containing the manifest content
     * @param mimetype mimetype string of the manifest file
     * @return BinaryObject representing the newly created manifest object
     * @throws FedoraException
     */
    public BinaryObject addManifest(File manifest, String mimetype)
            throws FedoraException, IOException {

        InputStream contentStream = new FileInputStream(manifest);
        return addManifest(contentStream, manifest.getName(), mimetype);
    }

    /**
     * Adds the given inputstream as the content of a manifest for this deposit.
     *
     * @param manifestStream inputstream containing the binary content for this manifest
     * @param filename filename for the manifest
     * @param mimetype mimetype for the content of the manifest
     * @return representing the newly created manifest object
     * @throws FedoraException
     */
    public BinaryObject addManifest(InputStream manifestStream, String filename, String mimetype)
            throws FedoraException {
        URI manifestsUri = getManifestsUri();
        return repoObjFactory.createBinary(manifestsUri, null, manifestStream, filename,
                mimetype, null, null, model);
    }

    /**
     * Retrieves the requested manifest of this deposit record
     *
     * @param pid
     * @return The requested manifest as a BinaryObject or null if the pid was
     *         not a component of this deposit record
     * @throws FedoraException
     */
    public BinaryObject getManifest(PID pid) throws FedoraException {
        if (!this.pid.containsComponent(pid)) {
            return null;
        }
        return driver.getRepositoryObject(pid, BinaryObject.class);
    }

    /**
     * Retrieves a list of pids for manifests contained by this deposit record
     *
     * @return
     * @throws FedoraException
     */
    public List<PID> listManifests() throws FedoraException {
        return addPidsToList(Cdr.hasManifest);
    }

    /**
     * Establishes a relationship between the deposit record and each object
     * that was added to the deposit.
     * @param depositPID
     * @param children
     * @return the DepositRecord itself, to allow method chaining
     */
    public DepositRecord addIngestedObjects(List<Resource> children) {
        Model triples = ModelFactory.createDefaultModel();
        Resource res = triples.createResource(getPid().getURI());
        for (Resource child : children) {
            res.addProperty(Cdr.hasIngestedObject, child);
        }
        // SPARQL update
        repoObjFactory.createRelationships(getPid(), triples);
        return this;
    }

    /**
     * Retrieves a list of pids for objects contained by this deposit record
     * @return
     * @throws FedoraException
     */
    public List<PID> listDepositedObjects() throws FedoraException {
        return addPidsToList(Cdr.hasIngestedObject);
    }

    @Override
    public DepositRecord addPremisEvents(List<PremisEventObject> events) throws FedoraException {
        return (DepositRecord) super.addPremisEvents(events);
    }

    /**
     * Ensure that the object retrieved has the DepositRecord type
     */
    @Override
    public DepositRecord validateType() throws FedoraException {
        if (!isType(Cdr.DepositRecord.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Deposit Record.");
        }
        return this;
    }

    @Override
    public RepositoryObject getParent() {
        return driver.getParentObject(this);
    }

    /**
     * Returns the URI for the container which holds manifests for this record
     *
     * @return
     */
    public URI getManifestsUri() {
        return URI.create(pid.getRepositoryUri()
                + "/" + RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER);
    }

    private List<PID> addPidsToList(Property p) {
        Resource resource = getResource();
        StmtIterator containsIt = resource.listProperties(p);
        List<PID> pids = new ArrayList<>();
        while (containsIt.hasNext()) {
            String path = containsIt.next().getObject().toString();
            pids.add(PIDs.get(path));
        }
        return pids;
    }
}
