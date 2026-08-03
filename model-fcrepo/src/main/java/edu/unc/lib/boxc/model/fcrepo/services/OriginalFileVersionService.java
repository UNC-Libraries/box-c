package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.unc.lib.boxc.model.api.rdf.Ebucore.hasMimeType;

/**
 * Service which returns OriginalFile version information for file objects stored in Fedora
 */
public class OriginalFileVersionService {
    private FcrepoClient fcrepoClient;

    /**
     * Contacts Fedora API to get a list of original file versions and then again for metadata for each version
     * @param pid FileObject PID
     * @return a map where the keys are OriginalFile version PIDS and the values
     *   are maps of filenames and mimetypes for each version
     */
    public Map<PID, Map<String, String>> getVersionMetadata(PID pid) {
        var model = getVersions(pid);
        Map<PID, Map<String, String>> map = new LinkedHashMap<>();
        var versions = model.listObjectsOfProperty(Ldp.contains);
        while (versions.hasNext()) {
            var version = versions.next();
            var versionPid = PIDs.get(version.asResource().getURI());
            var uriString = URIUtil.join(versionPid.getRepositoryUri());
            var originalFileUri = URI.create(uriString);

            try (FcrepoResponse resp = fcrepoClient.get(originalFileUri).perform()) {
                Model childModel = ModelFactory.createDefaultModel();
                var readModel =  childModel.read(resp.getBody(), null);
                Resource resc = readModel.getResource(versionPid.getRepositoryPath());

                Map<String, String> metadata = new HashMap<>();
                metadata.put("filename", resc.getProperty(Ebucore.filename).getString());
                metadata.put("mimetype", resc.getProperty(hasMimeType).getString());
                map.put(versionPid, metadata);
            } catch (IOException e) {
                throw new FedoraException("Failed to get metadata for " + originalFileUri, e);
            } catch (FcrepoOperationFailedException e) {
                throw ClientFaultResolver.resolve(e);
            }
        }
        return map;
    }

    /**
     * Contacts Fedora API for list of original file versions
     * @param pid FileObject PID
     * @return model created from Fedora API response
     */
    private Model getVersions(PID pid) {
        var originalFilePid = DatastreamPids.getOriginalFilePid(pid);
        var uriString = URIUtil.join(originalFilePid.getRepositoryUri(), "fcr:metadata", "fcr:versions");
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

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
