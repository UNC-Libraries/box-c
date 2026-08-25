package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.NTRIPLES_MIMETYPE;
import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.TombstoneFoundException;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import org.apache.http.HttpStatus;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.event.RepositoryPremisLog;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractRepositoryObject;

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

    private FcrepoClient client;

    /**
     * URI of the fcrepo Prefer header value indicating that inbound references to the requested
     * resource should be included in the response.
     */
    private static final URI PREFER_INBOUND_REFERENCES = URI.create(
            "http://fedora.info/definitions/fcrepo#PreferInboundReferences");
    private static final URI PREFER_SERVER_MANAGED = URI.create(
            "http://fedora.info/definitions/fcrepo#ServerManaged");

    protected PIDMinter pidMinter;

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

        ((AbstractRepositoryObject) obj).setTypes(types);

        return this;
    }

    /**
     * Loads and assigns the model for direct relationships of the given
     * repository object
     *
     * @param obj
     * @param checkForUpdates if true, will reload the model if the object has changed
     * @return
     * @throws FedoraException
     */
    public RepositoryObjectDriver loadModel(RepositoryObject obj, boolean checkForUpdates) throws FedoraException {
        long start = System.nanoTime();
        URI metadataUri = obj.getMetadataUri();
        // Model does not need to be loaded if it is already present AND if checkForUpdates is false, OR
        // checkForUpdates is true but the tx is no longer alive or the object is unmodified
        if (obj.hasModel() && !(checkForUpdates && (FedoraTransaction.isStillAlive() || !obj.isUnmodified()))) {
            log.debug("Object unchanged, reusing existing model for {}", obj.getPid());
            return this;
        }

        // Need to load the model from fedora
        try (FcrepoResponse response = getClient().get(metadataUri)
                .accept(TURTLE_MIMETYPE)
                .perform()) {

            Model model = ModelFactory.createDefaultModel();
            model.read(response.getBody(), null, Lang.TURTLE.getName());

            // Store the fresh model
            obj.storeModel(model);

            // Store updated modification info to track if the object changes
            obj.setEtag(parseEtag(response));
            log.debug("Retrieved new model for {} in {}s", obj.getPid(), (System.nanoTime() - start) / 1e9);

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
        if (repoObj instanceof Tombstone) {
            throw new TombstoneFoundException("Tombstone found, requested object " + pid + " no longer exists");
        }
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
     * Produces a list of PIDs for objects which are members of the provided object.
     *
     * @param obj the object
     * @return a List of PIDs for member objects of the provided object.
     */
    public List<PID> listMembers(RepositoryObject obj) {
        return listRelated(obj, PcdmModels.memberOf);
    }

    /**
     * Produces a list of PIDs for objects which are related to the current object via
     * the provided relationship property.
     *
     * @param obj the object
     * @param relation relation predicate
     * @return a List of PIDs for objects related by the given predicate
     */
    public List<PID> listRelated(RepositoryObject obj, Property relation) {
        PID pid = obj.getPid();
        URI metadataUri = obj.getMetadataUri();
        String selfUri = pid.getRepositoryUri().toString();

        // Ask fedora to include triples from other resources which reference this resource,
        // in addition to this resource's own properties.
        try (FcrepoResponse response = getClient().get(metadataUri)
                .accept(NTRIPLES_MIMETYPE)
                .preferRepresentation(List.of(PREFER_INBOUND_REFERENCES), List.of(PREFER_SERVER_MANAGED))
                .perform()) {
            return extractRelatedPids(response.getBody(), relation, selfUri);
        } catch (IOException e) {
            throw new FedoraException("Failed to list objects related to " + pid + " by " + relation, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Streams the n-triples response body, extracting the PIDs of subjects for any triples
     * which have the given predicate and whose object is the resource identified by selfUri.
     * The response is parsed as a stream rather than loaded into a Model, since it may include
     * a large number of inbound triples.
     */
    private List<PID> extractRelatedPids(InputStream bodyStream, Property relation, String selfUri) {
        List<PID> relatedPids = new ArrayList<>();
        String relationUri = relation.getURI();

        RDFParser.source(bodyStream)
                .lang(Lang.NTRIPLES)
                .parse(new StreamRDFBase() {
                    @Override
                    public void triple(Triple triple) {
                        if (!triple.getObject().isURI() || !triple.getSubject().isURI()) {
                            return;
                        }
                        if (relationUri.equals(triple.getPredicate().getURI())
                                && selfUri.equals(triple.getObject().getURI())) {
                            relatedPids.add(PIDs.get(triple.getSubject().getURI()));
                        }
                    }
                });

        return relatedPids;
    }

    /**
     * Retrieves parent object of the provided object
     * @param obj object to get the parent of.
     * @return RepositoryObject for the parent object of the provided object.
     * @throws OrphanedObjectException thrown if no parent object found for the object.
     * @throws ObjectTypeMismatchException thrown if object is not of a type eligible to have a parent.
     */
    public RepositoryObject getParentObject(RepositoryObject obj) {
        PID parentPid = getParentPid(obj);

        return repositoryObjectLoader.getRepositoryObject(parentPid);
    }

    /**
     * Retrieves the PID of the parent of the provided object
     * @param obj
     * @return
     */
    public PID getParentPid(RepositoryObject obj) {
        if (obj instanceof BinaryObject) {
            // Get the parent by removing the binary component from the path
            PID binPid = obj.getPid();
            String componentPath = binPid.getComponentPath();
            String binPath = obj.getPid().getRepositoryPath();
            return PIDs.get(binPath.substring(0, binPath.length() - componentPath.length() - 1));
        }
        if (obj instanceof ContentObject) {
            // For resources in the membership hierarchy, use reverse membership
            Statement memberOf = obj.getResource().getProperty(PcdmModels.memberOf);
            if (memberOf != null) {
                return PIDs.get(memberOf.getObject().toString());
            }
            throw new OrphanedObjectException("Cannot find a parent container for object " + obj.getPid());
        }
        throw new ObjectTypeMismatchException("Unable to get parent object for " + obj.getPid()
                + ", resources of type " + obj.getClass().getName() + " are not eligible.");
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

    public PremisLog getPremisLog(RepositoryObject repoObj) {
        return new RepositoryPremisLog(repoObj, repositoryObjectLoader);
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

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(PIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }
}
