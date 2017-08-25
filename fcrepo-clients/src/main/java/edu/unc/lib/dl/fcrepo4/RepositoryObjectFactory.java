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
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.ChecksumMismatchException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Creates objects in the repository matching specific object profile types.
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectFactory {

    private LdpContainerFactory ldpFactory;

    private FcrepoClient client;

    /**
     * Creates a deposit record object structure at the given path with the
     * properties specified in the provided model
     *
     * @param path
     *            URI of the full path where this deposit record should be
     *            created
     * @param model
     *            Model containing additional properties to add to this Deposit
     *            Record. Optional.
     * @return the URI of the created deposit record
     * @throws FedoraException
     */
    public URI createDepositRecord(URI path, Model model) throws FedoraException {
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

            return createdUri;
        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Creates an AdminUnit object structure at the given path with optional
     * properties.
     *
     * @param path
     *            URI of the full path where the work will be created
     * @param model
     *            Model containing additional properties. Optional.
     * @return URI to the object created
     * @throws FedoraException
     */
    public URI createAdminUnit(URI path, Model model) {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.AdminUnit, PcdmModels.Collection));

        return createContentContainerObject(path, model);
    }

    /**
     * Creates a collection object structure at the given path with optional
     * properties.
     *
     * @param path
     *            URI of the full path where the work will be created
     * @param model
     *            Model containing additional properties. Optional.
     * @return URI to the object created
     * @throws FedoraException
     */
    public URI createCollectionObject(URI path, Model model) {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.Collection, PcdmModels.Object));

        return createContentContainerObject(path, model);
    }

    /**
     * Creates a folder object structure at the given path with optional
     * properties.
     *
     * @param path
     *            URI of the full path where the work will be created
     * @param model
     *            Model containing additional properties. Optional.
     * @return URI to the object created
     * @throws FedoraException
     */
    public URI createFolderObject(URI path, Model model) throws FedoraException {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.Folder, PcdmModels.Object));

        return createContentContainerObject(path, model);
    }

    /**
     * Creates a work object structure at the given path with the properties
     * specified.
     *
     * @param path
     *            URI of the full path where the work will be created
     * @param model
     *            Model containing additional properties. Optional.
     * @return
     * @throws FedoraException
     */
    public URI createWorkObject(URI path, Model model) throws FedoraException {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.Work, PcdmModels.Object));

        return createContentContainerObject(path, model);
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
     * Helper to create a content object that can contain members and events
     *
     * @param path
     * @param model
     * @return
     * @throws FedoraException
     */
    private URI createContentContainerObject(URI path, Model model) throws FedoraException {
        try (FcrepoResponse response = getClient().put(path)
                .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
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

    /**
     * Creates a minimal file object structure at the given path with the
     * properties specified in the provided model
     *
     * @param path
     * @param model
     * @return
     * @throws FedoraException
     */
    public URI createFileObject(URI path, Model model) throws FedoraException {
        // Add types to the object being created
        model = populateModelTypes(path, model,
                Arrays.asList(Cdr.FileObject, PcdmModels.Object));

        try (FcrepoResponse response = getClient().put(path)
                .body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
                .perform()) {

            URI createdUri = response.getLocation();

            // Add PREMIS event container
            addEventContainer(createdUri);

            // Add the manifests container
            ldpFactory.createDirectFileSet(createdUri,
                    RepositoryPathConstants.DATA_FILE_FILESET);

            return createdUri;
        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Creates a binary resource at the given path.
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
     * @return URI of the newly created binary
     * @throws FedoraException
     */
    public URI createBinary(URI path, String slug, InputStream content, String filename, String mimetype,
            String checksum, Model model) throws FedoraException {
        if (content == null) {
            throw new IllegalArgumentException("Cannot create a binary object from a null content stream");
        }

        // Upload the binary and provided technical metadata
        URI resultUri;
        // Track the URI where metadata updates would be made to for this binary
        URI describedBy;
        try (FcrepoResponse response = getClient().post(path)
                .slug(slug)
                .body(content, mimetype)
                .filename(filename)
                .digest(checksum)
                .perform()) {

            resultUri = response.getLocation();
            describedBy = response.getLinkHeaders("describedby").get(0);
        } catch (IOException e) {
            throw new FedoraException("Unable to create binary at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                throw new ChecksumMismatchException("Failed to create binary for " + path + ", provided SHA1 checksum "
                        + checksum + " did not match the submitted content according to the repository.", e);
            }
            throw ClientFaultResolver.resolve(e);
        }

        // Add in pcdm:File type to model
        model = populateModelTypes(resultUri, model, Arrays.asList(PcdmModels.File));

        // If a model was provided, then add the triples to the new binary's metadata
        // Turn model into sparql update query
        String sparqlUpdate = RDFModelUtil.createSparqlInsert(model);
        InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

        try (FcrepoResponse response = getClient().patch(describedBy)
                .body(sparqlStream)
                .perform()) {
        } catch (IOException e) {
            throw new FedoraException("Unable to add triples to binary at " + path, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        return resultUri;
    }

    /**
     * Adds a set of resource types to the specified resource in the given
     * model. If no model is provided, then a new model is created.
     *
     * @param model
     *            Model to add to. If no model is provided, then one is created.
     * @param rescUri
     *            URI of the resource types will be added to.
     * @param types
     *            list of types to add
     * @return The model with types added.
     */
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

    private URI addEventContainer(URI parentUri) throws FedoraException, IOException {
        return ldpFactory.createDirectContainer(parentUri, Premis.hasEvent,
                RepositoryPathConstants.EVENTS_CONTAINER);
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

}
