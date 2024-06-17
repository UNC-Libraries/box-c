package edu.unc.lib.boxc.services.camel.streaming;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Objects;

import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.ADD;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.DELETE;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.STREAMREAPER_PREFIX_URL;

/**
 * Processing requests to edit streaming properties on a FileObject
 */
public class StreamingPropertiesRequestProcessor implements Processor {
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private AccessControlService aclService;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = StreamingPropertiesRequestSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var pid = PIDs.get(request.getId());

        aclService.assertHasAccess("User does not have permission to set streaming properties",
                pid, agent.getPrincipals(), Permission.ingest);

        assertValid(request, pid);

        var file = repositoryObjectLoader.getFileObject(pid);
        if (Objects.equals(request.getAction(), ADD)) {
            repositoryObjectFactory.createExclusiveRelationship(
                    file, Cdr.streamingUrl, request.getUrl());
        } else if (Objects.equals(request.getAction(), DELETE)) {
            repositoryObjectFactory.deleteProperty(file,Cdr.streamingUrl);
        }

        indexingMessageSender.sendIndexingOperation(agent.getUsername(), pid,
                IndexingActionType.UPDATE_STREAMING_URL);
    }

    private String validate(StreamingPropertiesRequest request, PID pid) {
        var action = request.getAction();
        if (StringUtils.isBlank(action)) {
            return "An action is required.";
        }

        if (Objects.equals(ADD, action)) {
            var url = request.getUrl();
            if (url == null) {
                return "URL is required";
            } else {
                if (!url.startsWith(STREAMREAPER_PREFIX_URL)) {
                    return "URL is not a stream reaper URL";
                }
            }
        }

        try {
            repositoryObjectLoader.getFileObject(pid);
        } catch (ObjectTypeMismatchException e) {
            return "Object is not a FileObject";
        }
        // Request is valid
        return null;
    }

    /**
     *  Throws an IllegalArgumentException if validate method returns any message
     * @param request StreamingPropertiesRequest
     * @param pid PID of the object to add streaming properties to
     */
    private void assertValid(StreamingPropertiesRequest request, PID pid) {
        var message = validate(request, pid);
        if (!StringUtils.isBlank(message)) {
            throw new IllegalArgumentException(message);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
