package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Service which returns version information for file objects stored in Fedora
 */
public class FileVersionService {
    private FcrepoClient fcrepoClient;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repositoryObjectLoader;


    public Model getVersionInfo(PID pid) {
        var uriString = pid.getRepositoryUri().toString() + "/fcr:versions";
        var objUri = URI.create(uriString);
        try (FcrepoResponse resp = fcrepoClient.get(objUri).perform()) {
            Model childModel = ModelFactory.createDefaultModel();
            return childModel.read(resp.getBody(), null);
        } catch (IOException e) {
            throw new FedoraException("Failed to list versions for " + objUri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    private Map<PID, String> getMetadataFromResponse(Model model) {
        var map = new HashMap<>();
        var versions = model.listObjectsOfProperty(Ldp.contains);
        for (NodeIterator it = versions; it.hasNext(); ) {
            var version = it.next();
            var pid = PIDs.get(version.asNode().getURI());
            var object = repositoryObjectLoader.getRepositoryObject(pid);
            object.
        }
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
