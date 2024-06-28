package edu.unc.lib.boxc.services.camel;

import static edu.unc.lib.boxc.model.api.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;

/**
 * Stores information related to identifying binary objects from the repository
 *
 * @author lfarrell
 *
 */
public class BinaryMetadataProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BinaryMetadataProcessor.class);

    private RepositoryObjectLoader repoObjLoader;
    private DerivativeService derivativeService;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String fcrepoBinaryUri = (String) in.getHeader(FCREPO_URI);
        PID binPid = PIDs.get(fcrepoBinaryUri);
        PID filePid = PIDs.get(binPid.getId());

        // If an access surrogate is present, supply its details instead of that of the original file
        Path surrogatePath = derivativeService.getDerivativePath(filePid, DatastreamType.ACCESS_SURROGATE);
        if (Files.exists(surrogatePath)) {
            String mimetype = getMimetype(surrogatePath);
            if (mimetype != null) {
                in.setHeader(CdrBinaryPath, surrogatePath.toString());
                in.setHeader(CdrBinaryMimeType, mimetype);
                log.info("Using access surrogate {} with type {} for derivative generation of object {}",
                        surrogatePath, mimetype, filePid.getId());
            }
            return;
        }

        // No access surrogate, so use the original file for producing derivatives
        BinaryObject binObj;
        try {
            binObj = repoObjLoader.getBinaryObject(binPid);
        } catch (ObjectTypeMismatchException e) {
            log.warn("Cannot extract binary metadata from {}, it is not a binary", binPid.getId());
            return;
        }

        if (binObj.getContentUri() != null) {
            Model model = ModelFactory.createDefaultModel();
            InputStream bodyStream = in.getBody(InputStream.class);
            // Reset the body inputstream in case it was already read elsewhere due to multicasting
            bodyStream.reset();
            model.read(bodyStream, null, "Turtle");
            Resource resc = model.getResource(binPid.getRepositoryPath());
            String binaryMimeType = resc.getProperty(hasMimeType).getObject().toString();

            URI contentUri = binObj.getContentUri();
            if (!contentUri.getScheme().equals("file")) {
                log.warn("Only file content URIs are supported at this time");
                return;
            }

            in.setHeader(CdrBinaryPath, Paths.get(binObj.getContentUri()).toString());
            in.setHeader(CdrBinaryMimeType, binaryMimeType);
        } else {
            log.warn("Cannot process {}, internal binaries are not currently supported", binPid.getId());
        }
    }

    private String getMimetype(Path path) {
        Tika tika = new Tika();
        try {
            return tika.detect(path);
        } catch (IOException e) {
            log.warn("Failed to detect mimetype for {}", path, e);
            return null;
        }
    }

    /**
     * @param repoObjLoader the repoObjLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
