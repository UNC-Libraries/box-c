package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.IOException;
import java.util.Objects;

/**
 * Processing requests to assign a thumbnail from a file to its parent work
 *
 * @author snluong
 */
public class ThumbnailRequestProcessor implements Processor {
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private AccessControlService aclService;
    private IndexingMessageSender indexingMessageSender;

    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = ThumbnailRequestSerializationHelper.toRequest(in.getBody(String.class));
        var agent = request.getAgent();
        var action = request.getAction();
        var pid = PIDs.get(request.getFilePidString());

        aclService.assertHasAccess("User does not have permission to add/update work thumbnail",
                pid, agent.getPrincipals(), Permission.editDescription);

        var file = repositoryObjectLoader.getFileObject(pid);
        var work = file.getParent();

        if (Objects.equals(action, ThumbnailRequest.ASSIGN)) {
            repositoryObjectFactory.createExclusiveRelationship(work, Cdr.useAsThumbnail, file.getResource());
        } else if ( Objects.equals(action, ThumbnailRequest.DELETE)) {
            repositoryObjectFactory.deleteProperty(work, Cdr.useAsThumbnail);
        }

        // send message to update solr
        indexingMessageSender.sendIndexingOperation(
                agent.getUsername(), work.getPid(), IndexingActionType.UPDATE_DATASTREAMS);
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
