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

import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Creates objects in the repository matching specific object profile types.
 *
 * @author bbpennel, harring
 *
 */
public class RepositoryObjectFactory {

    private LdpContainerFactory ldpFactory;

    private FcrepoClient client;

    private RepositoryPaths repoPaths;

    private RepositoryObjectDataLoader repoObjDataLoader;

    private RepositoryObjectLoader repoObjLoader;

    private RepositoryPIDMinter pidMinter;

    /**
     * Creates a new deposit record object with the given uuid.
     * Properties in the supplied model will be added to the deposit record.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException {

        PID newPid = pidMinter.mintDepositRecordPid();

        DepositRecord depositRecord = new DepositRecord(newPid, repoObjLoader, repoObjDataLoader, this);
        return depositRecord;
    }

    /**
     * Creates a new AdminUnit with the given pid
     *
     * @param pid
     * @return
     * @throws FedoraException
     */
    public AdminUnit createAdminUnit(PID pid) throws FedoraException {
        return createAdminUnit(pid, null);
    }

    /**
     * Creates a new AdminUnit with the given pid and properties.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public AdminUnit createAdminUnit(PID pid, Model model) throws FedoraException {
        verifyContentPID(pid);

        PID createdPid = pidMinter.mintContentPid();

        return new AdminUnit(createdPid, repoObjLoader, repoObjDataLoader, this);
    }

    /**
     * Creates a new CollectionObject with the given pid
     *
     * @param pid
     * @return
     * @throws FedoraException
     */
    public CollectionObject createCollectionObject(PID pid) throws FedoraException {
        return createCollectionObject(pid, null);
    }

    /**
     * Creates a new CollectionObject with the given pid and properties.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public CollectionObject createCollectionObject(PID pid, Model model) throws FedoraException {
        verifyContentPID(pid);

        PID createdPid = pidMinter.mintContentPid();

        return new CollectionObject(createdPid, repoObjLoader, repoObjDataLoader, this);
    }

    /**
     * Creates a new FolderObject with the given pid
     *
     * @param pid
     * @return
     * @throws FedoraException
     */
    public FolderObject createFolderObject(PID pid) throws FedoraException {
        return createFolderObject(pid, null);
    }

    /**
     * Creates a new FolderObject with the given pid and properties.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public FolderObject createFolderObject(PID pid, Model model) throws FedoraException {
        verifyContentPID(pid);

        PID createdPid = pidMinter.mintContentPid();

        return new FolderObject(createdPid, repoObjLoader, repoObjDataLoader, this);
    }

    /**
     * Creates a new WorkObject with the given pid
     *
     * @param pid
     * @return
     * @throws FedoraException
     */
    public WorkObject createWorkObject(PID pid) throws FedoraException {
        return createWorkObject(pid, null);
    }

    /**
     * Creates a new WorkObject with the given pid and properties.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public WorkObject createWorkObject(PID pid, Model model) throws FedoraException {
        verifyContentPID(pid);

        PID createdPid = pidMinter.mintContentPid();

        return new WorkObject(createdPid, repoObjLoader, repoObjDataLoader, this);
    }

    /**
     * Creates a new file object with the given PID.
     *
     * @param pid
     * @return
     * @throws FedoraException
     */
    public FileObject createFileObject(PID pid) throws FedoraException {
        return createFileObject(pid, null);
    }

    /**
     * Creates a new file object with the given PID.
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public FileObject createFileObject(PID pid, Model model) throws FedoraException {
        verifyContentPID(pid);

        PID newPid = pidMinter.mintDepositRecordPid();

        return new FileObject(newPid, repoObjLoader, repoObjDataLoader, this);
    }
    /**
     * Creates a binary object at the given path.
     *
     * @param path
     *            Repository path where the binary will be created
     * @param slug
     *            Name in the path for the binary resource. Optional.
     * @param content
     *            Input stream containing the binary content for this resource.
     * @param filename
     *            Filename of the binary content. Optional.
     * @param mimetype
     *            Mimetype of the content. Optional.
     * @param checksum
     *            SHA-1 digest of the content. Optional.
     * @param model
     *            Model containing additional triples to add to the new binary's
     *            metadata. Optional
     * @return A BinaryObject for this newly created resource.
     * @throws FedoraException
     */
    public BinaryObject createBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String checksum, Model model) throws FedoraException {

        PID newPid = pidMinter.mintContentPid();

        BinaryObject binary = new BinaryObject(newPid, repoObjLoader, repoObjDataLoader, this);
        return binary;
    }

    /**
     * Add a member to the parent object.
     *
     * @param parent
     * @param member
     */
    public void addMember(ContentObject parent, ContentObject member) {
        createMemberLink(parent.getPid().getRepositoryUri(),
                member.getPid().getRepositoryUri());
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    public void createProperty(PID subject, Property property, String object) {
        String sparqlUpdate = RDFModelUtil.createSparqlInsert(subject.getRepositoryPath(), property, object);
        persistTripleToFedora(subject, sparqlUpdate);
    }

    /**
     * Creates a link between a parent object and a member object.
     *
     * @param parentUri
     * @param memberUri
     * @return
     * @throws FedoraException
     */
    public URI createMemberLink(URI parentUri, URI memberUri) throws FedoraException {
        String memberContainer = URIUtil.join(parentUri, RepositoryPathConstants.MEMBER_CONTAINER);

        return ldpFactory.createIndirectProxy(URI.create(memberContainer),
                parentUri, memberUri);
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    public void createRelationship(PID subject, Property property, Resource object) {
        String sparqlUpdate = RDFModelUtil.createSparqlInsert(subject.getRepositoryPath(), property, object);
        persistTripleToFedora(subject, sparqlUpdate);
    }

    /**
     * Creates the relevant triples in Fedora from the given model
     * @param pid
     * @param model
     */
    public void createRelationships(PID pid, Model model) {
        String sparqlUpdate = RDFModelUtil.createSparqlInsert(model);
        persistTripleToFedora(pid, sparqlUpdate);
    }

    /**
     * Creates a fedora object at the given location with the provided
     * properties
     *
     * @param uri
     * @param model
     * @return
     * @throws FedoraException
     */
    public URI createObject(URI uri, Model model) throws FedoraException {

        try (FcrepoResponse response = getClient().put(uri)
                .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                .perform()) {

            return response.getLocation();
        } catch (IOException e) {
            throw new FedoraException("Unable to create object at " + uri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }

    public FcrepoClient getClient() {
        return client;
    }

    public LdpContainerFactory getLdpFactory() {
        return ldpFactory;
    }

    public void setLdpFactory(LdpContainerFactory ldpFactory) {
        this.ldpFactory = ldpFactory;
    }

    /**
     * Throws a ObjectTypeMismatchException if the pid provided is not in the
     * content path
     *
     * @param pid
     */
    protected void verifyContentPID(PID pid) {
        if (!pid.getQualifier().equals(RepositoryPathConstants.CONTENT_BASE)) {
            throw new ObjectTypeMismatchException("Requested object " + pid + " is not a content object.");
        }
    }

    private void persistTripleToFedora(PID subject, String sparqlUpdate) {
        URI uri = repoPaths.getMetadataUri(subject);

        InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

        try (FcrepoResponse response = getClient().patch(uri)
                .body(sparqlStream)
                .perform()) {
        } catch (IOException e) {
            throw new FedoraException("Unable to add relationship to object " + subject.getId(), e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

}
