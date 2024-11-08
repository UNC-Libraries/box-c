package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Processing requests to import images to use as a thumbnail for a non-work Repository object
 *
 * @author snluong
 */
public class ImportThumbnailRequestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ImportThumbnailRequestProcessor.class);
    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = ImportThumbnailRequestSerializationHelper.toRequest(in.getBody(String.class));
        var mimetype = request.getMimetype();
        var storagePath = request.getStoragePath();
        var pidString = request.getPidString();
        var repoPath = PIDs.get(pidString).getRepositoryPath();

        in.setHeader(CdrBinaryPath, storagePath.toString());
        in.setHeader(CdrBinaryMimeType, mimetype);
        in.setHeader(FCREPO_URI, repoPath);
        // force access copy regeneration when importing a thumbnail
        in.setHeader("force", true);
    }

    /**
     * Deletes the temporarily stored uploaded thumbnail file
     * @param exchange
     * @throws IOException
     */
    public void cleanupTempThumbnailFile(Exchange exchange) throws IOException {
        final Message in = exchange.getIn();
        String tempValue = (String) in.getHeader(CdrBinaryPath);
        Path tempPath = Paths.get(tempValue);

        boolean deleted = Files.deleteIfExists(tempPath);
        if (deleted) {
            log.debug("Cleaned up leftover temp file {}", tempPath);
        }
    }
}
