package edu.unc.lib.boxc.services.camel.accessSurrogates;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.makeEnhancementOperationBody;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.DELETE;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.SET;

/**
 * Processor for requests for adding, replacing, and removing access surrogates
 *
 * @author snluong
 */
public class AccessSurrogateRequestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AccessSurrogateRequestProcessor.class);
    private AccessControlService accessControlService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private DerivativeService derivativeService;
    private MessageSender messageSender;

    @Override
    public void process(Exchange exchange) throws Exception {
        var in = exchange.getIn();
        var request = AccessSurrogateRequestSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var pid = PIDs.get(request.getPidString());

        accessControlService.assertHasAccess("User does not have permission to update access surrogates",
                pid, agent.getPrincipals(), Permission.editDescription);

        var action = request.getAction();
        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        var filePath = request.getFilePath();
        if (repositoryObject instanceof FileObject) {
            Path surrogatePath = derivativeService.getDerivativePath(pid, DatastreamType.ACCESS_SURROGATE);
            if (Objects.equals(SET, action)) {
                Files.copy(filePath, surrogatePath, StandardCopyOption.REPLACE_EXISTING);
            } else if (Objects.equals(DELETE, action)) {
                Files.deleteIfExists(surrogatePath);
            }

            Document msg = makeEnhancementOperationBody(agent.getUsername(), pid, true);
            messageSender.sendMessage(msg);
        } else {
            log.error("Cannot process access surrogate update for {}, non FileObjects do not have access surrogates",
                    pid.getId());
        }
        // clean up file from request if it exists
        if (filePath != null) {
            Files.deleteIfExists(filePath);
        }
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
