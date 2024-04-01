package edu.unc.lib.boxc.services.camel.streaming;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Objects;

import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.ADD;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.DELETE;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.VALID_FOLDERS;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * Processing requests to edit streaming properties on a FileObject
 */
public class StreamingPropertiesRequestProcessor implements Processor {
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private AccessControlService aclService;

    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = StreamingPropertiesRequestSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var pid = PIDs.get(request.getFilePidString());

        aclService.assertHasAccess("User does not have permission to set streaming properties",
                pid, agent.getPrincipals(), Permission.ingest);

        assertValid(request, pid);

        var file = repositoryObjectLoader.getFileObject(pid);
        if (Objects.equals(request.getAction(), ADD)) {
            repositoryObjectFactory.createExclusiveRelationship(
                    file, Cdr.streamingHost, request.getHost());
            repositoryObjectFactory.createExclusiveRelationship(
                    file, Cdr.streamingFile, formatFilename(request.getFilename()));
            repositoryObjectFactory.createExclusiveRelationship(
                    file, Cdr.streamingFolder, request.getFolder());
        } else if (Objects.equals(request.getAction(), DELETE)) {
            repositoryObjectFactory.deleteProperty(file,Cdr.streamingHost);
            repositoryObjectFactory.deleteProperty(file,Cdr.streamingFile);
            repositoryObjectFactory.deleteProperty(file,Cdr.streamingFolder);
        }
    }

    private String validate(StreamingPropertiesRequest request, PID pid) {
        String errorMessage = null;
        var action = request.getAction();
        if (StringUtils.isBlank(action)) {
            errorMessage = "An action is required.";
        }

        if (Objects.equals(ADD, action)) {
            var folder = request.getFolder();
            if (StringUtils.isBlank(request.getFilename()) || StringUtils.isBlank(folder)) {
                errorMessage = "Both a filename and streaming folder are required.";
            }

            if (!VALID_FOLDERS.contains(folder)) {
                errorMessage = "Streaming folder is not valid.";
            }
        }

        try {
            repositoryObjectLoader.getFileObject(pid);
        } catch (ObjectTypeMismatchException e) {
            errorMessage = "Object is not a FileObject";
        }
        // Request is valid
        return errorMessage;
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

    /**
     * Converts a filename to appropriate format for streaming
     * A filename like a/b/banjo_recording.mp3 would transform to banjo_recording-playlist.m3u8
     * @param filename filename of the file object
     * @return formatted string
     */
    private String formatFilename(String filename) {
        var nameOnly = getName(filename);
        if (nameOnly.contains("-playlist.m3u8")) {
            return nameOnly;
        }
        return removeExtension(nameOnly) + "-playlist.m3u8";
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
}
