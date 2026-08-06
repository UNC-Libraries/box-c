package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
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
    private final static String FCR_METADATA = "fcr:metadata";
    private final static String FCR_VERSIONS = "fcr:versions";
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
            // get fedora metadata for each individual version
            var uriString = URIUtil.join(versionPid.getRepositoryUri(), FCR_METADATA);
            var originalFileVersionUri = URI.create(uriString);

            try (FcrepoResponse resp = fcrepoClient.get(originalFileVersionUri).perform()) {
                Model childModel = ModelFactory.createDefaultModel();
                var readModel =  childModel.read(resp.getBody(), null, Lang.TURTLE.getName());
                // remove versions specification in path to match the right resource
                var repositoryPath = StringUtils.substringBefore(versionPid.getRepositoryPath(), "/" + FCR_VERSIONS);
                Resource resc = readModel.getResource(repositoryPath);

                Map<String, String> metadata = new HashMap<>();
                metadata.put("filename", getPropertyValue(resc, Ebucore.filename));
                metadata.put("mimetype", getPropertyValue(resc, hasMimeType));
                map.put(versionPid, metadata);
            } catch (IOException e) {
                throw new FedoraException("Failed to get metadata for " + originalFileVersionUri, e);
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
        var uriString = URIUtil.join(originalFilePid.getRepositoryUri(), FCR_METADATA, FCR_VERSIONS);
        var objUri = URI.create(uriString);
        try (FcrepoResponse resp = fcrepoClient.get(objUri).perform()) {
            Model childModel = ModelFactory.createDefaultModel();
            return childModel.read(resp.getBody(), null, Lang.TURTLE.getName());
        } catch (IOException e) {
            throw new FedoraException("Failed to list versions for " + objUri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    private String getPropertyValue(Resource resc, Property property) {
        var propertyValue = resc.getProperty(property);
        if (propertyValue == null) {
            return null;
        }
        return propertyValue.getString();
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
