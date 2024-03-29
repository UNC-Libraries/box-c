package edu.unc.lib.boxc.services.camel;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

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
 * Processor to add headers to create display thumbnails for non-file objects
 *
 * @author lfarrell
 */
public class NonBinaryEnhancementProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(NonBinaryEnhancementProcessor.class);

    private String sourceImagesDir;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String uri = (String) in.getHeader(FCREPO_URI);
        String uuid = PIDs.get(uri).getUUID();
        String objBasePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        Path imgFile = Paths.get(sourceImagesDir, objBasePath, uuid);

        if (Files.isRegularFile(imgFile)) {
            in.setHeader(CdrBinaryPath, imgFile.toAbsolutePath().toString());
            in.setHeader(CdrBinaryMimeType, "image/*");
        }
    }

    public void setSourceImagesDir(String sourceImagesDir) {
        this.sourceImagesDir = sourceImagesDir;
    }
}
