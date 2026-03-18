package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.machineGenerated.MachineGenRequest;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Processor which contacts boxctron API to generate description
 *
 * @author snluong
 */
public class MachineGenDescriptionProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(MachineGenDescriptionProcessor.class);
    private static final String CONTEXT = "context";
    private static final String FILENAME = "filename";
    private static final String MIMETYPE = "mimetype";
    private static final String URI = "uri";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClientConnectionManager connectionManager;
    private MachineGenUpdateService machineGenDescriptionUpdateService;
    private String boxctronDescribesBasePath;
    private String apiKey;
    private RepositoryObjectLoader repositoryObjectLoader;
    private IndexingMessageSender indexingMessageSender;
    private CloseableHttpClient httpClient;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        var pid = getPid(in);
        var id = pid.getId();

        var fileObject = repositoryObjectLoader.getFileObject(pid);
        var originalFile = fileObject.getOriginalFile();

        var postMethod = new HttpPost(URIUtil.join(boxctronDescribesBasePath, "api", "v1", "describe", "uri"));
        postMethod.setEntity(getRequestJsonEntity(originalFile));
        postMethod.setHeader("X-API-Key", apiKey);

        try (
                var ignored = getHttpClient();
                var response = ignored.execute(postMethod);
        ) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.OK.value()) {
                log.warn("Failed to generate boxctron description for {}, status {}: {}",
                        id, statusCode, response.getEntity().toString());
            } else {
                log.debug("Successfully requested boxctron description for {}", id);
                var request = new MachineGenRequest();
                request.setPidString(id);
                request.setText(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                machineGenDescriptionUpdateService.updateMachineGenText(request);
                indexingMessageSender.sendIndexingOperation("automated", pid, IndexingActionType.UPDATE_DATASTREAMS);
            }
        }
    }

    /**
     * Used to filter whether machine gen desc service should be run
     * @param exchange Camel message exchange
     * @return boolean for the service needing to be run
     */
    public boolean needsRun(Exchange exchange) {
        Message in = exchange.getIn();
        String binaryId = getPid(in).getId();

        Path derivativeFinalPath = machineGenDescriptionUpdateService.getMachineGenDerivativePath(binaryId);
        if (Files.notExists(derivativeFinalPath)) {
            log.debug("Derivative run needed, no existing derivative at {}", derivativeFinalPath);
            return true;
        }

        String force = (String) in.getHeader("force");
        if (Boolean.parseBoolean(force)) {
            log.debug("Force flag was provided, forcing run of already existing derivative at {}",
                    derivativeFinalPath);
            return true;
        } else {
            log.debug("Derivative already exists at {}, run not needed", derivativeFinalPath);
            return false;
        }
    }

    private HttpEntity getRequestJsonEntity(BinaryObject originalFile) throws JsonProcessingException {
        var bodyMap = Map.of(
                CONTEXT, "",
                FILENAME, originalFile.getFilename(),
                MIMETYPE, originalFile.getMimetype(),
                URI, originalFile.getUri());

        return EntityBuilder.create()
                .setText(objectMapper.writeValueAsString(bodyMap))
                .setContentType(ContentType.APPLICATION_JSON).build();
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();
        }
        return httpClient;
    }

    private PID getPid(Message in) {
        String binaryUri = MessageUtil.getFcrepoUri(in);
        return PIDs.get(binaryUri);
    }

    public void setConnectionManager(HttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setMachineGenDescriptionUpdateService(MachineGenUpdateService machineGenDescriptionUpdateService) {
        this.machineGenDescriptionUpdateService = machineGenDescriptionUpdateService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setBoxctronDescribesBasePath(String boxctronDescribesBasePath) {
        this.boxctronDescribesBasePath = boxctronDescribesBasePath;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
