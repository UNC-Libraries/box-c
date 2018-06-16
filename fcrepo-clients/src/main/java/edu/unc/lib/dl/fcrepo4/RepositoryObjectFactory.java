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

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getMetadataUri;
import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.ChecksumMismatchException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.sparql.SparqlUpdateHelper;
import edu.unc.lib.dl.sparql.SparqlUpdateService;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Creates objects in the repository matching specific object profile types.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectFactory {

    private LdpContainerFactory ldpFactory;

    private FcrepoClient client;

    private RepositoryObjectDriver repoObjDriver;

    private RepositoryPIDMinter pidMinter;

    private SparqlUpdateService sparqlUpdateService;

    /**
     * Creates a new deposit record object with the given model.
     * Properties in the supplied model will be added to the deposit record.
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public DepositRecord createDepositRecord(Model model) throws FedoraException {
        PID pid = pidMinter.mintDepositRecordPid();
        return createDepositRecord(pid, model);
    }

    public DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add the deposit record type to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.DepositRecord));

        try (FcrepoResponse response = getClient().put(path)
                .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                .perform()) {
            URI createdUri = response.getLocation();
            // Add the manifests container
            ldpFactory.createDirectContainer(createdUri, Cdr.hasManifest,
                RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER);

            // Add the premis event container
            addEventContainer(createdUri);

        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        DepositRecord depositRecord = new DepositRecord(pid, repoObjDriver, this);
        return depositRecord;
    }

    /**
     * Creates a new AdminUnit with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public AdminUnit createAdminUnit(Model model) throws FedoraException {
        PID pid = pidMinter.mintContentPid();

        return createAdminUnit(pid, model);
    }

    /**
     * Creates a new AdminUnit with the given model and provided pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public AdminUnit createAdminUnit(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.AdminUnit, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return new AdminUnit(pid, repoObjDriver, this);
    }

    /**
     * Creates a content root object structure with the provided properties
     *
     * @param path
     * @param model
     * @return
     * @throws FedoraException
     */
    public URI createContentRootObject(URI path, Model model) throws FedoraException {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.ContentRoot));

        return createContentContainerObject(path, model);
    }


    /**
     * Creates a new CollectionObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public CollectionObject createCollectionObject(Model model) throws FedoraException {
        PID pid = pidMinter.mintContentPid();

        return createCollectionObject(pid, model);
    }

    /**
     * Creates a new CollectionObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public CollectionObject createCollectionObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Collection, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return new CollectionObject(pid, repoObjDriver, this);
    }

    /**
     * Creates a new FolderObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public FolderObject createFolderObject(Model model) throws FedoraException {
        PID pid = pidMinter.mintContentPid();

        return createFolderObject(pid, model);
    }

    /**
     * Creates a new FolderObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public FolderObject createFolderObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Folder, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return new FolderObject(pid, repoObjDriver, this);
    }

    /**
     * Creates a new WorkObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public WorkObject createWorkObject(Model model) throws FedoraException {
        PID pid = pidMinter.mintContentPid();

        return createWorkObject(pid, model);
    }

    /**
     * Creates a new WorkObject with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public WorkObject createWorkObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Work, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return new WorkObject(pid, repoObjDriver, this);
    }

    /**
     * Creates a new file object with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    public FileObject createFileObject(Model model) throws FedoraException {
        PID pid = pidMinter.mintContentPid();

        return createFileObject(pid, model);
    }

    /**
     * Creates a new file object with the given model and pid
     *
     * @param pid
     * @param model
     * @return
     * @throws FedoraException
     */
    public FileObject createFileObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.FileObject, PcdmModels.Object));

        try (FcrepoResponse response = getClient().put(path)
                .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                .perform()) {
            URI createdUri = response.getLocation();
            // Add PREMIS event container
            addEventContainer(createdUri);

            // Add the manifests container
            ldpFactory.createDirectFileSet(createdUri, RepositoryPathConstants.DATA_FILE_FILESET);

        } catch (IOException e) {
            throw new FedoraException("Unable to create file object at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        return new FileObject(pid, repoObjDriver, this);
    }

    /**
    * Creates a binary resource at the given path.
    *
    * @param path
    *        Repository path where the binary will be created
    * @param slug
    *        Name in the path for the binary resource. Optional.
    * @param content
    *        Input stream containing the binary content for this resource.
    * @param filename
    *        Filename of the binary content. Optional.
    * @param mimetype
    *        Mimetype of the content. Optional.
    * @param checksum
    *        SHA-1 digest of the content. Optional.
    * @param model
    *        Model containing additional triples to add to the new binary's metadata. Optional
    * @return URI of the newly created binary
    * @throws FedoraException
    */
    public BinaryObject createBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model) throws FedoraException {
        if (content == null) {
            throw new IllegalArgumentException("Cannot create a binary object from a null content stream");
        }
        // Upload the binary and provided technical metadata
        URI resultUri;
        // Track the URI where metadata updates would be made to for this binary
        URI describedBy;
        try (FcrepoResponse response = getClient().post(path).slug(slug).body(content, mimetype).filename(filename)
                .digestSha1(sha1Checksum).digestMd5(md5Checksum).perform()) {
            resultUri = response.getLocation();
            describedBy = response.getLinkHeaders("describedby").get(0);
        } catch (IOException e) {
            throw new FedoraException("Unable to create binary at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            // if one or more checksums don't match
            if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                throw new ChecksumMismatchException("Failed to create binary for " + path + ", since checksum(s)"
                        + " did not match the submitted content according to the repository.", e);
            }
            throw ClientFaultResolver.resolve(e);
        }
        if (model != null) {
            // Add in pcdm:File type to model
            model = populateModelTypes(resultUri, model, Arrays.asList(PcdmModels.File));

            // If a model was provided, then add the triples to the new binary's
            // metadata
            // Turn model into sparql update query
            String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(model);
            InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

            try (FcrepoResponse response = getClient().patch(describedBy).body(sparqlStream).perform()) {
            } catch (IOException e) {
                throw new FedoraException("Unable to add triples to binary at " + path, e);
            } catch (FcrepoOperationFailedException e) {
                throw ClientFaultResolver.resolve(e);
            }
        }
        return new BinaryObject(PIDs.get(resultUri), repoObjDriver, this);
    }

    /**
     * Updates a binary resource at the given path.
     *
     * @param path
     *        Repository path for the binary that will be updated
     * @param slug
     *        Name in the path for the binary resource.
     * @param content
     *        Input stream containing the binary content for this resource.
     * @param filename
     *        Filename of the binary content. Optional.
     * @param mimetype
     *        Mimetype of the content. Optional.
     * @param checksum
     *        SHA-1 digest of the content. Optional.
     * @param model
     *        Model containing additional triples to add to the binary's metadata. Optional
     * @return URI of the updated binary
     * @throws FedoraException
     */
     public BinaryObject updateBinary(URI path, String slug, InputStream content, String filename, String mimetype,
             String sha1Checksum, String md5Checksum, Model model) throws FedoraException {
         if (content == null) {
             throw new IllegalArgumentException("Cannot update a binary object from a null content stream");
         }
         // Track the URI where metadata updates would be made for this binary
         URI describedBy;
         if (path == null || slug == null) {
             throw new IllegalArgumentException("Path and slug for binary must both not be null");
         }
         URI updatePath = URI.create(URIUtil.join(path, slug));

         try (FcrepoResponse response = getClient().put(updatePath).body(content, mimetype).filename(filename)
                 .digestSha1(sha1Checksum).digestMd5(md5Checksum).perform()) {
             describedBy = response.getLinkHeaders("describedby").get(0);
         } catch (IOException e) {
             throw new FedoraException("Unable to update binary at " + updatePath, e);
         } catch (FcrepoOperationFailedException e) {
             // if one or more checksums don't match
             if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                 throw new ChecksumMismatchException("Failed to update binary for " + updatePath + ", since checksum(s)"
                         + " did not match the submitted content according to the repository.", e);
             }
             throw ClientFaultResolver.resolve(e);
         }
         if (model != null) {
             // Add in pcdm:File type to model
             model = populateModelTypes(updatePath, model, Arrays.asList(PcdmModels.File));

             // If a model was provided, then add the triples to the binary's metadata
             // Turn model into sparql update query
             String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(model);
             InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

             try (FcrepoResponse response = getClient().patch(describedBy).body(sparqlStream).perform()) {
             } catch (IOException e) {
                 throw new FedoraException("Unable to add triples to binary at " + updatePath, e);
             } catch (FcrepoOperationFailedException e) {
                 throw ClientFaultResolver.resolve(e);
             }
         }
         return new BinaryObject(PIDs.get(updatePath), repoObjDriver, this);
     }

    /**
     * Creates an event for the specified object.
     *
     * @param eventPid
     *            the PID of the event to add
     * @param model
     *            Model containing properties of this event. Must only contain
     *            the properties for one event.
     * @return URI of the event created
     * @throws FedoraException
     */
    public PremisEventObject createPremisEvent(PID eventPid, Model model) throws FedoraException {

        URI createdUri = createOrTransformObject(eventPid.getRepositoryUri(), model);

        return new PremisEventObject(PIDs.get(createdUri), repoObjDriver, this);
    }

    public PremisEventObject getPremisEvent(PID pid) throws FedoraException {
        return new PremisEventObject(pid, repoObjDriver, this).validateType();
    }

    /**
     * Add a member to the parent object.
     *
     * @param parent
     * @param member
     */
    public void addMember(ContentObject parent, ContentObject member) {
        createMemberLink(parent.getPid().getRepositoryUri(), member.getPid().getRepositoryUri());
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    public void createProperty(PID subject, Property property, String object) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(subject.getRepositoryPath(), property, object);
        persistTripleToFedora(subject, sparqlUpdate);
    }

    /**
     * Creates a link between a parent object and a member object.
     *
     * @param parentUri
     * @param memberUri
     * @throws FedoraException
     */
    public void createMemberLink(URI parentUri, URI memberUri) throws FedoraException {
        String memberContainer = URIUtil.join(parentUri, RepositoryPathConstants.MEMBER_CONTAINER);
        ldpFactory.createIndirectProxy(URI.create(memberContainer), parentUri, memberUri);
    }

    /**
     * Creates a triple in Fedora by replacing the current property with the given property parameter
     * @param subject
     * @param property the new property value
     * @param object
     */
    public void createExclusiveRelationship(RepositoryObject repoObj, Property property, Object object) {
        NodeIterator valuesIt = repoObj.getModel().listObjectsOfProperty(property);
        List<Object> previousValues = null;
        if (valuesIt != null && valuesIt.hasNext()) {
            previousValues = new ArrayList<>();
            while (valuesIt.hasNext()) {
                previousValues.add(valuesIt.next());
            }
        }
        PID subject = repoObj.getPid();
        String sparqlUpdate = SparqlUpdateHelper.createSparqlReplace(subject.getRepositoryPath(), property, object,
                previousValues);
        persistTripleToFedora(subject, sparqlUpdate);
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    public void createRelationship(PID subject, Property property, Resource object) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(subject.getRepositoryPath(), property, object);
        persistTripleToFedora(subject, sparqlUpdate);
    }

    /**
     * Creates the relevant triples in Fedora from the given model
     * @param pid
     * @param model
     */
    public void createRelationships(PID pid, Model model) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(model);
        persistTripleToFedora(pid, sparqlUpdate);
    }

    /**
     * Creates a fedora object at the given location with the provided
     * properties, or replaces an existing object's triples with those in
     * the provided model
     *
     * @param uri
     * @param model
     * @return
     * @throws FedoraException
     */
    public URI createOrTransformObject(URI uri, Model model) throws FedoraException {

        InputStream modelStream = null;
        if (model != null) {
            try {
                Model newModel = ModelFactory.createDefaultModel();
                newModel.add(model.listStatements(new SanitizeServerManagedTriplesSelector()));
                modelStream = RDFModelUtil.streamModel(newModel);
            } catch (IOException e) {
                throw new FedoraException("Unable to create object at " + uri, e);
            }
        }

        try (FcrepoResponse response = getClient().put(uri)
                .body(modelStream, TURTLE_MIMETYPE)
                .preferLenient()
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

    public void setRepositoryObjectDriver(RepositoryObjectDriver repoObjDriver) {
        this.repoObjDriver = repoObjDriver;
    }

    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    /**
     * @param sparqlUpdateService the sparqlUpdateService to set
     */
    public void setSparqlUpdateService(SparqlUpdateService sparqlUpdateService) {
        this.sparqlUpdateService = sparqlUpdateService;
    }

    private void persistTripleToFedora(PID subject, String sparqlUpdate) {
        URI uri = getMetadataUri(subject);

        sparqlUpdateService.executeUpdate(uri.toString(), sparqlUpdate);
    }

    private URI createContentContainerObject(URI path, Model model) throws FedoraException {
        try (FcrepoResponse response = getClient().put(path).body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                .perform()) {

            URI createdUri = response.getLocation();

            // Add PREMIS event container
            addEventContainer(createdUri);

            // Add the container for member objects
            ldpFactory.createIndirectContainer(createdUri, PcdmModels.hasMember,
                    RepositoryPathConstants.MEMBER_CONTAINER);

            return createdUri;

        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    private void addEventContainer(URI parentUri) throws FedoraException, IOException {
        ldpFactory.createDirectContainer(parentUri, Premis.hasEvent, RepositoryPathConstants.EVENTS_CONTAINER);
    }

    private Model populateModelTypes(URI rescUri, Model model, List<Resource> types) {
        // Create an empty model if none was provided
        if (model == null) {
            model = ModelFactory.createDefaultModel();
        }

        // Add the required type for DepositRecords
        Resource mainResc = model.getResource(rescUri.toString());
        for (Resource type : types) {
            mainResc.addProperty(RDF.type, type);
        }

        return model;
    }

}
