package edu.unc.lib.boxc.services.camel.accessSurrogates;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jdom2.Document;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.makeEnhancementOperationBody;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.DELETE;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.SET;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.file.PathUtils.deleteFile;

/**
 * Processor for requests for adding, replacing, and removing access surrogates
 *
 * @author snluong
 */
public class AccessSurrogateRequestProcessor implements Processor {
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
        var action = request.getAction();

        accessControlService.assertHasAccess("User does not have permission to update access surrogates",
                pid, agent.getPrincipals(), Permission.editDescription);

        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        if (repositoryObject instanceof FileObject) {

            Path surrogatePath = derivativeService.getDerivativePath(pid, DatastreamType.ACCESS_SURROGATE);
            if (Objects.equals(SET, action)) {
                var file = new File(request.getFilePath());
                copyFile(file, surrogatePath.toFile());
            } else if (Objects.equals(DELETE, action)) {
                deleteFile(surrogatePath);
            }

            Document msg = makeEnhancementOperationBody(agent.getUsername(), pid, true);
            messageSender.sendMessage(msg);
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
