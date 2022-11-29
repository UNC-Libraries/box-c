package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Route to execute requests to destroy object derivative files
 *
 * @author lfarrell
 *
 */
public class DestroyDerivativesProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(DestroyDerivativesProcessor.class);
    private final String fileExtension;
    private final String derivativeBasePath;

    public DestroyDerivativesProcessor(String fileExtension, String derivativeBasePath) {
        this.fileExtension = fileExtension;
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getIn();
        String binaryId = (String) in.getHeader(CdrBinaryPidId);
        String binaryUUID = PIDs.get(binaryId).getId();
        String binarySubPath = idToPath(binaryUUID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        String addDot = fileExtension.equals("") ? "" : ".";
        Path derivativePath = Paths.get(derivativeBasePath, binarySubPath, binaryUUID + addDot + fileExtension);

        deleteDerivative(derivativePath, binaryId);
    }

    private void deleteDerivative(Path derivativePath, String binaryId) {
        try {
            if (shouldRemoveFile(derivativePath)) {
                boolean deleted = Files.deleteIfExists(derivativePath);
                if (deleted) {
                    log.debug("Deleted derivative path {}", derivativePath);
                    deleteDerivative(derivativePath.getParent(), binaryId);
                }
            }
        } catch (IOException e) {
            log.warn("Unable to destroy derivative and parent directories for {}: {}", binaryId, e.getMessage());
        }
    }

    private boolean shouldRemoveFile(Path path) throws IOException {
        if (path.endsWith(derivativeBasePath)) {
            return false;
        }

        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }
        return true;
    }
}
