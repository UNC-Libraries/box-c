package edu.unc.lib.boxc.operations.impl.importxml;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.operations.jms.MessageSender;

/**
 * A service for performing import of bulk metadata updates
 *
 * @author harring
 *
 */
public class ImportXMLService extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(ImportXMLService.class);

    private String dataDir;
    private Path storagePath;

    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(ImportXMLRequest.class);

    public void init() throws IOException {
        storagePath = Paths.get(dataDir, "metadataImport");
        // Explicit check if directory exists before creating, to avoid failure due to mounted permissions check
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
    }

    public void pushJobToQueue(InputStream importStream, AgentPrincipals agent,
            String userEmail) throws IOException {

        File importFile = createTempFile("import", ".xml", storagePath.toFile());
        copyInputStreamToFile(importStream, importFile);

        ImportXMLRequest request = new ImportXMLRequest(userEmail, agent, importFile);
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to import {} has been queued for {}", importFile.getName(), agent.getUsername());
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDataDir() {
        return dataDir;
    }

}
