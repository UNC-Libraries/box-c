package edu.unc.lib.boxc.services.camel.viewSettings;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Processor for requests for updating the view settings for UV
 *
 * @author snluong
 */
public class ViewSettingRequestProcessor implements Processor {
    private AccessControlService accessControlService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws Exception {
        var in = exchange.getIn();
        var request = ViewSettingRequestSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var pid = PIDs.get(request.getObjectPidString());

        accessControlService.assertHasAccess("User does not have permission to update view settings",
                pid, agent.getPrincipals(), Permission.editViewSettings);

        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        var behavior = request.getViewBehavior();

        if (repositoryObject instanceof WorkObject) {
            if (behavior == null) {
                repositoryObjectFactory.deleteProperty(repositoryObject, CdrView.viewBehavior);
            } else {
                repositoryObjectFactory.createExclusiveRelationship(repositoryObject,
                        CdrView.viewBehavior, behavior.getString());
            }
        }
        indexingMessageSender.sendIndexingOperation(agent.getUsername(), pid, IndexingActionType.UPDATE_VIEW_BEHAVIOR);
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
