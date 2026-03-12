package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.machineGenerated.MachineGenRequestSerializationHelper;
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
    @BeanInject("machineGenDescriptionUpdateService")
    private MachineGenUpdateService machineGenDescriptionUpdateService;
    private String boxctronApiPath;
    private RepositoryObjectLoader repositoryObjectLoader;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        var request = MachineGenRequestSerializationHelper.toRequest(in.getBody(String.class));
        var id = request.getPidString();
        PID pid = PIDs.get(id);

        var fileObject = repositoryObjectLoader.getFileObject(pid);
        var originalFile = fileObject.getOriginalFile();

        var postMethod = new HttpPost(boxctronApiPath);
        postMethod.setEntity(getEntity(originalFile));

        try (
            var ignored = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
            var response = ignored.execute(postMethod);
        ) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 400 || statusCode == 422) {
                log.warn("Failed to generate boxctron description for {}, status {}: {}",
                        id, statusCode, response.getEntity().toString());
            } else {
                log.debug("Successfully requested boxctron description for {}", id);
                request.setText(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                machineGenDescriptionUpdateService.updateMachineGenText(request);
                indexingMessageSender.sendIndexingOperation("automated", pid, IndexingActionType.UPDATE_DATASTREAMS);
            }
        }
    }

    private HttpEntity getEntity(BinaryObject originalFile) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var bodyMap = Map.of(
                CONTEXT, "",
                FILENAME, originalFile.getFilename(),
                MIMETYPE, originalFile.getMimetype(),
                URI, originalFile.getUri());

        return EntityBuilder.create()
                .setText(objectMapper.writeValueAsString(bodyMap))
                .setContentType(ContentType.APPLICATION_JSON).build();
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

    public void setBoxctronApiPath(String boxctronApiPath) {
        this.boxctronApiPath = boxctronApiPath;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
