package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.order.MemberOrderHelper;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Job which sets/overwrites the order of children for a container object
 *
 * @author bbpennel
 */
public class SetOrderJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SetOrderJob.class);

    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private OrderRequest request;

    @Override
    public void run() {
        var parentObject = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
        var order = MemberOrderHelper.serializeOrder(request.getOrderedChildren());
        log.debug("Updating order property for {} to {}", request.getParentPid(), order);
        repositoryObjectFactory.createExclusiveRelationship(parentObject, Cdr.memberOrder, order);
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRequest(OrderRequest request) {
        this.request = request;
    }
}
