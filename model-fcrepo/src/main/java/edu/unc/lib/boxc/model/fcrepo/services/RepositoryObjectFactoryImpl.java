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
package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.FCR_METADATA;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.METADATA_CONTAINER;
import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;
import static edu.unc.lib.dl.util.MimetypeHelpers.formatMimetype;
import static org.fcrepo.client.ExternalContentHandling.PROXY;
import static org.fcrepo.client.FedoraTypes.LDP_NON_RDF_SOURCE;
import static org.fcrepo.client.LinkHeaderConstants.DESCRIBEDBY_REL;
import static org.slf4j.LoggerFactory.getLogger;

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
import org.slf4j.Logger;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.fcrepo4.ClientFaultResolver;
import edu.unc.lib.dl.fcrepo4.SanitizeServerManagedTriplesSelector;
import edu.unc.lib.dl.fedora.ChecksumMismatchException;
import edu.unc.lib.dl.sparql.SparqlUpdateHelper;
import edu.unc.lib.dl.sparql.SparqlUpdateService;

/**
 * Creates objects in the repository matching specific object profile types.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectFactoryImpl implements RepositoryObjectFactory {
    private static final Logger log = getLogger(RepositoryObjectFactoryImpl.class);

    private LdpContainerFactory ldpFactory;

    private FcrepoClient client;

    private RepositoryObjectLoader repoObjLoader;

    private PIDMinter pidMinter;

    private SparqlUpdateService sparqlUpdateService;

    /**
     * Creates a new deposit record object with the given model.
     * Properties in the supplied model will be added to the deposit record.
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
    public DepositRecord createDepositRecord(Model model) throws FedoraException {
        PID pid = pidMinter.mintDepositRecordPid();
        return createDepositRecord(pid, model);
    }

    @Override
    public DepositRecord createDepositRecord(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        log.debug("Creating deposit record {}", pid.getId());
        // Add the deposit record type to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.DepositRecord));

        log.debug("Streaming model and requesting creation of {}", pid.getId());
        try {
            URI createdUri;
            try (FcrepoResponse response = getClient().put(path)
                    .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                    .perform()) {
                createdUri = response.getLocation();
            }
            // Add the manifests container
            log.debug("Created with location {}, adding manifest container", createdUri);
            ldpFactory.createDirectContainer(createdUri, Cdr.hasManifest,
                RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER);

            log.debug("Adding metadata container to {}", createdUri);
            // Add container for metadata objects
            addMetadataContainer(createdUri);

        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        log.debug("Retrieving created deposit record object {}", pid.getId());
        return repoObjLoader.getDepositRecord(pid);
    }

    /**
     * Creates a new AdminUnit with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
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
    @Override
    public AdminUnit createAdminUnit(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.AdminUnit, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return repoObjLoader.getAdminUnit(pid);
    }

    /**
     * Creates a content root object structure with the provided properties
     *
     * @param path
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
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
    @Override
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
    @Override
    public CollectionObject createCollectionObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Collection, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return repoObjLoader.getCollectionObject(pid);
    }

    /**
     * Creates a new FolderObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
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
    @Override
    public FolderObject createFolderObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Folder, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return repoObjLoader.getFolderObject(pid);
    }

    /**
     * Creates a new WorkObject with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
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
    @Override
    public WorkObject createWorkObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.Work, PcdmModels.Object));

        createContentContainerObject(pid.getRepositoryUri(), model);

        return repoObjLoader.getWorkObject(pid);
    }

    /**
     * Creates a new file object with the given model and a generated pid
     *
     * @param model
     * @return
     * @throws FedoraException
     */
    @Override
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
    @Override
    public FileObject createFileObject(PID pid, Model model) throws FedoraException {
        URI path = pid.getRepositoryUri();

        // Add types to the object being created
        model = populateModelTypes(path, model, Arrays.asList(Cdr.FileObject, PcdmModels.Object));

        try {
            URI createdUri;
            try (FcrepoResponse response = getClient().put(path)
                    .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                    .perform()) {
                createdUri = response.getLocation();
            }

            // Add container for metadata objects
            addMetadataContainer(createdUri);

            // Add the manifests container
            ldpFactory.createDirectFileSet(createdUri, RepositoryPathConstants.DATA_FILE_FILESET);

        } catch (IOException e) {
            throw new FedoraException("Unable to create file object at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        return repoObjLoader.getFileObject(pid);
    }

    /**
     * Creates a BinaryObject with the given PID, using the provided storage URI as the proxied external binary
     * location.
     *
     * @param pid pid of the binary
     * @param storageUri location where the binary is stored
     * @param filename filename for the binary
     * @param mimetype mimetype of the binary
     * @param sha1Checksum sha1 digest of the content.
     * @param md5Checksum md5 digest of the content.
     * @param model Model containing any properties to include in the description of this binary
     * @return the newly created BinaryObject
     * @throws FedoraException
     */
    @Override
    public BinaryObject createOrUpdateBinary(PID pid, URI storageUri, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model) {
        // Upload the binary and provided metadata
        URI resultUri;
        // Track the URI where metadata updates would be made to for this binary
        URI describedBy;
        log.debug("Updating binary {} with fields:\n    uri = '{}'\n    mimetype = '{}', " +
                "filename = '{}'\n    sha1 = '{}', m5 = '{}'", pid.getRepositoryPath(),
                storageUri, mimetype, filename, sha1Checksum, md5Checksum);
        try (FcrepoResponse response = getClient().put(pid.getRepositoryUri())
                .externalContent(storageUri, formatMimetype(mimetype), PROXY)
                .addInteractionModel(LDP_NON_RDF_SOURCE)
                .filename(filename)
                .digestSha1(sha1Checksum)
                .digestMd5(md5Checksum)
                .perform()) {
            resultUri = response.getLocation();
            describedBy = response.getLinkHeaders(DESCRIBEDBY_REL).get(0);
        } catch (IOException e) {
            throw new FedoraException("Unable to create binary at " + pid.getRepositoryPath(), e);
        } catch (FcrepoOperationFailedException e) {
            // if one or more checksums don't match
            if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                throw new ChecksumMismatchException(String.format("Failed to create binary for %s"
                    + " from URI '%s', checksum(s) did not match the submitted"
                    + " content according to the repository: md5=%s sha1=%s",
                    pid.getRepositoryPath(), storageUri, md5Checksum, sha1Checksum));
            }
            throw ClientFaultResolver.resolve(e);
        }

        if (model != null) {
            updateBinaryDescription(pid, describedBy, model);
        }

        String resultUriString = resultUri.toString();
        if (resultUriString.endsWith(FCR_METADATA)) {
            resultUriString = resultUriString.replace("/" + FCR_METADATA, "");
        }

        PID binPid = PIDs.get(resultUriString);
        repoObjLoader.invalidate(binPid);
        return repoObjLoader.getBinaryObject(binPid);
    }

    private void updateBinaryDescription(PID binPid, URI describedBy, Model model) {
        // Add in pcdm:File type to model
        populateModelTypes(binPid.getRepositoryUri(), model, Arrays.asList(PcdmModels.File));

        // If a model was provided, then add the triples to the new binary's
        // metadata
        // Turn model into sparql update query
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(model);
        InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

        try (FcrepoResponse response = getClient().patch(describedBy).body(sparqlStream).perform()) {
        } catch (IOException e) {
            throw new FedoraException("Unable to add triples to binary at " + describedBy, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
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
    @Override
    public BinaryObject createBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String sha1Checksum, String md5Checksum, Model model) throws FedoraException {
        if (content == null) {
            throw new IllegalArgumentException("Cannot create a binary object from a null content stream");
        }
        // Upload the binary and provided technical metadata
        URI resultUri;
        // Track the URI where metadata updates would be made to for this binary
        URI describedBy;
        try (FcrepoResponse response = getClient().post(path).slug(slug)
                .body(content, formatMimetype(mimetype))
                .addInteractionModel(LDP_NON_RDF_SOURCE)
                .filename(filename)
                .digestSha1(sha1Checksum)
                .digestMd5(md5Checksum)
                .perform()) {
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
        return repoObjLoader.getBinaryObject(PIDs.get(resultUri));
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
     * @param sha1Checksum
     *        SHA-1 digest of the content. Optional.
     * @param md5Checksum
     *        SHA-1 digest of the content. Optional.
     * @param model
     *        Model containing additional triples to add to the binary's metadata. Optional
     * @return URI of the updated binary
     * @throws FedoraException
     */
     @Override
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

         try (FcrepoResponse response = getClient().put(updatePath)
                 .body(content, formatMimetype(mimetype))
                 .addInteractionModel(LDP_NON_RDF_SOURCE)
                 .filename(filename)
                 .digestSha1(sha1Checksum)
                 .digestMd5(md5Checksum)
                 .perform()) {
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
         PID binPid = PIDs.get(updatePath);
         repoObjLoader.invalidate(binPid);
         return repoObjLoader.getBinaryObject(binPid);
     }

    /**
     * Add a member to the parent object.
     *
     * @param parent
     * @param member
     */
    @Override
    public void addMember(ContentObject parent, ContentObject member) {
        createExclusiveRelationship(member, PcdmModels.memberOf, parent.getResource());
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    @Override
    public void createProperty(RepositoryObject subject, Property property, String object) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(subject.getPid().getRepositoryPath(),
                property, object);
        persistTripleToFedora(subject.getMetadataUri(), sparqlUpdate);
        subject.shouldRefresh();
    }

    /**
     * Creates a triple in Fedora by replacing the current property with the given property parameter
     * @param repoObj repository object to update the properties of.
     * @param property the property to update
     * @param object all of the new values for the property
     */
    @Override
    public void createExclusiveRelationship(RepositoryObject repoObj, Property property, Object object) {
        NodeIterator valuesIt = repoObj.getModel(true).listObjectsOfProperty(property);
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
        persistTripleToFedora(repoObj.getMetadataUri(), sparqlUpdate);
        repoObj.shouldRefresh();
    }

    /**
     * Deletes all triples with the given property predicate from the specified repository object.
     * @param repoObj repository object to remove properties from
     * @param property predicate of properties to remove
     */
    @Override
    public void deleteProperty(RepositoryObject repoObj, Property property) {
        PID subject = repoObj.getPid();
        String sparqlUpdate = SparqlUpdateHelper.createSparqlDelete(
                subject.getRepositoryPath(), property, null);
        sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), sparqlUpdate);
        repoObj.shouldRefresh();
    }

    /**
     * Creates a triple in Fedora from the given parameters
     * @param subject
     * @param property
     * @param object
     */
    @Override
    public void createRelationship(RepositoryObject subject, Property property, Resource object) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(subject.getPid().getRepositoryPath(),
                property, object);
        persistTripleToFedora(subject.getMetadataUri(), sparqlUpdate);
        subject.shouldRefresh();
    }

    /**
     * Creates the relevant triples in Fedora from the given model
     * @param subject
     * @param model
     */
    @Override
    public void createRelationships(RepositoryObject subject, Model model) {
        String sparqlUpdate = SparqlUpdateHelper.createSparqlInsert(model);
        persistTripleToFedora(subject.getMetadataUri(), sparqlUpdate);
        subject.shouldRefresh();
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
    @Override
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

    @Override
    public boolean objectExists(URI uri) {
        try (FcrepoResponse response = getClient().head(uri).perform()) {
            return true;
        } catch (IOException e) {
            throw new FedoraException("Failed to close HEAD response for " + uri, e);
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            throw new FedoraException("Failed to check on object " + uri, e);
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

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setPidMinter(PIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    /**
     * @param sparqlUpdateService the sparqlUpdateService to set
     */
    public void setSparqlUpdateService(SparqlUpdateService sparqlUpdateService) {
        this.sparqlUpdateService = sparqlUpdateService;
    }

    private void persistTripleToFedora(URI subject, String sparqlUpdate) {
        sparqlUpdateService.executeUpdate(subject.toString(), sparqlUpdate);
    }

    private URI createContentContainerObject(URI path, Model model) throws FedoraException {
        try {
            URI createdUri;
            try (FcrepoResponse response = getClient().put(path)
                    .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                    .perform()) {
                createdUri = response.getLocation();
            }

            // Add container for metadata objects
            addMetadataContainer(createdUri);

            return createdUri;

        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    private void addMetadataContainer(URI parentUri) throws FedoraException, IOException {
        ldpFactory.createDirectContainer(parentUri, IanaRelation.describedby, METADATA_CONTAINER);
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
