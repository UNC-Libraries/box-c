package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenRequest;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.BeanInject;
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

import java.nio.charset.StandardCharsets;
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

    private HttpClientConnectionManager connectionManager;
    private ObjectMapper objectMapper = new ObjectMapper();
    private CloseableHttpClient httpClient;
    @BeanInject("machineGenDescriptionUpdateService")
    private MachineGenUpdateService machineGenDescriptionUpdateService;
    private String boxctronApiPath;
    private RepositoryObjectLoader repositoryObjectLoader;
    private MachineGenRequest request;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String fcrepoUri = MessageUtil.getFcrepoUri(in);
        PID pid = PIDs.get(fcrepoUri);

        if (pid != null) {
            var id = pid.getId();
            var fileObject = repositoryObjectLoader.getFileObject(pid);
            var originalFile = fileObject.getOriginalFile();

            httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            var bodyMap = Map.of(
                    CONTEXT, "",
                    FILENAME, originalFile.getFilename(),
                    MIMETYPE, originalFile.getMimetype(),
                    URI, originalFile.getUri());

            // Serialize the body map to JSON and set it as the entity of the post method
            HttpEntity entity = EntityBuilder.create()
                    .setText(objectMapper.writeValueAsString(bodyMap))
                    .setContentType(ContentType.APPLICATION_JSON).build();

            var postMethod = new HttpPost(boxctronApiPath);
            postMethod.setEntity(entity);

            try (var response = httpClient.execute(postMethod)) {
                var statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 400 || statusCode == 422) {
                    log.warn("Failed to generate machine description for {}, status {}: {}",
                            id, statusCode, response.getEntity().toString());
                } else {
                    log.debug("Successfully requested machine gen description for {}", id);
                    request = new MachineGenRequest();
                    request.setPidString(id);
                    request.setText(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    machineGenDescriptionUpdateService.updateMachineGenText(request);
                    indexingMessageSender.sendIndexingOperation("automated", pid, IndexingActionType.UPDATE_DATASTREAMS);
                }
            }
        }
    }

    public void setConnectionManager(HttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setMachineGenDescriptionUpdateService(MachineGenUpdateService machineGenDescriptionUpdateService) {
        this.machineGenDescriptionUpdateService = machineGenDescriptionUpdateService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setBoxctronApiPath(String boxctronApiPath) {
        this.boxctronApiPath = boxctronApiPath;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
