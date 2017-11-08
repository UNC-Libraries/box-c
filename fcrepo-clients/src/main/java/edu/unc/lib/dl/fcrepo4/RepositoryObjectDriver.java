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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.RepositoryPremisLogger;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Service that provides data and clients to interact with an object's data
 * model.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectDriver {
    private static final Logger log = LoggerFactory.getLogger(RepositoryObjectDriver.class);

    private RepositoryObjectLoader repositoryObjectLoader;

    private RepositoryObjectFactory repositoryObjectFactory;

    private FcrepoClient client;

    private SparqlQueryService sparqlQueryService;

    protected RepositoryPIDMinter pidMinter;

    /**
     * Loads and assigns the RDF types for the given object
     *
     * @param obj
     * @return
     * @throws FedoraException
     */
    public RepositoryObjectDriver loadTypes(RepositoryObject obj) throws FedoraException {
        List<String> types = new ArrayList<>();
        // Iterate through all type properties and add to list
        Resource resc = obj.getModel().getResource(obj.getPid().getRepositoryUri().toString());
        StmtIterator it = resc.listProperties(RDF.type);
        while (it.hasNext()) {
            types.add(it.nextStatement().getResource().getURI());
        }

        obj.setTypes(types);

        return this;
    }

    /**
     * Loads and assigns the model for direct relationships of the given
     * repository object
     *
     * @param obj
     * @return
     * @throws FedoraException
     */
    public RepositoryObjectDriver loadModel(RepositoryObject obj) throws FedoraException {
        URI metadataUri = obj.getMetadataUri();
        // If the object is up to date and has already loaded the model then we're done
        if (obj.hasModel() && obj.isUnmodified()) {
            log.debug("Object unchanged, reusing existing model for {}", obj.getPid());
            return this;
        }

        // Need to load the model from fedora
        try (FcrepoResponse response = getClient().get(metadataUri)
                .accept(TURTLE_MIMETYPE)
                .perform()) {

            log.debug("Retrieving new model for {}", obj.getPid());
            Model model = ModelFactory.createDefaultModel();
            model.read(response.getBody(), null, Lang.TURTLE.getName());

            // Store the fresh model
            obj.storeModel(model);

            // Store updated modification info to track if the object changes
            obj.setEtag(parseEtag(response));

            return this;
        } catch (IOException e) {
            throw new FedoraException("Failed to read model for " + metadataUri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Retrieves a RepositoryObject identified by pid
     *
     * @param pid
     * @return
     */
    public RepositoryObject getRepositoryObject(PID pid) {
        return repositoryObjectLoader.getRepositoryObject(pid);
    }

    /**
     * Retrieves a RepositoryObject of the type provided
     *
     * @param pid
     * @param type class of the type of object to retrieve
     * @return
     * @throws ObjectTypeMismatchException thrown if the retrieved object does
     *             not match the requested type
     */
    public <T extends RepositoryObject> T getRepositoryObject(PID pid, Class<T> type)
            throws ObjectTypeMismatchException {
        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);
        if (!type.isInstance(repoObj)) {
            throw new ObjectTypeMismatchException("Requested object " + pid + " is not a " + type.getName());
        }

        return type.cast(repoObj);
    }

    /**
     * Retrieve the binary content for the given BinaryObject as an inputstream
     *
     * @param obj
     * @return
     * @throws FedoraException
     */
    public InputStream getBinaryStream(BinaryObject obj) throws FedoraException {
        PID pid = obj.getPid();

        try {
            FcrepoResponse response = getClient().get(pid.getRepositoryUri()).perform();
            return response.getBody();
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Retrieves parent container of the current object
     * @param child
     * @param membershipRelation
     * @return
     */
    public PID fetchContainer(PID child, Resource membershipRelation) {
        Resource relationship = (membershipRelation != null) ? membershipRelation : PcdmModels.hasMember;

        String queryString = String.format("select ?pid where { ?pid <%1$s> <%2$s> }", relationship, child.getURI());

        try (QueryExecution qexec = sparqlQueryService.executeQuery(queryString)) {
            ResultSet results = qexec.execSelect();

            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                Resource res = soln.getResource("pid");

                if (res != null) {
                    return PIDs.get(res.getURI());
                }
            }
        }

        return null;
    }

    /**
     * Retrieves parent container of the current object
     * @param child
     * @return
     */
    public PID fetchContainer(PID child) {
        return fetchContainer(child, null);
    }

    /**
     * Retrieves parent object of the current object
     * @param obj
     * @return
     */
    public RepositoryObject getParentObject(RepositoryObject obj) {
        Resource resourceType = null;
        final Model model = obj.getModel();

        if (model.containsResource(Fcrepo4Repository.Binary)) {
            resourceType = PcdmModels.hasFile;
        } else if (model.containsResource(Fcrepo4Repository.Container)) {
            resourceType = PcdmModels.hasMember;
        }

        PID pid = fetchContainer(obj.getPid(), resourceType);

        return repositoryObjectLoader.getRepositoryObject(pid);
    }

    /**
     * Retrieves the etag for the provided object
     *
     * @param obj
     * @return
     */
    public String getEtag(RepositoryObject obj) {
        try (FcrepoResponse response = getClient().head(obj.getMetadataUri()).perform()) {
            if (response.getStatusCode() != HttpStatus.SC_OK) {
                throw new FedoraException("Received " + response.getStatusCode()
                        + " response while retrieving headers for " + obj.getPid().getRepositoryUri());
            }

            return parseEtag(response);
        } catch (IOException e) {
            throw new FedoraException("Unable to create deposit record at "
                    + obj.getPid().getRepositoryUri(), e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Retrieve the ETag of the response, with surrounding quotes stripped.
     *
     * @param response
     * @return
     */
    private static String parseEtag(FcrepoResponse response) {
        String etag = response.getHeaderValue("ETag");
        if (etag != null) {
            return etag.substring(1, etag.length() - 1);
        }
        return null;
    }

    public PremisLogger getPremisLog(RepositoryObject repoObj) {
        return new RepositoryPremisLogger(
                repoObj, pidMinter, repositoryObjectLoader, repositoryObjectFactory);
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }

    public FcrepoClient getClient() {
        return client;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repositoryObjectLoader = repoObjLoader;
    }

    public SparqlQueryService getSparqlQueryService() {
        return sparqlQueryService;
    }

    public void setSparqlQueryService(SparqlQueryService SparqlQueryService) {
        this.sparqlQueryService = SparqlQueryService;
    }

    /**
     * @param repositoryObjectFactory the repositoryObjectFactory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }
}
